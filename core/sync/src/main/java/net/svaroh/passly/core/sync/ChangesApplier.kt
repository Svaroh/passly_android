/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.core.sync

import net.svaroh.passly.common.transaction.DatabaseTransactionRunner
import net.svaroh.passly.core.sync.dto.SyncBootstrapDto
import net.svaroh.passly.core.sync.dto.SyncChangesDto
import net.svaroh.passly.core.sync.dto.SyncDeletionsDto
import net.svaroh.passly.core.sync.dto.SyncUpsertsDto
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.resourcetypes.usecase.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.dto.response.FolderResponseDto
import net.svaroh.passly.dto.response.GroupsResponseDto
import net.svaroh.passly.dto.response.MetadataKeysResponseDto
import net.svaroh.passly.dto.response.ResourceTypeDto
import net.svaroh.passly.dto.response.UserDto
import net.svaroh.passly.entity.folder.Folder
import net.svaroh.passly.entity.folder.FolderAndUsersCrossRef
import net.svaroh.passly.entity.folder.FolderUpdateState
import net.svaroh.passly.entity.group.FolderAndGroupsCrossRef
import net.svaroh.passly.entity.group.GroupUpdateState
import net.svaroh.passly.entity.group.ResourceAndGroupsCrossRef
import net.svaroh.passly.entity.group.UsersAndGroupCrossRef
import net.svaroh.passly.entity.group.UsersGroup
import net.svaroh.passly.entity.metadata.MetadataKey
import net.svaroh.passly.entity.resource.Permission
import net.svaroh.passly.entity.resource.Resource
import net.svaroh.passly.entity.resource.ResourceAndTagsCrossRef
import net.svaroh.passly.entity.resource.ResourceMetadata
import net.svaroh.passly.entity.resource.ResourceType
import net.svaroh.passly.entity.resource.ResourceUri
import net.svaroh.passly.entity.resource.ResourceUpdateState
import net.svaroh.passly.entity.resource.Tag
import net.svaroh.passly.entity.secret.Secret
import net.svaroh.passly.entity.sync.SyncState
import net.svaroh.passly.entity.user.ResourceAndUsersCrossRef
import net.svaroh.passly.entity.user.User
import net.svaroh.passly.entity.user.UserGpgKey
import net.svaroh.passly.entity.user.UserProfile
import net.svaroh.passly.entity.user.UserUpdateState
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.mappers.ResourceModelMapper
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Applies sync changes (upserts and deletions) to the local Room database within a single transaction
 * along with the updated cursor, ensuring strict consistency and atomic durability.
 */
