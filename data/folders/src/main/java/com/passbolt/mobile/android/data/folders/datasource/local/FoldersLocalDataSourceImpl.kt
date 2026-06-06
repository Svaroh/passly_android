/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package net.svaroh.passly.data.folders.datasource.local

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import net.svaroh.passly.data.folders.mapper.toDomain
import net.svaroh.passly.data.folders.mapper.toEntity
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.database.QuerySanitizer
import net.svaroh.passly.domain.folders.datasource.FoldersLocalDataSource
import net.svaroh.passly.domain.folders.model.FolderModel
import net.svaroh.passly.domain.folders.model.FolderModelWithAttributes
import net.svaroh.passly.domain.folders.model.FolderUpdateState
import net.svaroh.passly.domain.folders.model.FolderWithCountAndPath
import net.svaroh.passly.domain.folders.model.ParentPermissionItemId
import net.svaroh.passly.entity.folder.FolderAndUsersCrossRef
import net.svaroh.passly.entity.group.FolderAndGroupsCrossRef
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.mappers.SharePermissionsModelMapper
import net.svaroh.passly.ui.Folder
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.PermissionModelUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.svaroh.passly.domain.folders.model.FolderUpdateState.UPDATED as DOMAIN_UPDATED

internal class FoldersLocalDataSourceImpl(
    private val databaseProvider: DatabaseProvider,
    private val querySanitizer: QuerySanitizer,
    private val permissionsModelMapper: PermissionsModelMapper,
) : FoldersLocalDataSource {
    override suspend fun addFolder(
        folder: FolderModel,
        userId: String,
    ) {
        databaseProvider
            .get(userId)
            .foldersDao()
            .insert(folder.toEntity(DOMAIN_UPDATED.toEntity(), permissionsModelMapper))
    }

    override suspend fun upsertFolders(
        folders: List<FolderModel>,
        userId: String,
    ) {
        val entities = folders.map { it.toEntity(DOMAIN_UPDATED.toEntity(), permissionsModelMapper) }
        databaseProvider
            .get(userId)
            .foldersDao()
            .upsertAll(entities)
    }

    override suspend fun setAllFoldersUpdateState(
        updateState: FolderUpdateState,
        userId: String,
    ) {
        databaseProvider
            .get(userId)
            .foldersDao()
            .setAllUpdateState(updateState.toEntity())
    }

    override suspend fun removeFoldersWithUpdateState(
        updateState: FolderUpdateState,
        userId: String,
    ) {
        databaseProvider
            .get(userId)
            .foldersDao()
            .removeWithUpdateState(updateState.toEntity())
    }

    override suspend fun updateFoldersIsShared(
        currentUserServerId: String,
        userId: String,
    ) {
        databaseProvider
            .get(userId)
            .foldersDao()
            .updateIsShared(currentUserServerId)
    }

    override suspend fun addFolderPermissions(
        foldersWithAttributes: List<FolderModelWithAttributes>,
        userId: String,
    ) {
        val db = databaseProvider.get(userId)
        val foldersAndGroupsCrossRefDao = db.folderAndGroupsCrossRefDao()
        val foldersAndUsersCrossRefDao = db.folderAndUsersCrossRefDao()

        val folderGroupsPermissions =
            foldersWithAttributes.map {
                it.folderModel.folderId to it.folderPermissions.filterIsInstance<PermissionModel.GroupPermissionModel>()
            }
        val folderUsersPermissions =
            foldersWithAttributes.map {
                it.folderModel.folderId to it.folderPermissions.filterIsInstance<PermissionModel.UserPermissionModel>()
            }

        val folderAndGroupCrossRefs =
            folderGroupsPermissions.flatMap { (folderId, groupPermissions) ->
                groupPermissions.map { groupPermission ->
                    FolderAndGroupsCrossRef(
                        folderId,
                        groupPermission.group.groupId,
                        permissionsModelMapper.map(groupPermission.permission),
                        groupPermission.permissionId,
                    )
                }
            }

        val folderAndUsersCrossRefs =
            folderUsersPermissions.flatMap { (folderId, userPermissions) ->
                userPermissions.map { userPermission ->
                    FolderAndUsersCrossRef(
                        folderId,
                        userPermission.userId,
                        permissionsModelMapper.map(userPermission.permission),
                        userPermission.permissionId,
                    )
                }
            }

        foldersAndGroupsCrossRefDao.insertAll(folderAndGroupCrossRefs)
        foldersAndUsersCrossRefDao.insertAll(folderAndUsersCrossRefs)
    }

    override suspend fun clearFolderPermissions(userId: String) {
        databaseProvider
            .get(userId)
            .apply {
                folderAndGroupsCrossRefDao().deleteAll()
                folderAndUsersCrossRefDao().deleteAll()
            }
    }

    override suspend fun getFolderDetails(
        folderId: String,
        userId: String,
    ): FolderModel =
        databaseProvider
            .get(userId)
            .foldersDao()
            .get(folderId)
            .toDomain(permissionsModelMapper)

    override suspend fun getFolderLocation(
        folderId: String,
        userId: String,
    ): List<FolderModel> =
        databaseProvider
            .get(userId)
            .foldersDao()
            .getFolderLocation(folderId)
            .map { it.toDomain(permissionsModelMapper) }

    override suspend fun getFolderPermissions(
        folderId: String,
        userId: String,
    ): List<PermissionModelUi> {
        val foldersDao = databaseProvider.get(userId).foldersDao()
        val groupsPermissions = foldersDao.getFolderGroupsPermissions(folderId)
        val usersPermissions = foldersDao.getFolderUsersPermissions(folderId)
        return permissionsModelMapper.map(groupsPermissions, usersPermissions)
    }

    override suspend fun getParentFolderPermissionsToApplyToNewItem(
        parentFolderId: String,
        itemId: ParentPermissionItemId,
        currentUserServerId: String,
        userId: String,
    ): List<PermissionModelUi> {
        val db = databaseProvider.get(userId)
        val foldersDao = db.foldersDao()
        val resourcesDao = db.resourcesDao()

        val groupsPermissions =
            foldersDao
                .getFolderGroupsPermissions(parentFolderId)
                .map { it.copy(permissionId = SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID) }
        val usersPermissions =
            foldersDao
                .getFolderUsersPermissions(parentFolderId)
                .map {
                    if (it.userId != currentUserServerId) {
                        // new permissions from parent folder to inherit
                        it.copy(permissionId = SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID)
                    } else {
                        // special case: permission for current user
                        //
                        // use permission values from parent folder to apply but overwrite the permission id
                        val currentUserPermissions =
                            when (itemId) {
                                is ParentPermissionItemId.FolderId -> foldersDao.getFolderUsersPermissions(itemId.folderId)
                                is ParentPermissionItemId.ResourceId -> resourcesDao.getResourceUsersPermissions(itemId.resourceId)
                            }

                        require(currentUserPermissions.size == 1) {
                            "On newly created item there should be exactly one permission" +
                                " - only for the current user (the one who just created the item)"
                        }
                        it.copy(permissionId = currentUserPermissions[0].permissionId)
                    }
                }

        return permissionsModelMapper.map(groupsPermissions, usersPermissions)
    }

    override fun getDirectChildFoldersPaged(
        folderId: String?,
        searchQuery: String?,
        pageSize: Int,
        enablePlaceholders: Boolean,
        userId: String,
    ): Flow<PagingData<FolderWithCountAndPath>> {
        val ftsQuery = querySanitizer.sanitize(searchQuery)
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = enablePlaceholders),
            pagingSourceFactory = {
                databaseProvider
                    .get(userId)
                    .paginatedFoldersDao()
                    .getFolderDirectChildFolders(folderId, ftsQuery)
            },
        ).flow.map { pagingData -> pagingData.map { it.toDomain(permissionsModelMapper) } }
    }

    override suspend fun getSubFoldersForFolder(
        folder: Folder,
        searchQuery: String?,
        userId: String,
    ): List<FolderWithCountAndPath> {
        val foldersDao = databaseProvider.get(userId).foldersDao()
        val ftsQuery = querySanitizer.sanitize(searchQuery)
        val folders =
            when (folder) {
                is Folder.Child -> foldersDao.getFolderAllChildFoldersRecursively(folder.folderId, ftsQuery)
                is Folder.Root -> foldersDao.getAllFolders(ftsQuery)
            }
        return folders.map { it.toDomain(permissionsModelMapper) }
    }

    override fun getSubFoldersForFolderPaged(
        folder: Folder,
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<FolderWithCountAndPath>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = {
                val foldersDao = databaseProvider.get(userId).paginatedFoldersDao()
                val ftsQuery = querySanitizer.sanitize(searchQuery)
                when (folder) {
                    is Folder.Child -> foldersDao.getFolderAllChildFoldersRecursively(folder.folderId, ftsQuery)
                    is Folder.Root -> foldersDao.getAllFolders(ftsQuery)
                }
            },
        ).flow.map { pagingData -> pagingData.map { it.toDomain(permissionsModelMapper) } }
}
