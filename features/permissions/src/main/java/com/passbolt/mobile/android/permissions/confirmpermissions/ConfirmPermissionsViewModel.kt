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

package com.passbolt.mobile.android.permissions.confirmpermissions

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.domain.groups.usecase.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.domain.groups.usecase.GroupsInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.domain.permissionsconfirmation.mapper.toCreateModePermissions
import com.passbolt.mobile.android.domain.permissionsconfirmation.mapper.toEditModePermissions
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import com.passbolt.mobile.android.domain.resources.actions.ResourceShareActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.ShareActionResult
import com.passbolt.mobile.android.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import com.passbolt.mobile.android.domain.users.usecase.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.domain.users.usecase.UsersInteractor
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.permissions.common.PermissionsListMapper
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.AddPermission
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.Confirm
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.DismissMetadataKeyDeletedDialog
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.DismissMetadataKeyModifiedDialog
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.GroupPermissionDeleted
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.GroupPermissionModified
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.SeePermission
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.ShareRecipientsAdded
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.TrustedMetadataKeyDeleted
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.UserPermissionDeleted
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.UserPermissionModified
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.CloseWithPermissionsConfirmed
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.CloseWithShareSuccess
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToGroupPermissionDetails
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToSelectShareRecipients
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToUserPermissionDetails
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowPermissionsDriftedSnackbar
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowToast
import com.passbolt.mobile.android.permissions.permissions.validation.HasAtLeastOneOwnerPermission
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.PermissionsMode.EDIT
import com.passbolt.mobile.android.ui.PermissionsMode.VIEW
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber

