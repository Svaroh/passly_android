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

    fun indirectGroupAccess(
        userIds: Collection<String>,
        groupIds: Collection<String>,
        addedGroupsMembers: Map<String, List<String>> = emptyMap(),
    ): List<IndirectGroupAccess> {
        val allGroupsMembers = groupsMembers + addedGroupsMembers
        return groupIds.flatMap { groupId ->
            allGroupsMembers[groupId]
                .orEmpty()
                .filter { it in userIds }
                .map { IndirectGroupAccess(userId = it, groupId = groupId) }
        }
    }

    fun detectDrift(original: PermissionsSnapshot): DriftResult {
        val currentEntries = permissionEntries()
        val originalEntries = original.permissionEntries()
        val driftedEntries = (currentEntries - originalEntries) + (originalEntries - currentEntries)
        val driftedMembershipsGroupIds =
            (groupsMemberships().keys + original.groupsMemberships().keys)
                .filter { groupsMemberships()[it] != original.groupsMemberships()[it] }
        val driftedFingerprintsUserIds =
            (usersFingerprints().keys + original.usersFingerprints().keys)
                .filter { usersFingerprints()[it] != original.usersFingerprints()[it] }
        if (driftedEntries.isEmpty() && driftedMembershipsGroupIds.isEmpty() && driftedFingerprintsUserIds.isEmpty()) {
            return DriftResult.NoDrift
        }

        val driftedUserIds = driftedEntries.filterNot { it.isGroup }.map { it.aroId } + driftedFingerprintsUserIds
        val driftedGroupIds = driftedEntries.filter { it.isGroup }.map { it.aroId } + driftedMembershipsGroupIds
        return DriftResult.DriftDetected(
            driftedEntityNames =
                (
                    driftedUserIds.distinct().map { userName(it, original) } +
                        driftedGroupIds.distinct().map { groupName(it, original) }
                ).distinct(),
        )
    }

    private fun userName(
        userId: String,
        original: PermissionsSnapshot,
    ): String {
        val profile = users[userId] ?: original.users[userId]
        val fullName = listOfNotNull(profile?.firstName, profile?.lastName).joinToString(" ")
        return fullName.ifBlank { profile?.username ?: userId }
    }

    private fun groupName(
        groupId: String,
        original: PermissionsSnapshot,
    ): String =
        (permissions + original.permissions)
            .filterIsInstance<PermissionModel.GroupPermissionModel>()
            .firstOrNull { it.group.groupId == groupId }
            ?.group
            ?.groupName ?: groupId

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

    data class IndirectGroupAccess(
        val userId: String,
        val groupId: String,
    )

    sealed class DriftResult {
        data object NoDrift : DriftResult()

        data class DriftDetected(
            val driftedEntityNames: List<String>,
        ) : DriftResult()
    }
}
