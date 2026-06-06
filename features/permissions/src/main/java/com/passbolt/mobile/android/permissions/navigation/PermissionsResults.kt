package net.svaroh.passly.permissions.navigation

import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel

data class GroupPermissionModifiedResult(
    val permission: GroupPermissionModel,
)

data class GroupPermissionDeletedResult(
    val permission: GroupPermissionModel,
)

data class UserPermissionModifiedResult(
    val permission: UserPermissionModel,
)

data class UserPermissionDeletedResult(
    val permission: UserPermissionModel,
)

data class ShareRecipientsAddedResult(
    val permissions: List<PermissionModelUi>?,
)
