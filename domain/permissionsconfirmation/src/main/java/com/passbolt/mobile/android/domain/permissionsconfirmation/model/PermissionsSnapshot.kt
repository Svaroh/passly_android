package com.passbolt.mobile.android.domain.permissionsconfirmation.model

import com.passbolt.mobile.android.domain.users.model.UserProfile
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission
import java.time.ZonedDateTime

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
data class PermissionsSnapshot(
    val permissions: List<PermissionModel>,
    val groupsMembers: Map<String, List<String>>,
    val users: Map<String, UserProfile>,
    val created: ZonedDateTime,
) {
    fun isUserOwner(userId: String): Boolean {
        val hasDirectOwnership =
            permissions
                .filterIsInstance<PermissionModel.UserPermissionModel>()
                .any { it.userId == userId && it.permission == ResourcePermission.OWNER }
        val hasGroupOwnership =
            permissions
                .filterIsInstance<PermissionModel.GroupPermissionModel>()
                .any { it.permission == ResourcePermission.OWNER && userId in groupsMembers[it.group.groupId].orEmpty() }
        return hasDirectOwnership || hasGroupOwnership
    }

    fun hasDriftedFrom(original: PermissionsSnapshot): Boolean =
        permissionEntries() != original.permissionEntries() ||
            groupsMemberships() != original.groupsMemberships() ||
            usersFingerprints() != original.usersFingerprints()

    private fun permissionEntries(): Set<PermissionEntry> =
        permissions
            .map {
                when (it) {
                    is PermissionModel.UserPermissionModel ->
                        PermissionEntry(aroId = it.userId, isGroup = false, permission = it.permission)
                    is PermissionModel.GroupPermissionModel ->
                        PermissionEntry(aroId = it.group.groupId, isGroup = true, permission = it.permission)
                }
            }.toSet()

    private fun groupsMemberships(): Map<String, Set<String>> = groupsMembers.mapValues { it.value.toSet() }

    private fun usersFingerprints(): Map<String, String?> = users.mapValues { it.value.gpgKey?.fingerprint }

    private data class PermissionEntry(
        val aroId: String,
        val isGroup: Boolean,
        val permission: ResourcePermission,
    )
}
