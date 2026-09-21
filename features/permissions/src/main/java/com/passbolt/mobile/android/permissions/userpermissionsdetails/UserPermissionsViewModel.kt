package com.passbolt.mobile.android.permissions.userpermissionsdetails

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import com.passbolt.mobile.android.domain.users.mapper.toUserModel
import com.passbolt.mobile.android.domain.users.usecase.GetLocalUserUseCase
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.CancelPermissionDelete
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.ConfirmPermissionDelete
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.DeletePermission
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.Save
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.SelectPermission
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetDeletePermissionResult
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetUpdatedPermissionResult
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.PermissionsMode.EDIT
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.launch

class UserPermissionsViewModel(
    mode: PermissionsMode,
    permission: UserPermissionModel,
    fromSnapshot: Boolean,
    private val getLocalUserUseCase: GetLocalUserUseCase,
    private val getPermissionsSnapshotUseCase: GetPermissionsSnapshotUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<UserPermissionsState, UserPermissionsSideEffect>(
        initialState =
            UserPermissionsState(
                permission = permission,
                isEditMode = mode == EDIT,
            ),
    ) {
    init {
        if (fromSnapshot) {
            loadSnapshotUserDetails(permission.user.userId)
        } else {
            loadUserDetails(permission.user.userId)
        }
    }

    fun onIntent(intent: UserPermissionsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            is SelectPermission -> selectPermission(intent.permission)
            Save -> save()
            DeletePermission -> updateViewState { copy(isDeleteConfirmationVisible = true) }
            CancelPermissionDelete -> updateViewState { copy(isDeleteConfirmationVisible = false) }
            ConfirmPermissionDelete -> deletePermission()
        }
    }

    private fun loadSnapshotUserDetails(userId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val snapshot = getPermissionsSnapshotUseCase.execute(Unit).snapshot
            when {
                // the snapshot is not available (i.e. after process death) - close instead of showing unconfirmed
                snapshot == null -> emitSideEffect(NavigateBack)
                userId in snapshot.users -> updateViewState { copy(user = requireNotNull(snapshot.users[userId]).toUserModel()) }
                // not part of the snapshot - a recipient added from the local search during confirmation
                else -> loadUserDetails(userId)
            }
        }
    }

    private fun loadUserDetails(userId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val user =
                getLocalUserUseCase
                    .execute(
                        GetLocalUserUseCase.Input(userId),
                    ).user

            updateViewState { copy(user = user) }
        }
    }

    private fun selectPermission(permission: ResourcePermission) {
        val userPermission = requireNotNull(viewState.value.permission)
        updateViewState { copy(permission = userPermission.copy(permission = permission)) }
    }

    private fun save() {
        emitSideEffect(SetUpdatedPermissionResult(requireNotNull(viewState.value.permission)))
    }

    private fun deletePermission() {
        updateViewState { copy(isDeleteConfirmationVisible = false) }
        emitSideEffect(SetDeletePermissionResult(requireNotNull(viewState.value.permission)))
    }
}
