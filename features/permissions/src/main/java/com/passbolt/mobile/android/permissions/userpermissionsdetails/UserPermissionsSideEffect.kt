package net.svaroh.passly.permissions.userpermissionsdetails

import net.svaroh.passly.ui.PermissionModelUi

sealed interface UserPermissionsSideEffect {
    data object NavigateBack : UserPermissionsSideEffect

    data class SetUpdatedPermissionResult(
        val permission: PermissionModelUi.UserPermissionModel,
    ) : UserPermissionsSideEffect

    data class SetDeletePermissionResult(
        val permission: PermissionModelUi.UserPermissionModel,
    ) : UserPermissionsSideEffect
}
