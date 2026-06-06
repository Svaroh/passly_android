package net.svaroh.passly.permissions.grouppermissionsdetails

import net.svaroh.passly.ui.PermissionModelUi

sealed interface GroupPermissionsSideEffect {
    data object NavigateBack : GroupPermissionsSideEffect

    data class NavigateToGroupMembers(
        val groupId: String,
    ) : GroupPermissionsSideEffect

    data class SetUpdatedPermissionResult(
        val permission: PermissionModelUi.GroupPermissionModel,
    ) : GroupPermissionsSideEffect

    data class SetDeletePermissionResult(
        val permission: PermissionModelUi.GroupPermissionModel,
    ) : GroupPermissionsSideEffect
}
