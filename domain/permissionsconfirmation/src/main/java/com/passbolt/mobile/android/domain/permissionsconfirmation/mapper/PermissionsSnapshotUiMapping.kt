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

package net.svaroh.passly.domain.permissionsconfirmation.mapper

import net.svaroh.passly.domain.permissionsconfirmation.model.PermissionsSnapshot
import net.svaroh.passly.domain.users.model.UserProfile
import net.svaroh.passly.mappers.SharePermissionsModelMapper.Companion.TEMPORARY_NEW_PERMISSION_ID
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.UserWithAvatar

// in create mode the new resource inherits the parent folder's permission set as-is - the operator
// carries their folder-derived permission and new permissions get a temporary id
fun PermissionsSnapshot.toCreateModePermissions(): List<PermissionModelUi> = toPermissionModelUis { TEMPORARY_NEW_PERMISSION_ID }

fun PermissionsSnapshot.toEditModePermissions(): List<PermissionModelUi> = toPermissionModelUis { realPermissionId -> realPermissionId }

private fun PermissionsSnapshot.toPermissionModelUis(permissionId: (String) -> String): List<PermissionModelUi> {
    val groupsPermissions =
        permissions
            .filterIsInstance<PermissionModel.GroupPermissionModel>()
            .map {
                PermissionModelUi.GroupPermissionModel(
                    permission = it.permission,
                    permissionId = permissionId(it.permissionId),
                    group = it.group,
                )
            }
    val usersPermissions =
        permissions
            .filterIsInstance<PermissionModel.UserPermissionModel>()
            .mapNotNull { permission ->
                users[permission.userId]?.let { user ->
                    PermissionModelUi.UserPermissionModel(
                        permission = permission.permission,
                        permissionId = permissionId(permission.permissionId),
                        user = user.toUserWithAvatar(),
                    )
                }
            }
    return groupsPermissions + usersPermissions
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
