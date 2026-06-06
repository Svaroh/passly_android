package net.svaroh.passly.permissions.userpermissionsdetails

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.users.usecase.db.GetLocalUserUseCase
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.CancelPermissionDelete
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.ConfirmPermissionDelete
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.DeletePermission
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.GoBack
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.Save
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsIntent.SelectPermission
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsSideEffect.NavigateBack
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetDeletePermissionResult
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetUpdatedPermissionResult
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.PermissionsMode.EDIT
import net.svaroh.passly.ui.ResourcePermission
import kotlinx.coroutines.launch

class UserPermissionsViewModel(
    mode: PermissionsMode,
    permission: UserPermissionModel,
    private val getLocalUserUseCase: GetLocalUserUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<UserPermissionsState, UserPermissionsSideEffect>(
        initialState =
            UserPermissionsState(
                permission = permission,
                isEditMode = mode == EDIT,
            ),
    ) {
    init {
        loadUserDetails(permission.user.userId)
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
