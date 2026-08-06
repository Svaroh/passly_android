/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.core.ui.sharedwith

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission.READ
import com.passbolt.mobile.android.ui.UserWithAvatar
import org.junit.Assert.assertThrows
import org.junit.Test

class PermissionsDatasetCreatorTest {
    @Test
    fun `permissions that all fit keep their group and user split and get no counter`() {
        val permissions = listOf(group("g1")) + users(2)

        val output =
            PermissionsDatasetCreator(
                permissionsListWidth = 1000,
                permissionItemWidth = 100f,
            ).prepareDataset(permissions)

        assertThat(output.groupPermissions).hasSize(1)
        assertThat(output.userPermissions).hasSize(2)
        assertThat(output.counterValue).isEmpty()
        assertThat(output.overlap).isEqualTo(0)
    }

    @Test
    fun `permissions past the visible count are dropped and counted instead`() {
        val permissions = listOf(group("g1")) + users(9)

        val output =
            PermissionsDatasetCreator(
                permissionsListWidth = 300,
                permissionItemWidth = 100f,
            ).prepareDataset(permissions)

        // 6 items would be visible, two slots are given up so that the counter has room
        assertThat(output.groupPermissions).hasSize(1)
        assertThat(output.userPermissions).hasSize(3)
        assertThat(output.counterValue).containsExactly("6")
        assertThat(output.overlap).isEqualTo(-50)
    }

    @Test
    fun `the counter is a plain number up to the display cap`() {
        val permissions = listOf(group("g1")) + users(101)

        val output =
            PermissionsDatasetCreator(
                permissionsListWidth = 300,
                permissionItemWidth = 100f,
            ).prepareDataset(permissions)

        assertThat(output.counterValue).containsExactly("98")
    }

    @Test
    fun `the counter is capped so that it cannot outgrow its bubble`() {
        val permissions = listOf(group("g1")) + users(102)

        val output =
            PermissionsDatasetCreator(
                permissionsListWidth = 300,
                permissionItemWidth = 100f,
            ).prepareDataset(permissions)

        assertThat(output.counterValue).containsExactly("99+")
    }

    @Test
    fun `an unmeasured container is rejected rather than laid out`() {
        val creator = PermissionsDatasetCreator(permissionsListWidth = 0, permissionItemWidth = 100f)

        assertThrows(IllegalArgumentException::class.java) { creator.prepareDataset(users(1)) }
    }

    @Test
    fun `an unmeasured item is rejected rather than laid out`() {
        val creator = PermissionsDatasetCreator(permissionsListWidth = 300, permissionItemWidth = 0f)

        assertThrows(IllegalArgumentException::class.java) { creator.prepareDataset(users(1)) }
    }

    private fun group(id: String): GroupPermissionModel =
        GroupPermissionModel(
            permission = READ,
            permissionId = id,
            group = GroupModel(groupId = id, groupName = "group $id"),
        )

    private fun users(count: Int): List<PermissionModelUi> =
        List(count) { index ->
            UserPermissionModel(
                permission = READ,
                permissionId = "u$index",
                user =
                    UserWithAvatar(
                        userId = "u$index",
                        firstName = "First$index",
                        lastName = "Last$index",
                        userName = "user$index@passbolt.com",
                        isDisabled = false,
                        avatarUrl = null,
                    ),
            )
        }
}
