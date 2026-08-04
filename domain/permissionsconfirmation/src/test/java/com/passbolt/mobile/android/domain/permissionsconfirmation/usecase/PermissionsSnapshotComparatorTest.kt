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

package com.passbolt.mobile.android.domain.permissionsconfirmation.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot
import com.passbolt.mobile.android.domain.users.model.GpgKey
import com.passbolt.mobile.android.domain.users.model.UserProfile
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission
import org.junit.Test
import java.time.ZonedDateTime

class PermissionsSnapshotComparatorTest {
    private val comparator = PermissionsSnapshotComparator()

    @Test
    fun `equal snapshots with different creation times have no drift`() {
        val original = snapshot()
        val current = snapshot(created = ZonedDateTime.now().plusMinutes(5))

        assertThat(comparator.hasDrift(original, current)).isFalse()
    }

    @Test
    fun `changed permission level is a drift`() {
        val original = snapshot()
        val current = snapshot(userAPermission = ResourcePermission.OWNER)

        assertThat(comparator.hasDrift(original, current)).isTrue()
    }

    @Test
    fun `added permission is a drift`() {
        val original = snapshot()
        val current =
            snapshot(
                additionalPermissions = listOf(userPermission("added-user", ResourcePermission.READ)),
            )

        assertThat(comparator.hasDrift(original, current)).isTrue()
    }

    @Test
    fun `changed group membership is a drift`() {
        val original = snapshot()
        val current = snapshot(groupMembers = listOf(USER_A, USER_B))

        assertThat(comparator.hasDrift(original, current)).isTrue()
    }

    @Test
    fun `changed user key fingerprint is a drift`() {
        val original = snapshot()
        val current = snapshot(userAFingerprint = "changed-fingerprint")

        assertThat(comparator.hasDrift(original, current)).isTrue()
    }

    @Test
    fun `same members in different order have no drift`() {
        val original = snapshot(groupMembers = listOf(USER_A, USER_B), users = listOf(USER_A, USER_B))
        val current = snapshot(groupMembers = listOf(USER_B, USER_A), users = listOf(USER_A, USER_B))

        assertThat(comparator.hasDrift(original, current)).isFalse()
    }

    private fun snapshot(
        userAPermission: ResourcePermission = ResourcePermission.READ,
        additionalPermissions: List<PermissionModel> = emptyList(),
        groupMembers: List<String> = listOf(USER_A),
        users: List<String> = listOf(USER_A),
        userAFingerprint: String = "fingerprint-$USER_A",
        created: ZonedDateTime = ZonedDateTime.now(),
    ) = PermissionsSnapshot(
        permissions =
            listOf(
                userPermission(USER_A, userAPermission),
                groupPermission(GROUP_ID),
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

    private fun groupPermission(groupId: String) =
        PermissionModel.GroupPermissionModel(
            permission = ResourcePermission.UPDATE,
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
    }
}
