package com.passbolt.mobile.android.permissions.grouppermissionsdetails

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.domain.groups.usecase.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import com.passbolt.mobile.android.domain.users.mapper.toUserModel
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.CancelPermissionDelete
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.ConfirmPermissionDelete
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.DeletePermission
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.Save
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.SeeGroupMembers
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.SelectPermission
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateToGroupMembers
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetDeletePermissionResult
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetUpdatedPermissionResult
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.PermissionsMode.EDIT
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.launch

class GroupPermissionsViewModel(
    mode: PermissionsMode,
    permission: PermissionModelUi.GroupPermissionModel,
    private val fromSnapshot: Boolean,
    private val getGroupWithUsersUseCase: GetGroupWithUsersUseCase,
    private val getPermissionsSnapshotUseCase: GetPermissionsSnapshotUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<GroupPermissionsState, GroupPermissionsSideEffect>(
        initialState =
            GroupPermissionsState(
                groupPermission = permission,
                isEditMode = mode == EDIT,
            ),
    ) {
    init {
        if (fromSnapshot) {
            loadSnapshotGroupMembers(permission.group.groupId)
        } else {
            loadGroupDetails(permission.group.groupId)
        }
    }

    fun onIntent(intent: GroupPermissionsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            SeeGroupMembers ->
                emitSideEffect(
                    NavigateToGroupMembers(
                        groupId = requireNotNull(viewState.value.groupPermission).group.groupId,
                        fromSnapshot = fromSnapshot,
                    ),
                )
            DeletePermission -> updateViewState { copy(isDeleteConfirmationVisible = true) }
            ConfirmPermissionDelete -> deletePermission()
            CancelPermissionDelete -> updateViewState { copy(isDeleteConfirmationVisible = false) }
            is SelectPermission -> selectPermission(intent.permission)
            Save -> save()
        }
    }

    private fun loadSnapshotGroupMembers(groupId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val snapshot = getPermissionsSnapshotUseCase.execute(Unit).snapshot
            when {
                // the snapshot is not available (i.e. after process death) - close instead of showing unconfirmed
                snapshot == null -> emitSideEffect(NavigateBack)
                groupId in snapshot.groupsMembers -> {
                    val members =
                        snapshot.groupsMembers
                            .getValue(groupId)
                            .mapNotNull { memberId -> snapshot.users[memberId] }
                            .map { it.toUserModel() }
                    updateViewState { copy(users = members) }
                }
                // not part of the snapshot - a group added from the local search during confirmation
                else -> loadGroupDetails(groupId)
            }
        }
    }

    private fun loadGroupDetails(groupId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val groupWithUsers =
                getGroupWithUsersUseCase
                    .execute(
                        GetGroupWithUsersUseCase.Input(groupId),
                    ).groupWithUsers

            updateViewState { copy(users = groupWithUsers.users) }
        }
    }

    private fun selectPermission(permission: ResourcePermission) {
        val groupPermission = requireNotNull(viewState.value.groupPermission)
        updateViewState { copy(groupPermission = groupPermission.copy(permission = permission)) }
    }

    private fun save() {
        emitSideEffect(SetUpdatedPermissionResult(requireNotNull(viewState.value.groupPermission)))
    }

    private fun deletePermission() {
        updateViewState { copy(isDeleteConfirmationVisible = false) }
        emitSideEffect(SetDeletePermissionResult(requireNotNull(viewState.value.groupPermission)))
    }
}
