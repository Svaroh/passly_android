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

package net.svaroh.passly.permissions.confirmpermissions

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.svaroh.passly.common.validation.validation
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.domain.groups.usecase.GetGroupWithUsersUseCase
import net.svaroh.passly.domain.groups.usecase.GroupsInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.domain.permissionsconfirmation.mapper.toCreateModePermissions
import net.svaroh.passly.domain.permissionsconfirmation.mapper.toEditModePermissions
import net.svaroh.passly.domain.permissionsconfirmation.model.PermissionsSnapshot
import net.svaroh.passly.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import net.svaroh.passly.domain.resources.actions.ResourceShareActionsInteractor
import net.svaroh.passly.domain.resources.actions.ShareActionResult
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import net.svaroh.passly.domain.users.usecase.GetLocalCurrentUserUseCase
import net.svaroh.passly.domain.users.usecase.UsersInteractor
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.mappers.UsersModelMapper
import net.svaroh.passly.permissions.common.PermissionsListMapper
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.AddPermission
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.Confirm
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.DismissMetadataKeyDeletedDialog
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.DismissMetadataKeyModifiedDialog
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.GoBack
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.GroupPermissionDeleted
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.GroupPermissionModified
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.SeePermission
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.ShareRecipientsAdded
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.TrustNewMetadataKey
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.TrustedMetadataKeyDeleted
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.UserPermissionDeleted
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.UserPermissionModified
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.CloseWithPermissionsConfirmed
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.CloseWithShareSuccess
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateBack
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToGroupPermissionDetails
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToSelectShareRecipients
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToUserPermissionDetails
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowPermissionsDriftedSnackbar
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowSuccessSnackbar
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowToast
import net.svaroh.passly.permissions.permissions.validation.HasAtLeastOneOwnerPermission
import net.svaroh.passly.serializers.jsonschema.SchemaEntity
import net.svaroh.passly.ui.ConfirmPermissionsMode
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel
import net.svaroh.passly.ui.PermissionsMode.EDIT
import net.svaroh.passly.ui.PermissionsMode.VIEW
import net.svaroh.passly.ui.UserWithAvatar
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
                        is ConfirmPermissionsMode.Edit,
                        is ConfirmPermissionsMode.Share,
                        -> showEditModePermissions(output.snapshot, operator)
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
        updateViewState { copy(isEditable = snapshot.isUserOwner(operator.userId)) }
        updatePermissions { snapshot.toCreateModePermissions() }
    }

    private fun showEditModePermissions(
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
            copy(permissions = permissionsListMapper.sorted(transform(permissions)))
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

    private fun permissionClick(permission: PermissionModelUi) {
        val detailsMode = if (viewState.value.isEditable) EDIT else VIEW
        when (permission) {
            is GroupPermissionModel -> emitSideEffect(NavigateToGroupPermissionDetails(permission, detailsMode))
            is UserPermissionModel -> emitSideEffect(NavigateToUserPermissionDetails(permission, detailsMode))
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
