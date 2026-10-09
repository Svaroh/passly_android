package net.svaroh.passly.permissions.grouppermissionsdetails

import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.UserUiModel

data class GroupPermissionsState(
    val groupPermission: GroupPermissionModel,
    val users: List<UserUiModel> = emptyList(),
    val isEditMode: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
)
