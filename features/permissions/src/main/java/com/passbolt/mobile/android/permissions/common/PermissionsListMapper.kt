package net.svaroh.passly.permissions.common

import net.svaroh.passly.permissions.permissions.PermissionModelUiComparator
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel

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
class PermissionsListMapper(
    private val permissionModelUiComparator: PermissionModelUiComparator,
) {
    fun sorted(permissions: List<PermissionModelUi>): List<PermissionModelUi> = permissions.sortedWith(permissionModelUiComparator)

    fun withModifiedUserPermission(
        permissions: List<PermissionModelUi>,
        modified: UserPermissionModel,
    ): List<PermissionModelUi> =
        permissions.map { existing ->
            if (existing is UserPermissionModel && existing.user.userId == modified.user.userId) {
                UserPermissionModel(
                    modified.permission,
                    modified.permissionId,
                    existing.user.copy(),
                )
            } else {
                existing
            }
        }

    fun withDeletedUserPermission(
        permissions: List<PermissionModelUi>,
        deleted: UserPermissionModel,
    ): List<PermissionModelUi> =
        permissions.filterNot {
            it is UserPermissionModel && it.user.userId == deleted.user.userId
        }

    fun withModifiedGroupPermission(
        permissions: List<PermissionModelUi>,
        modified: GroupPermissionModel,
    ): List<PermissionModelUi> =
        permissions.map { existing ->
            if (existing is GroupPermissionModel && existing.group.groupId == modified.group.groupId) {
                GroupPermissionModel(
                    modified.permission,
                    modified.permissionId,
                    existing.group.copy(),
                )
            } else {
                existing
            }
        }

    fun withDeletedGroupPermission(
        permissions: List<PermissionModelUi>,
        deleted: GroupPermissionModel,
    ): List<PermissionModelUi> =
        permissions.filterNot {
            it is GroupPermissionModel && it.group.groupId == deleted.group.groupId
        }
}
