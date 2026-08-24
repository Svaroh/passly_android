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
import com.passbolt.mobile.android.domain.permissionsconfirmation.mapper.toCreateModePermissions
import com.passbolt.mobile.android.domain.permissionsconfirmation.mapper.toEditModePermissions
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.SetPermissionsConfirmationOptOutUseCase
import com.passbolt.mobile.android.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import com.passbolt.mobile.android.domain.users.usecase.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.domain.users.usecase.UsersInteractor
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.permissions.common.PermissionsListMapper
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.AddPermission
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.Confirm
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.GroupPermissionDeleted
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.GroupPermissionModified
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.SeePermission
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.ShareRecipientsAdded
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.SkipConfirmationToggled
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.UserPermissionDeleted
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent.UserPermissionModified
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.CloseWithPermissionsConfirmed
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToGroupPermissionDetails
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToSelectShareRecipients
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToUserPermissionDetails
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowPermissionsDriftedSnackbar
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowToast
import com.passbolt.mobile.android.permissions.permissions.validation.HasAtLeastOneOwnerPermission
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.PermissionsMode.EDIT
import com.passbolt.mobile.android.ui.PermissionsMode.VIEW
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber

class ConfirmPermissionsViewModel(
    private val confirmMode: ConfirmPermissionsMode,
    private val driftedEntityNames: List<String>?,
    private val createPermissionsSnapshotInteractor: CreatePermissionsSnapshotInteractor,
    private val getPermissionsSnapshotUseCase: GetPermissionsSnapshotUseCase,
    private val getGroupWithUsersUseCase: GetGroupWithUsersUseCase,
    private val usersInteractor: UsersInteractor,
    private val groupsInteractor: GroupsInteractor,
    private val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase,
    private val usersModelMapper: UsersModelMapper,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val setPermissionsConfirmationOptOutUseCase: SetPermissionsConfirmationOptOutUseCase,
    private val permissionsListMapper: PermissionsListMapper,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<ConfirmPermissionsState, ConfirmPermissionsSideEffect>(
        initialState = ConfirmPermissionsState(),
    ) {
    init {
        loadSnapshotPermissions()
        refreshUsersAndGroups()
        loadSkipConfirmationSwitchVisibility()
        showDriftInfoIfReopenedAfterDrift()
    }

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
            is SkipConfirmationToggled -> updateViewState { copy(isSkipConfirmationChecked = intent.isChecked) }
        }
    }

    private fun loadSnapshotPermissions() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState { copy(isPreparingPermissions = true) }
            when (
                val output =
                    runAuthenticatedOperation {
                        when (confirmMode) {
                            is ConfirmPermissionsMode.Create ->
                                createPermissionsSnapshotInteractor.createForFolder(confirmMode.folderId)
                            is ConfirmPermissionsMode.Edit ->
                                createPermissionsSnapshotInteractor.createForResource(confirmMode.resourceId)
                        }
                    }
            ) {
                is CreatePermissionsSnapshotInteractor.Output.Success -> {
                    val operator =
                        usersModelMapper.mapToUserWithAvatar(
                            getLocalCurrentUserUseCase.execute(Unit).user,
                        )
                    updateViewState { copy(isPreparingPermissions = false) }
                    when (confirmMode) {
                        is ConfirmPermissionsMode.Create -> {
                            updateViewState {
                                copy(
                                    isEditable = output.snapshot.isUserOwner(operator.userId),
                                    lockedOperatorPermission =
                                        UserPermissionModel(
                                            ResourcePermission.OWNER,
                                            SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID,
                                            operator,
                                        ),
                                )
                            }
                            updatePermissions { output.snapshot.toCreateModePermissions(operator) }
                        }
                        is ConfirmPermissionsMode.Edit -> {
                            val editPermissions = output.snapshot.toEditModePermissions()
                            updateViewState {
                                copy(
                                    isEditable = output.snapshot.isUserOwner(operator.userId),
                                    lockedOperatorPermission =
                                        editPermissions
                                            .filterIsInstance<UserPermissionModel>()
                                            .find { it.user.userId == operator.userId },
                                )
                            }
                            updatePermissions { editPermissions }
                        }
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

    private fun loadSkipConfirmationSwitchVisibility() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val isOptOutAvailable =
                getFeatureFlagsUseCase
                    .execute(Unit)
                    .featureFlags.isPermissionsConfirmationOptOutAvailable
            updateViewState { copy(showSkipConfirmationSwitch = isOptOutAvailable) }
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
        viewModelScope.launch(coroutineLaunchContext.io) {
            if (viewState.value.isSkipConfirmationChecked) {
                setPermissionsConfirmationOptOutUseCase.execute(SetPermissionsConfirmationOptOutUseCase.Input(isOptedOut = true))
            }
            Timber.d("Permissions confirmed for ${viewState.value.permissions.size} recipient(s)")
            emitSideEffect(CloseWithPermissionsConfirmed(viewState.value.permissions))
        }
    }
}
