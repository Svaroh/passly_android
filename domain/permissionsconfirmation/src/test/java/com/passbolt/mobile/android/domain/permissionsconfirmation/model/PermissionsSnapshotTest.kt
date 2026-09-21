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

package com.passbolt.mobile.android.domain.permissionsconfirmation.model

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot.DriftResult.DriftDetected
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot.DriftResult.NoDrift
import com.passbolt.mobile.android.domain.users.model.GpgKey
import com.passbolt.mobile.android.domain.users.model.UserProfile
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission
import org.junit.Test
import java.time.ZonedDateTime

class PermissionsSnapshotTest {
    @Test
    fun `equal snapshots with different creation times have no drift`() {
        val original = snapshot()
        val current = snapshot(created = ZonedDateTime.now().plusMinutes(5))

        assertThat(current.detectDrift(original)).isEqualTo(NoDrift)
    }

    @Test
    fun `changed permission level is a drift naming the user`() {
        val original = snapshot()
        val current = snapshot(userAPermission = ResourcePermission.OWNER)

        assertThat(current.detectDrift(original)).isEqualTo(DriftDetected(listOf("first-$USER_A last-$USER_A")))
    }

    @Test
    fun `added permission is a drift naming the user`() {
        val original = snapshot()
        val current =
            snapshot(
                additionalPermissions = listOf(userPermission("added-user", ResourcePermission.READ)),
            )

        assertThat(current.detectDrift(original)).isEqualTo(DriftDetected(listOf("added-user")))
    }

    @Test
    fun `changed group membership is a drift naming the group`() {
        val original = snapshot()
        val current = snapshot(groupMembers = listOf(USER_A, USER_B))

        assertThat(current.detectDrift(original)).isEqualTo(DriftDetected(listOf("group-name")))
    }

    @Test
    fun `changed user key fingerprint is a drift naming the user`() {
        val original = snapshot()
        val current = snapshot(userAFingerprint = "changed-fingerprint")

        assertThat(current.detectDrift(original)).isEqualTo(DriftDetected(listOf("first-$USER_A last-$USER_A")))
    }

    @Test
    fun `same members in different order have no drift`() {
        val original = snapshot(groupMembers = listOf(USER_A, USER_B), users = listOf(USER_A, USER_B))
        val current = snapshot(groupMembers = listOf(USER_B, USER_A), users = listOf(USER_A, USER_B))

        assertThat(current.detectDrift(original)).isEqualTo(NoDrift)
    }

    @Test
    fun `user with a direct owner permission is an owner`() {
        assertThat(snapshot(userAPermission = ResourcePermission.OWNER).isUserOwner(USER_A)).isTrue()
    }

    @Test
    fun `user with a lower direct permission is not an owner`() {
        assertThat(snapshot(userAPermission = ResourcePermission.UPDATE).isUserOwner(USER_A)).isFalse()
    }

    @Test
    fun `member of an owner group is an owner`() {
        val snapshot = snapshot(groupPermission = ResourcePermission.OWNER, groupMembers = listOf(USER_B))

        assertThat(snapshot.isUserOwner(USER_B)).isTrue()
    }

    @Test
    fun `member of a non-owner group is not an owner`() {
        val snapshot = snapshot(groupPermission = ResourcePermission.UPDATE, groupMembers = listOf(USER_B))

        assertThat(snapshot.isUserOwner(USER_B)).isFalse()
    }

    @Test
    fun `indirect group access is reported for users who are also group members`() {
        val snapshot = snapshot(groupMembers = listOf(USER_A, USER_B))

        val indirectAccess = snapshot.indirectGroupAccess(userIds = listOf(USER_A), groupIds = listOf(GROUP_ID))

        assertThat(indirectAccess)
            .containsExactly(PermissionsSnapshot.IndirectGroupAccess(userId = USER_A, groupId = GROUP_ID))
    }

    @Test
    fun `indirect group access ignores groups outside of the provided ids`() {
        val snapshot = snapshot(groupMembers = listOf(USER_A))

        assertThat(snapshot.indirectGroupAccess(userIds = listOf(USER_A), groupIds = emptyList())).isEmpty()
    }

    @Test
    fun `indirect group access ignores users outside of the provided ids`() {
        val snapshot = snapshot(groupMembers = listOf(USER_A, USER_B))

        assertThat(snapshot.indirectGroupAccess(userIds = listOf(USER_B), groupIds = listOf(GROUP_ID)))
            .containsExactly(PermissionsSnapshot.IndirectGroupAccess(userId = USER_B, groupId = GROUP_ID))
    }

    @Test
    fun `indirect group access covers additionally provided groups members`() {
        val snapshot = snapshot(groupMembers = emptyList())

        val indirectAccess =
            snapshot.indirectGroupAccess(
                userIds = listOf(USER_A),
                groupIds = listOf(GROUP_ID, ADDED_GROUP_ID),
                addedGroupsMembers = mapOf(ADDED_GROUP_ID to listOf(USER_A)),
            )

        assertThat(indirectAccess)
            .containsExactly(PermissionsSnapshot.IndirectGroupAccess(userId = USER_A, groupId = ADDED_GROUP_ID))
    }

    private fun snapshot(
        userAPermission: ResourcePermission = ResourcePermission.READ,
        additionalPermissions: List<PermissionModel> = emptyList(),
        groupPermission: ResourcePermission = ResourcePermission.UPDATE,
        groupMembers: List<String> = listOf(USER_A),
        users: List<String> = listOf(USER_A),
        userAFingerprint: String = "fingerprint-$USER_A",
        created: ZonedDateTime = ZonedDateTime.now(),
    ) = PermissionsSnapshot(
        permissions =
            listOf(
                userPermission(USER_A, userAPermission),
                groupPermission(GROUP_ID, groupPermission),
            ) + additionalPermissions,
        groupsMembers = mapOf(GROUP_ID to groupMembers),
        users =
            users.associateWith { userId ->
                userProfile(
                    userId = userId,
                    fingerprint = if (userId == USER_A) userAFingerprint else "fingerprint-$userId",
                )
            },
        created = created,
    )

    private fun userPermission(
        userId: String,
        permission: ResourcePermission,
    ) = PermissionModel.UserPermissionModel(
        permission = permission,
        permissionId = "permission-$userId",
        userId = userId,
    )

    private fun groupPermission(
        groupId: String,
        permission: ResourcePermission,
    ) = PermissionModel.GroupPermissionModel(
        permission = permission,
        permissionId = "permission-$groupId",
        group = GroupModel(groupId, "group-name"),
    )

    private fun userProfile(
        userId: String,
        fingerprint: String,
    ) = UserProfile(
        id = userId,
        username = "$userId@passbolt.com",
        disabled = false,
        role = null,
        firstName = "first-$userId",
        lastName = "last-$userId",
        avatarUrl = null,
        gpgKey =
            GpgKey(
                id = "gpg-$userId",
                armoredKey = "armored-key-$userId",
                fingerprint = fingerprint,
                bits = 2048,
                uid = null,
                keyId = "key-$userId",
                type = null,
                keyExpirationDate = null,
                keyCreationDate = null,
            ),
    )

    private companion object {
        const val USER_A = "user-a"
        const val USER_B = "user-b"
        const val GROUP_ID = "group-id"
        const val ADDED_GROUP_ID = "added-group-id"
    }
}
