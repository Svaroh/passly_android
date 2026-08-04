package com.passbolt.mobile.android.domain.permissionsconfirmation.usecase

import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission

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

class PermissionsSnapshotComparator {
    fun hasDrift(
        original: PermissionsSnapshot,
        current: PermissionsSnapshot,
    ): Boolean =
        permissionEntries(original) != permissionEntries(current) ||
            groupsMemberships(original) != groupsMemberships(current) ||
            usersFingerprints(original) != usersFingerprints(current)

    private fun permissionEntries(snapshot: PermissionsSnapshot): Set<PermissionEntry> =
        snapshot.permissions
            .map {
                when (it) {
                    is PermissionModel.UserPermissionModel ->
                        PermissionEntry(aroId = it.userId, isGroup = false, permission = it.permission)
                    is PermissionModel.GroupPermissionModel ->
                        PermissionEntry(aroId = it.group.groupId, isGroup = true, permission = it.permission)
                }
            }.toSet()

    private fun groupsMemberships(snapshot: PermissionsSnapshot): Map<String, Set<String>> =
        snapshot.groupsMembers.mapValues { it.value.toSet() }

    private fun usersFingerprints(snapshot: PermissionsSnapshot): Map<String, String?> =
        snapshot.users.mapValues { it.value.gpgKey?.fingerprint }

    private data class PermissionEntry(
        val aroId: String,
        val isGroup: Boolean,
        val permission: ResourcePermission,
    )
}
