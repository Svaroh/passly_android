package net.svaroh.passly.permissions.grouppermissionsdetails

import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.UserModel

data class GroupPermissionsState(
    val groupPermission: GroupPermissionModel,
    val users: List<UserModel> = emptyList(),
    val isEditMode: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
)
