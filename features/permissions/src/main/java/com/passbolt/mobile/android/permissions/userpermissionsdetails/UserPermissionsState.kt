package net.svaroh.passly.permissions.userpermissionsdetails

import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel
import net.svaroh.passly.ui.UserModel

data class UserPermissionsState(
    val permission: UserPermissionModel,
    val user: UserModel? = null,
    val isEditMode: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
)