class ConfirmPermissionsViewModel(
    private val confirmMode: ConfirmPermissionsMode,
    private val driftedEntityNames: List<String>?,
    private val createPermissionsSnapshotInteractor: CreatePermissionsSnapshotInteractor,
    private val resourceShareActionsInteractor: ResourceShareActionsInteractor,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    private val getPermissionsSnapshotUseCase: GetPermissionsSnapshotUseCase,
    private val getGroupWithUsersUseCase: GetGroupWithUsersUseCase,
    private val usersInteractor: UsersInteractor,
    private val groupsInteractor: GroupsInteractor,
    private val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase,
    private val usersModelMapper: UsersModelMapper,
    private val permissionsListMapper: PermissionsListMapper,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<ConfirmPermissionsState, ConfirmPermissionsSideEffect>(
        initialState = ConfirmPermissionsState(),
    ) {
    init {
        loadSnapshotPermissions()
        refreshUsersAndGroups()
        showDriftInfoIfReopenedAfterDrift()
    }

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: ConfirmPermissionsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            Confirm -> confirmClick()
            AddPermission -> addPermissionClick()
            is SeePermission -> permissionClick(intent.permission)
            is ShareRecipientsAdded -> shareRecipientsAdded(intent.recipients)
            is UserPermissionModified ->
                updatePermissions { permissionsListMapper.withModifiedUserPermission(it, intent.permission) }
            is UserPermissionDeleted ->
                updatePermissions { permissionsListMapper.withDeletedUserPermission(it, intent.permission) }
            is GroupPermissionModified ->
                updatePermissions { permissionsListMapper.withModifiedGroupPermission(it, intent.permission) }
            is GroupPermissionDeleted ->
                updatePermissions { permissionsListMapper.withDeletedGroupPermission(it, intent.permission) }
            TrustNewMetadataKey -> trustNewMetadataKey()
            TrustedMetadataKeyDeleted -> trustedMetadataKeyDeleted()
            DismissMetadataKeyModifiedDialog ->
                updateViewState { copy(showMetadataKeyModifiedDialog = false, newMetadataKeyToTrustModel = null) }
            DismissMetadataKeyDeletedDialog ->
                updateViewState { copy(showMetadataKeyDeletedDialog = false, trustedKeyDeletedModel = null) }
        }
    }

    private fun loadSnapshotPermissions() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState { copy(isPreparingPermissions = true) }
            when (val output = runAuthenticatedOperation { createSnapshot() }) {
                is CreatePermissionsSnapshotInteractor.Output.Success -> {
                    val operator =
                        usersModelMapper.mapToUserWithAvatar(
                            getLocalCurrentUserUseCase.execute(Unit).user,
                        )
                    updateViewState { copy(isPreparingPermissions = false) }
                    when (confirmMode) {
                        is ConfirmPermissionsMode.Create -> showCreateModePermissions(output.snapshot, operator)
                        is ConfirmPermissionsMode.Edit -> showEditModePermissions(output.snapshot, operator)
                        is ConfirmPermissionsMode.Share -> showShareModePermissions(output.snapshot, operator)
                    }
                }
                is CreatePermissionsSnapshotInteractor.Output.Failure -> {
                    Timber.e("Failed to create permissions snapshot: ${output.message}")
                    emitSideEffect(ShowToast(ToastType.PERMISSIONS_FETCH_FAILURE))
                    emitSideEffect(NavigateBack)
                }
            }
        }
    }

    private suspend fun createSnapshot(): CreatePermissionsSnapshotInteractor.Output =
        when (confirmMode) {
            is ConfirmPermissionsMode.Create -> createPermissionsSnapshotInteractor.createForFolder(confirmMode.folderId)
            is ConfirmPermissionsMode.Edit -> createPermissionsSnapshotInteractor.createForResource(confirmMode.resourceId)
            is ConfirmPermissionsMode.Share -> createPermissionsSnapshotInteractor.createForResource(confirmMode.resourceId)
        }

    private fun showCreateModePermissions(
        snapshot: PermissionsSnapshot,
        operator: UserWithAvatar,
    ) {
        updateViewState {
            copy(
                isEditable = snapshot.isUserOwner(operator.userId),
                lockedOperatorPermission =
                    UserPermissionModel(
                        ResourcePermission.OWNER,
                        SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID,
                        operator,
                    ),
            )
        }
        updatePermissions { snapshot.toCreateModePermissions(operator) }
    }

    private fun showEditModePermissions(
        snapshot: PermissionsSnapshot,
        operator: UserWithAvatar,
    ) {
        val editPermissions = snapshot.toEditModePermissions()
        updateViewState {
            copy(
                isEditable = snapshot.isUserOwner(operator.userId),
                lockedOperatorPermission =
                    editPermissions
                        .filterIsInstance<UserPermissionModel>()
                        .find { it.user.userId == operator.userId },
            )
        }
        updatePermissions { editPermissions }
    }

    private fun showShareModePermissions(
        snapshot: PermissionsSnapshot,
        operator: UserWithAvatar,
    ) {
        updateViewState { copy(isEditable = snapshot.isUserOwner(operator.userId)) }
        updatePermissions { snapshot.toEditModePermissions() }
    }

    private fun refreshUsersAndGroups() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState { copy(isRefreshingUsersAndGroups = true) }
            try {
                Timber.d("Refreshing users and groups before permissions confirmation")
                val usersRefresh = async { runAuthenticatedOperation { usersInteractor.fetchAndSaveUsers() } }
                val groupsRefresh = async { runAuthenticatedOperation { groupsInteractor.fetchAndSaveGroups() } }
                if (usersRefresh.await() !is UsersInteractor.Output.Success) {
                    Timber.e("Failed to refresh users before permissions confirmation - using local data")
                }
                if (groupsRefresh.await() !is GroupsInteractor.Output.Success) {
                    Timber.e("Failed to refresh groups before permissions confirmation - using local data")
                }
            } finally {
                updateViewState { copy(isRefreshingUsersAndGroups = false) }
            }
        }
    }

    private fun showDriftInfoIfReopenedAfterDrift() {
        driftedEntityNames?.let {
            emitSideEffect(ShowPermissionsDriftedSnackbar(it))
        }
    }

    private fun updatePermissions(transform: (List<PermissionModelUi>) -> List<PermissionModelUi>) {
        updateViewState {
            copy(
                permissions =
                    permissionsListMapper.sorted(
                        transform(permissions).withEnforcedOperatorOwnership(lockedOperatorPermission),
                    ),
            )
        }
        refreshIndirectAccessWarning()
    }

    private fun refreshIndirectAccessWarning() {
        viewModelScope.launch(coroutineLaunchContext.default) {
            val snapshot = getPermissionsSnapshotUseCase.execute(Unit).snapshot
            val addedGroupsMembers = addedGroupsLocalMembers(snapshot)
            updateViewState { copy(indirectAccessWarning = indirectAccessWarning(snapshot, permissions, addedGroupsMembers)) }
        }
    }

    private suspend fun addedGroupsLocalMembers(snapshot: PermissionsSnapshot?): Map<String, List<String>> =
        viewState.value.permissions
            .filterIsInstance<GroupPermissionModel>()
            .map { it.group.groupId }
            .filter { snapshot == null || it !in snapshot.groupsMembers }
            .associateWith { groupId ->
                getGroupWithUsersUseCase
                    .execute(GetGroupWithUsersUseCase.Input(groupId))
                    .groupWithUsers.users
                    .map { it.id }
            }

    private fun indirectAccessWarning(
        snapshot: PermissionsSnapshot?,
        permissions: List<PermissionModelUi>,
        addedGroupsMembers: Map<String, List<String>>,
    ): IndirectAccessWarning? {
        val usersPermissions = permissions.filterIsInstance<UserPermissionModel>()
        val groupsPermissions = permissions.filterIsInstance<GroupPermissionModel>()
        val indirectAccess =
            snapshot
                ?.indirectGroupAccess(
                    userIds = usersPermissions.map { it.user.userId },
                    groupIds = groupsPermissions.map { it.group.groupId },
                    addedGroupsMembers = addedGroupsMembers,
                ).orEmpty()
        val indirectUserNames =
            indirectAccess
                .map { it.userId }
                .distinct()
                .mapNotNull { userId -> usersPermissions.find { it.user.userId == userId }?.user }
                .map { "${it.firstName} ${it.lastName}" }
        return when {
            indirectUserNames.isEmpty() -> null
            indirectUserNames.size == 1 ->
                IndirectAccessWarning.SingleUser(
                    userName = indirectUserNames.single(),
                    groupName =
                        groupsPermissions
                            .first { it.group.groupId == indirectAccess.first().groupId }
                            .group.groupName,
                )
            else -> IndirectAccessWarning.MultipleUsers(indirectUserNames)
        }
    }

    private fun trustNewMetadataKey() {
        val model = viewState.value.newMetadataKeyToTrustModel ?: return
        updateViewState { copy(showMetadataKeyModifiedDialog = false, newMetadataKeyToTrustModel = null) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState { copy(isApplyingShare = true) }
            when (
                val output =
                    runAuthenticatedOperation {
                        metadataPrivateKeysHelperInteractor.trustNewKey(model)
                    }
            ) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    emitSideEffect(ShowSuccessSnackbar(SnackbarSuccessType.METADATA_KEY_IS_TRUSTED))
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.FAILED_TO_TRUST_METADATA_KEY))
                }
            }
            updateViewState { copy(isApplyingShare = false) }
        }
    }

    private fun trustedMetadataKeyDeleted() {
        updateViewState { copy(showMetadataKeyDeletedDialog = false, trustedKeyDeletedModel = null) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
        }
    }

    private fun List<PermissionModelUi>.withEnforcedOperatorOwnership(
        lockedOperatorPermission: UserPermissionModel?,
    ): List<PermissionModelUi> {
        if (lockedOperatorPermission == null) return this
        return filterNot { it is UserPermissionModel && it.user.userId == lockedOperatorPermission.user.userId } +
            lockedOperatorPermission
    }

    private fun permissionClick(permission: PermissionModelUi) {
        val detailsMode = if (viewState.value.isEditable) EDIT else VIEW
        when (permission) {
            is GroupPermissionModel -> emitSideEffect(NavigateToGroupPermissionDetails(permission, detailsMode))
            is UserPermissionModel -> {
                val isLockedOperatorPermission =
                    permission.user.userId ==
                        viewState.value.lockedOperatorPermission
                            ?.user
                            ?.userId
                emitSideEffect(
                    NavigateToUserPermissionDetails(
                        permission = permission,
                        mode = if (isLockedOperatorPermission) VIEW else detailsMode,
                    ),
                )
            }
        }
    }

    private fun addPermissionClick() {
        val permissions = viewState.value.permissions
        emitSideEffect(
            NavigateToSelectShareRecipients(
                permissions.filterIsInstance<GroupPermissionModel>(),
                permissions.filterIsInstance<UserPermissionModel>(),
            ),
        )
    }

    private fun shareRecipientsAdded(recipients: List<PermissionModelUi>?) {
        recipients?.let { newRecipients ->
            updatePermissions { newRecipients.toList() }
        }
    }

    private fun confirmClick() {
        validation {
            of(viewState.value.permissions) {
                withRules(HasAtLeastOneOwnerPermission) {
                    onInvalid { emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.ONE_OWNER_REQUIRED)) }
                }
            }
            onValid {
                confirmPermissions()
            }
        }
    }

    private fun confirmPermissions() {
        when (confirmMode) {
            is ConfirmPermissionsMode.Share -> performConfirmedShare(confirmMode.resourceId)
            else -> {
                Timber.d("Permissions confirmed for ${viewState.value.permissions.size} recipient(s)")
                emitSideEffect(CloseWithPermissionsConfirmed(viewState.value.permissions))
            }
        }
    }

    private fun performConfirmedShare(resourceId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState { copy(isApplyingShare = true) }
            Timber.d("Sharing with confirmed permissions for ${viewState.value.permissions.size} recipient(s)")
            val result =
                resourceShareActionsInteractor.shareWithConfirmedPermissions(
                    resourceId = resourceId,
                    confirmedPermissions = viewState.value.permissions,
                )
            updateViewState { copy(isApplyingShare = false) }
            when (result) {
                is ShareActionResult.Success -> emitSideEffect(CloseWithShareSuccess)
                is ShareActionResult.PermissionsDrifted -> {
                    loadSnapshotPermissions()
                    emitSideEffect(ShowPermissionsDriftedSnackbar(result.driftedEntityNames))
                }
                is ShareActionResult.ShareFailure -> {
                    Timber.e("Failed to share with confirmed permissions: ${result.message}")
                    emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.SHARE_FAILED))
                }
                is ShareActionResult.CryptoFailure -> {
                    Timber.e("Encryption failure during the confirmed share: ${result.message}")
                    emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.ENCRYPTION_ERROR))
                }
                is ShareActionResult.SchemaValidationFailure ->
                    emitSideEffect(
                        ShowErrorSnackbar(
                            when (result.entity) {
                                SchemaEntity.RESOURCE -> SnackbarErrorType.JSON_RESOURCE_SCHEMA_ERROR
                                SchemaEntity.SECRET -> SnackbarErrorType.JSON_SECRET_SCHEMA_ERROR
                            },
                        ),
                    )
                is ShareActionResult.CannotUpdateWithCurrentConfig ->
                    emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.CANNOT_UPDATE_TOTP_WITH_CURRENT_CONFIG))
                is ShareActionResult.MetadataKeyVerificationFailure ->
                    emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.FAILED_TO_VERIFY_METADATA_KEY))
                is ShareActionResult.MetadataKeyModified ->
                    updateViewState {
                        copy(showMetadataKeyModifiedDialog = true, newMetadataKeyToTrustModel = result.keyToTrust)
                    }
                is ShareActionResult.MetadataKeyDeleted ->
                    updateViewState {
                        copy(showMetadataKeyDeletedDialog = true, trustedKeyDeletedModel = result.deletedKey)
                    }
                is ShareActionResult.Unauthorized -> {
                    // session handling is performed by runAuthenticatedOperation
                }
            }
        }
    }
}
