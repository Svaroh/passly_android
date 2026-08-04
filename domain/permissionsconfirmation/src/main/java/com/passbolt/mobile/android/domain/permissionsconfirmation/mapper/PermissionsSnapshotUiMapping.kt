/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.domain.permissionsconfirmation.mapper

import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot
import com.passbolt.mobile.android.domain.users.model.UserProfile
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper.Companion.TEMPORARY_NEW_PERMISSION_ID
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar

// in create mode the operator is the sole owner of the new item - the operator's folder-derived
// permission is replaced by a locked owner permission
fun PermissionsSnapshot.toCreateModePermissions(currentUser: UserWithAvatar): List<PermissionModelUi> {
    val groupsPermissions =
        permissions
            .filterIsInstance<PermissionModel.GroupPermissionModel>()
            .map {
                PermissionModelUi.GroupPermissionModel(
                    permission = it.permission,
                    permissionId = TEMPORARY_NEW_PERMISSION_ID,
                    group = it.group,
                )
            }
    val otherUsersPermissions =
        permissions
            .filterIsInstance<PermissionModel.UserPermissionModel>()
            .filter { it.userId != currentUser.userId }
            .mapNotNull { permission ->
                users[permission.userId]?.let { user ->
                    PermissionModelUi.UserPermissionModel(
                        permission = permission.permission,
                        permissionId = TEMPORARY_NEW_PERMISSION_ID,
                        user = user.toUserWithAvatar(),
                    )
                }
            }
    val currentUserPermission =
        PermissionModelUi.UserPermissionModel(
            permission = ResourcePermission.OWNER,
            permissionId = TEMPORARY_NEW_PERMISSION_ID,
            user = currentUser,
        )
    return groupsPermissions + otherUsersPermissions + currentUserPermission
}

fun UserProfile.toUserWithAvatar(): UserWithAvatar =
    UserWithAvatar(
        userId = id,
        firstName = firstName.orEmpty(),
        lastName = lastName.orEmpty(),
        userName = username,
        isDisabled = disabled,
        avatarUrl = avatarUrl,
    )
