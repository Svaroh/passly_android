package net.svaroh.passly.permissions.grouppermissionsdetails

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.domain.groups.usecase.GetGroupWithUsersUseCase
import net.svaroh.passly.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import net.svaroh.passly.domain.users.mapper.toUserModel
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.CancelPermissionDelete
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.ConfirmPermissionDelete
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.DeletePermission
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.GoBack
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.Save
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.SeeGroupMembers
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.SelectPermission
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateBack
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateToGroupMembers
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetDeletePermissionResult
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetUpdatedPermissionResult
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.PermissionsMode.EDIT
import net.svaroh.passly.ui.ResourcePermission
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
