package com.passbolt.mobile.android.core.ui.screenshot

import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourcePermission.READ
import com.passbolt.mobile.android.ui.UserWithAvatar

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
