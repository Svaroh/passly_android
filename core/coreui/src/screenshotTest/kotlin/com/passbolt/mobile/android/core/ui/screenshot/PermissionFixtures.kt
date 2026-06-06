package net.svaroh.passly.core.ui.screenshot

import net.svaroh.passly.ui.GroupModel
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.ResourcePermission.READ
import net.svaroh.passly.ui.UserWithAvatar

internal fun userPermission(
    id: String = "1",
    firstName: String = "Ada",
    lastName: String = "Lovelace",
    userName: String = "ada@passbolt.com",
    permission: ResourcePermission = READ,
    isDisabled: Boolean = false,
): UserPermissionModel =
    UserPermissionModel(
        permission = permission,
        permissionId = id,
        user =
            UserWithAvatar(
                userId = id,
                firstName = firstName,
                lastName = lastName,
                userName = userName,
                isDisabled = isDisabled,
                avatarUrl = null,
            ),
    )

internal fun groupPermission(
    id: String = "1",
    groupName: String = "Engineering",
    permission: ResourcePermission = READ,
): GroupPermissionModel =
    GroupPermissionModel(
        permission = permission,
        permissionId = id,
        group =
            GroupModel(
                groupId = id,
                groupName = groupName,
            ),
    )