class ChangesApplier(
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val databaseTransactionRunner: DatabaseTransactionRunner,
    private val resourceModelMapper: ResourceModelMapper,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
) {
    /**
     * Applies a single delta page to Room and advances the sync cursor atomically.
     */
    suspend fun applyChanges(
        changes: SyncChangesDto,
        epoch: String? = null,
    ): SyncStats =
        databaseTransactionRunner.runInTransaction {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount) {
                "No selected account during applyChanges"
            }
            val db = databaseProvider.get(userId)

            val upsertStats = applyUpserts(db, changes.upserts, userId)
            val deletedCount = applyDeletions(db, changes.deletions)

            val currentState = db.syncStateDao().getSyncState()
            val resolvedEpoch = epoch ?: currentState?.epoch
            db.syncStateDao().upsert(
                SyncState(
                    id = SyncState.SINGLE_ROW_ID,
                    cursor = changes.cursor,
                    epoch = resolvedEpoch,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastReconcileTimestamp = currentState?.lastReconcileTimestamp,
                    failureCount = 0,
                ),
            )

            SyncStats(
                upserted = upsertStats.upserted,
                deleted = deletedCount,
                secretsUpdated = upsertStats.secretsUpdated,
            )
        }

    /**
     * Applies a bootstrap slice to Room and records the pinned cursor.
     */
    suspend fun applyBootstrapPage(
        bootstrap: SyncBootstrapDto,
        page: Int,
    ): SyncStats =
        databaseTransactionRunner.runInTransaction {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount) {
                "No selected account during applyBootstrapPage"
            }
            val db = databaseProvider.get(userId)

            val upsertStats = applyUpserts(db, bootstrap.upserts, userId)

            val currentState = db.syncStateDao().getSyncState()
            db.syncStateDao().upsert(
                SyncState(
                    id = SyncState.SINGLE_ROW_ID,
                    cursor = bootstrap.cursor,
                    epoch = bootstrap.epoch,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastReconcileTimestamp = currentState?.lastReconcileTimestamp,
                    failureCount = 0,
                ),
            )

            SyncStats(
                upserted = upsertStats.upserted,
                deleted = 0,
                secretsUpdated = upsertStats.secretsUpdated,
            )
        }

    private suspend fun applyUpserts(
        db: net.svaroh.passly.database.ResourceDatabase,
        upserts: SyncUpsertsDto,
        userId: String,
    ): SyncStats {
        var upsertedCount = 0
        var secretsUpdatedCount = 0

        // 1. Metadata keys
        if (upserts.metadataKeys.isNotEmpty()) {
            val keys = mapMetadataKeys(upserts.metadataKeys)
            db.metadataKeysDao().insertAll(keys)
            upsertedCount += keys.size
        }

        // 2. Resource types
        if (upserts.resourceTypes.isNotEmpty()) {
            val types = upserts.resourceTypes.map(::mapResourceType)
            db.resourceTypesDao().upsertAll(types)
            upsertedCount += types.size
        }

        // 3. Users
        if (upserts.users.isNotEmpty()) {
            val users = upserts.users.mapNotNull(::mapUser)
            db.usersDao().upsertAll(users)
            upsertedCount += users.size
        }

        // 4. Groups
        if (upserts.groups.isNotEmpty()) {
            val groups = upserts.groups.map(::mapGroup)
            db.groupsDao().upsertAll(groups)
            upsertedCount += groups.size

            val userGroupRefs = upserts.groups.flatMap { gDto ->
                gDto.users?.map { uDto ->
                    UsersAndGroupCrossRef(
                        groupId = gDto.id.toString(),
                        userId = uDto.id.toString(),
                    )
                }.orEmpty()
            }
            if (userGroupRefs.isNotEmpty()) {
                db.usersAndGroupsCrossRefDao().upsertAll(userGroupRefs)
            }
        }

        // 5. Folders
        if (upserts.folders.isNotEmpty()) {
            val folders = upserts.folders.map { mapFolder(it, userId) }
            db.foldersDao().upsertAll(folders)
            upsertedCount += folders.size

            val folderUserRefs = upserts.folders.flatMap { fDto ->
                fDto.permissions.filter { it.group == null }.map { p ->
                    FolderAndUsersCrossRef(
                        folderId = fDto.id.toString(),
                        userId = p.aroForeignKey.toString(),
                        permission = mapEntityPermission(p.type),
                        permissionId = p.id.toString(),
                    )
                }
            }
            if (folderUserRefs.isNotEmpty()) {
                db.folderAndUsersCrossRefDao().upsertAll(folderUserRefs)
            }

            val folderGroupRefs = upserts.folders.flatMap { fDto ->
                fDto.permissions.filter { it.group != null }.map { p ->
                    FolderAndGroupsCrossRef(
                        folderId = fDto.id.toString(),
                        groupId = p.group!!.id.toString(),
                        permission = mapEntityPermission(p.type),
                        permissionId = p.id.toString(),
                    )
                }
            }
            if (folderGroupRefs.isNotEmpty()) {
                db.folderAndGroupsCrossRefDao().upsertAll(folderGroupRefs)
            }
        }

        // 6. Resources & associated secrets, tags, permissions, metadata
        if (upserts.resources.isNotEmpty()) {
            val slugMapping = resourceTypeIdToSlugMappingProvider.provideMappingForSelectedAccount()
            val resourceEntities = mutableListOf<Resource>()
            val metadataEntities = mutableListOf<ResourceMetadata>()
            val uriEntities = mutableListOf<ResourceUri>()
            val tagEntities = mutableListOf<Tag>()
            val tagCrossRefs = mutableListOf<ResourceAndTagsCrossRef>()
            val userCrossRefs = mutableListOf<ResourceAndUsersCrossRef>()
            val groupCrossRefs = mutableListOf<ResourceAndGroupsCrossRef>()
            val resourceSecrets = mutableListOf<Secret>()

            for (resDto in upserts.resources) {
                try {
                    val slug = slugMapping[resDto.resourceTypeId] ?: "password-and-description"
                    val mappedResource = resourceModelMapper.map(resDto, slug = slug)
                    resourceEntities.add(resourceModelMapper.map(mappedResource, resourceUpdateState = ResourceUpdateState.UPDATED))
                    metadataEntities.add(resourceModelMapper.mapResourceMetadata(mappedResource))
                    uriEntities.addAll(resourceModelMapper.mapResourceUris(mappedResource))

                    resDto.tags?.forEach { tagDto ->
                        tagEntities.add(Tag(id = tagDto.id.toString(), slug = tagDto.slug, isShared = tagDto.isShared))
                        tagCrossRefs.add(ResourceAndTagsCrossRef(tagId = tagDto.id.toString(), resourceId = resDto.id.toString()))
                    }

                    resDto.permissions?.forEach { permDto ->
                        if (permDto.group != null) {
                            groupCrossRefs.add(
                                ResourceAndGroupsCrossRef(
                                    resourceId = resDto.id.toString(),
                                    groupId = permDto.group!!.id.toString(),
                                    permission = mapEntityPermission(permDto.type),
                                    permissionId = permDto.id.toString(),
                                ),
                            )
                        } else {
                            userCrossRefs.add(
                                ResourceAndUsersCrossRef(
                                    resourceId = resDto.id.toString(),
                                    userId = permDto.aroForeignKey.toString(),
                                    permission = mapEntityPermission(permDto.type),
                                    permissionId = permDto.id.toString(),
                                ),
                            )
                        }
                    }

                    resDto.secrets?.firstOrNull()?.let { sDto ->
                        resourceSecrets.add(
                            Secret(
                                resourceId = resDto.id.toString(),
                                secretId = sDto.id?.toString(),
                                armoredData = sDto.data,
                                modified = sDto.modified?.let(::parseZonedDateTime),
                                fetchedAt = ZonedDateTime.now(),
                            ),
                        )
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Skipping unparseable resource payload: ${resDto.id}")
                }
            }

            db.resourcesDao().upsertAll(resourceEntities)
            db.resourceMetadataDao().upsertAll(metadataEntities)

            for (res in resourceEntities) {
                db.resourceUriDao().deleteForResource(res.resourceId)
            }
            db.resourceUriDao().insertAll(uriEntities)

            if (tagEntities.isNotEmpty()) {
                db.tagsDao().upsertAll(tagEntities)
            }
            if (tagCrossRefs.isNotEmpty()) {
                db.resourcesAndTagsCrossRefDao().upsertAll(tagCrossRefs)
            }
            if (userCrossRefs.isNotEmpty()) {
                db.resourcesAndUsersCrossRefDao().upsertAll(userCrossRefs)
            }
            if (groupCrossRefs.isNotEmpty()) {
                db.resourcesAndGroupsCrossRefDao().upsertAll(groupCrossRefs)
            }
            if (resourceSecrets.isNotEmpty()) {
                db.secretsDao().upsertAll(resourceSecrets)
                secretsUpdatedCount += resourceSecrets.size
            }

            upsertedCount += resourceEntities.size
        }

        // 7. Standalone secrets
        if (upserts.secrets.isNotEmpty()) {
            val standaloneSecrets = upserts.secrets.mapNotNull { sDto ->
                sDto.resourceId?.toString()?.let { resId ->
                    Secret(
                        resourceId = resId,
                        secretId = sDto.id?.toString(),
                        armoredData = sDto.data,
                        modified = sDto.modified?.let(::parseZonedDateTime),
                        fetchedAt = ZonedDateTime.now(),
                    )
                }
            }
            if (standaloneSecrets.isNotEmpty()) {
                db.secretsDao().upsertAll(standaloneSecrets)
                secretsUpdatedCount += standaloneSecrets.size
            }
        }

        return SyncStats(
            upserted = upsertedCount,
            secretsUpdated = secretsUpdatedCount,
        )
    }

    private suspend fun applyDeletions(
        db: net.svaroh.passly.database.ResourceDatabase,
        deletions: SyncDeletionsDto,
    ): Int {
        var totalDeleted = 0
        if (deletions.resources.isNotEmpty()) {
            db.resourcesDao().deleteAll(deletions.resources)
            totalDeleted += deletions.resources.size
        }
        if (deletions.folders.isNotEmpty()) {
            db.foldersDao().deleteAll(deletions.folders)
            totalDeleted += deletions.folders.size
        }
        if (deletions.users.isNotEmpty()) {
            db.usersDao().deleteAll(deletions.users)
            totalDeleted += deletions.users.size
        }
        if (deletions.groups.isNotEmpty()) {
            db.groupsDao().deleteAll(deletions.groups)
            totalDeleted += deletions.groups.size
        }
        return totalDeleted
    }

    private fun mapMetadataKeys(
        dtos: List<MetadataKeysResponseDto>,
    ): List<MetadataKey> =
        dtos.map { dto ->
            MetadataKey(
                id = dto.id.toString(),
                fingerprint = dto.fingerprint,
                armoredKey = dto.armoredKey,
                modified = parseZonedDateTime(dto.modified) ?: ZonedDateTime.now(),
                expired = dto.expired?.let(::parseZonedDateTime),
                deleted = dto.deleted?.let(::parseZonedDateTime),
            )
        }

    private fun mapResourceType(dto: ResourceTypeDto): ResourceType =
        ResourceType(
            resourceTypeId = dto.id.toString(),
            name = dto.name,
            slug = dto.slug,
            deleted = dto.deleted?.let(::parseZonedDateTime),
        )

    private fun mapUser(dto: UserDto): User? {
        val gpg = dto.gpgKey ?: return null
        return User(
            id = dto.id.toString(),
            userName = dto.username,
            disabled = dto.disabled?.let { parseZonedDateTime(it)?.isBefore(ZonedDateTime.now()) } ?: false,
            profile = UserProfile(
                firstName = dto.profile?.firstName,
                lastName = dto.profile?.lastName,
                avatarUrl = dto.profile?.avatar?.url?.medium,
            ),
            gpgKey = UserGpgKey(
                id = gpg.id.toString(),
                armoredKey = gpg.armoredKey,
                bits = gpg.bits,
                uid = gpg.uid,
                keyId = gpg.keyId,
                fingerprint = gpg.fingerprint,
                type = gpg.type,
                expires = gpg.expires?.let(::parseZonedDateTime),
                created = gpg.keyCreated?.let(::parseZonedDateTime),
            ),
            updateState = UserUpdateState.UPDATED,
        )
    }

    private fun mapGroup(dto: GroupsResponseDto): UsersGroup =
        UsersGroup(
            groupId = dto.id.toString(),
            name = dto.name,
            updateState = GroupUpdateState.UPDATED,
        )

    private fun mapFolder(dto: FolderResponseDto, currentUserId: String): Folder =
        Folder(
            folderId = dto.id.toString(),
            name = dto.name.orEmpty(),
            permission = mapEntityPermission(dto.permission.type),
            parentId = dto.folderParentId?.toString(),
            isShared = dto.permissions.any { p -> p.group != null || p.aroForeignKey.toString() != currentUserId },
            modified = dto.modified?.let(::parseZonedDateTime) ?: ZonedDateTime.now(),
            updateState = FolderUpdateState.UPDATED,
        )

    private fun mapEntityPermission(typeInt: Int): Permission =
        permissionsModelMapper.map(permissionsModelMapper.map(typeInt))

    private fun parseZonedDateTime(value: String): ZonedDateTime? =
        try {
            ZonedDateTime.parse(value)
        } catch (e: DateTimeParseException) {
            Timber.w(e, "Could not parse ISO timestamp: $value")
            null
        }
}
