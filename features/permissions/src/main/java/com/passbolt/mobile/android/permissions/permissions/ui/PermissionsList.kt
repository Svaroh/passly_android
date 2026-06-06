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

package net.svaroh.passly.permissions.permissions.ui

import PassboltTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.svaroh.passly.core.ui.permissions.GroupPermissionRow
import net.svaroh.passly.core.ui.permissions.UserPermissionRow
import net.svaroh.passly.ui.GroupModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.UserWithAvatar

@Composable
internal fun PermissionsList(
    permissions: List<PermissionModelUi>,
    onPermissionClick: (PermissionModelUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(
            items = permissions,
            key = { permission ->
                when (permission) {
                    is GroupPermissionModel -> "group_${permission.group.groupId}"
                    is UserPermissionModel -> "user_${permission.user.userId}"
                }
            },
        ) { permission ->
            when (permission) {
                is GroupPermissionModel ->
                    GroupPermissionRow(
                        permission = permission,
                        modifier = Modifier.clickable { onPermissionClick(permission) },
                    )
                is UserPermissionModel ->
                    UserPermissionRow(
                        permission = permission,
                        modifier = Modifier.clickable { onPermissionClick(permission) },
                    )
            }
        }
    }
}

private val previewPermissions =
    listOf(
        GroupPermissionModel(
            permission = ResourcePermission.OWNER,
            permissionId = "1",
            group = GroupModel(groupId = "1", groupName = "Engineering Team"),
        ),
        UserPermissionModel(
            permission = ResourcePermission.READ,
            permissionId = "2",
            user =
                UserWithAvatar(
                    userId = "1",
                    firstName = "John",
                    lastName = "Doe",
                    userName = "john@passbolt.com",
                    isDisabled = false,
                    avatarUrl = null,
                ),
        ),
        UserPermissionModel(
            permission = ResourcePermission.UPDATE,
            permissionId = "3",
            user =
                UserWithAvatar(
                    userId = "2",
                    firstName = "Jane",
                    lastName = "Smith",
                    userName = "jane@passbolt.com",
                    isDisabled = true,
                    avatarUrl = null,
                ),
        ),
    )

@Preview(showBackground = true)
@Composable
private fun PermissionsListPreview() {
    PassboltTheme {
        PermissionsList(
            permissions = previewPermissions,
            onPermissionClick = {},
        )
    }
}
