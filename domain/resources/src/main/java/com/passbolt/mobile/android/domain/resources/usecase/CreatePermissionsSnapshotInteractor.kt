package com.passbolt.mobile.android.domain.resources.usecase

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.displayMessage
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.CompleteAuthenticatedOutput
import com.passbolt.mobile.android.core.mvp.authentication.IncompleteAuthenticatedOutput
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.folders.usecase.FetchFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.groups.model.GroupWithMembers
import com.passbolt.mobile.android.domain.groups.usecase.FetchGroupsByIdsUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.PermissionsSnapshotRepository
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot.DriftResult
import com.passbolt.mobile.android.domain.users.model.UserProfile
import com.passbolt.mobile.android.domain.users.usecase.FetchUsersByIdsUseCase
import com.passbolt.mobile.android.ui.PermissionModel
import timber.log.Timber
import java.time.ZonedDateTime

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
class CreatePermissionsSnapshotInteractor(
    private val fetchFolderPermissionsUseCase: FetchFolderPermissionsUseCase,
    private val fetchResourcePermissionsUseCase: FetchResourcePermissionsUseCase,
    private val fetchGroupsByIdsUseCase: FetchGroupsByIdsUseCase,
    private val fetchUsersByIdsUseCase: FetchUsersByIdsUseCase,
    private val permissionsSnapshotRepository: PermissionsSnapshotRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) {
    suspend fun createForFolder(folderId: String): Output {
        Timber.d("Creating permissions snapshot for a folder")
        return createAndStoreSnapshot { buildSnapshotForFolder(folderId) }
    }

    suspend fun createForResource(resourceId: String): Output {
        Timber.d("Creating permissions snapshot for a resource")
        return createAndStoreSnapshot { buildSnapshotForResource(resourceId) }
    }

    suspend fun detectDriftForFolder(folderId: String): DriftOutput = detectDrift { buildSnapshotForFolder(folderId) }

    suspend fun detectDriftForResource(resourceId: String): DriftOutput = detectDrift { buildSnapshotForResource(resourceId) }

    private suspend fun createAndStoreSnapshot(buildFreshSnapshot: suspend () -> Output): Output =
        buildFreshSnapshot().also { output ->
            if (output is Output.Success) {
                storeSnapshot(output.snapshot)
            }
        }

    private suspend fun detectDrift(buildFreshSnapshot: suspend () -> Output): DriftOutput {
        Timber.d("Checking for permissions drift before applying permissions")
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val original =
            permissionsSnapshotRepository.getPermissionsSnapshot(userId)
                ?: return DriftOutput.SnapshotMissing.also {
                    Timber.e("No stored permissions snapshot present for the drift check")
                }
        return when (val fresh = buildFreshSnapshot()) {
            is Output.Success ->
                when (val drift = fresh.snapshot.detectDrift(original)) {
                    is DriftResult.DriftDetected -> {
                        Timber.e("Permissions drift detected for ${drift.driftedEntityNames.size} recipient(s)")
                        DriftOutput.DriftDetected(drift.driftedEntityNames)
                    }
                    DriftResult.NoDrift -> {
                        Timber.d("No permissions drift detected")
                        DriftOutput.NoDrift
                    }
                }
            is Output.Failure -> DriftOutput.Failure(fresh.incomplete)
        }
    }

    private suspend fun buildSnapshotForFolder(folderId: String): Output {
        val permissions =
            when (val result = fetchFolderPermissionsUseCase.execute(FetchFolderPermissionsUseCase.Input(folderId))) {
                is FetchFolderPermissionsUseCase.Output.Success -> result.permissions
                is FetchFolderPermissionsUseCase.Output.Failure -> {
                    Timber.e("Failed to fetch folder permissions for the snapshot: ${result.message}")
                    return Output.Failure(result.incomplete)
                }
            }
        return createSnapshot(permissions)
    }

    private suspend fun buildSnapshotForResource(resourceId: String): Output {
        val permissions =
            when (val result = fetchResourcePermissionsUseCase.execute(FetchResourcePermissionsUseCase.Input(resourceId))) {
                is FetchResourcePermissionsUseCase.Output.Success -> result.permissions
                is FetchResourcePermissionsUseCase.Output.Failure -> {
                    Timber.e("Failed to fetch resource permissions for the snapshot: ${result.message}")
                    return Output.Failure(result.incomplete)
                }
            }
        return createSnapshot(permissions)
    }

    private suspend fun createSnapshot(permissions: List<PermissionModel>): Output {
        val groups =
            when (val result = fetchPermissionsGroups(permissions)) {
                is FetchGroupsByIdsUseCase.Output.Success -> result.groups
                is FetchGroupsByIdsUseCase.Output.Failure -> {
                    Timber.e("Failed to fetch groups for the snapshot: ${result.message}")
                    return Output.Failure(result.incomplete)
                }
            }
        val users =
            when (val result = fetchPermissionsUsers(permissions, groups)) {
                is FetchUsersByIdsUseCase.Output.Success -> result.users
                is FetchUsersByIdsUseCase.Output.Failure -> {
                    Timber.e("Failed to fetch users for the snapshot: ${result.message}")
                    return Output.Failure(result.incomplete)
                }
            }
        return Output.Success(resolveSnapshot(permissions, groups, users))
    }

    private suspend fun storeSnapshot(snapshot: PermissionsSnapshot) {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        permissionsSnapshotRepository.setPermissionsSnapshot(userId, snapshot)
    }

    private suspend fun fetchPermissionsGroups(permissions: List<PermissionModel>): FetchGroupsByIdsUseCase.Output {
        val groupIds =
            permissions
                .filterIsInstance<PermissionModel.GroupPermissionModel>()
                .map { it.group.groupId }
        return if (groupIds.isEmpty()) {
            FetchGroupsByIdsUseCase.Output.Success(emptyList())
        } else {
            fetchGroupsByIdsUseCase.execute(FetchGroupsByIdsUseCase.Input(groupIds))
        }
    }

    private suspend fun fetchPermissionsUsers(
        permissions: List<PermissionModel>,
        groups: List<GroupWithMembers>,
    ): FetchUsersByIdsUseCase.Output {
        val directUserIds =
            permissions
                .filterIsInstance<PermissionModel.UserPermissionModel>()
                .map { it.userId }
        val memberUserIds = groups.flatMap { group -> group.members.map { it.userId } }
        val userIds = (directUserIds + memberUserIds).distinct()
        return if (userIds.isEmpty()) {
            FetchUsersByIdsUseCase.Output.Success(emptyList())
        } else {
            fetchUsersByIdsUseCase.execute(FetchUsersByIdsUseCase.Input(userIds))
        }
    }

    private fun resolveSnapshot(
        permissions: List<PermissionModel>,
        groups: List<GroupWithMembers>,
        users: List<UserProfile>,
    ): PermissionsSnapshot {
        val usersById = users.associateBy { it.id }
        val resolvedPermissions =
            permissions.filter { it !is PermissionModel.UserPermissionModel || it.userId in usersById }
        val droppedPermissionsCount = permissions.size - resolvedPermissions.size
        if (droppedPermissionsCount > 0) {
            Timber.w("Dropping $droppedPermissionsCount user permission(s) referencing users that could not be fetched")
        }
        val groupsMembers =
            groups.associate { groupWithMembers ->
                groupWithMembers.group.id to
                    groupWithMembers.members
                        .map { it.userId }
                        .filter { it in usersById }
            }
        Timber.d("Permissions snapshot created.")

        return PermissionsSnapshot(
            permissions = resolvedPermissions,
            groupsMembers = groupsMembers,
            users = usersById,
            created = ZonedDateTime.now(),
        )
    }

    sealed class Output : AuthenticatedUseCaseOutput {
        data class Success(
            val snapshot: PermissionsSnapshot,
        ) : Output(),
            CompleteAuthenticatedOutput

        data class Failure(
            override val incomplete: DomainResult.Incomplete,
        ) : Output(),
            IncompleteAuthenticatedOutput {
            val message: String?
                get() = incomplete.displayMessage()
        }
    }

    sealed class DriftOutput : AuthenticatedUseCaseOutput {
        data object NoDrift :
            DriftOutput(),
            CompleteAuthenticatedOutput

        data object SnapshotMissing :
            DriftOutput(),
            CompleteAuthenticatedOutput

        data class DriftDetected(
            val driftedEntityNames: List<String>,
        ) : DriftOutput(),
            CompleteAuthenticatedOutput

        data class Failure(
            override val incomplete: DomainResult.Incomplete,
        ) : DriftOutput(),
            IncompleteAuthenticatedOutput {
            val message: String?
                get() = incomplete.displayMessage()
        }
    }
}
