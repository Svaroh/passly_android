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

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot
import com.passbolt.mobile.android.domain.users.model.UserProfile
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper.Companion.TEMPORARY_NEW_PERMISSION_ID
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import org.junit.Test
import java.time.ZonedDateTime

class PermissionsSnapshotUiMappingTest {
    @Test
    fun `operator carries their folder-derived permission unchanged`() {
        val snapshot =
            snapshot(
                permissions =
                    listOf(
                        userPermission(OPERATOR_ID, ResourcePermission.UPDATE),
                        userPermission(USER_ID, ResourcePermission.OWNER),
                    ),
                users = listOf(userProfile(OPERATOR_ID), userProfile(USER_ID)),
            )

        val result = snapshot.toCreateModePermissions()

        val userPermissions = result.filterIsInstance<PermissionModelUi.UserPermissionModel>()
        assertThat(userPermissions.single { it.user.userId == OPERATOR_ID }.permission).isEqualTo(ResourcePermission.UPDATE)
        assertThat(userPermissions.single { it.user.userId == USER_ID }.permission).isEqualTo(ResourcePermission.OWNER)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `no direct operator permission is added when access is group-derived`() {
        val snapshot =
            snapshot(
                permissions = listOf(groupPermission(GROUP_ID)),
                users = emptyList(),
            )

        val result = snapshot.toCreateModePermissions()

        assertThat(result.filterIsInstance<PermissionModelUi.UserPermissionModel>()).isEmpty()
        assertThat(result.filterIsInstance<PermissionModelUi.GroupPermissionModel>()).hasSize(1)
    }

    @Test
    fun `all mapped permissions are marked as new`() {
        val snapshot =
            snapshot(
                permissions =
                    listOf(
                        userPermission(USER_ID, ResourcePermission.READ),
                        groupPermission(GROUP_ID),
                    ),
                users = listOf(userProfile(USER_ID)),
            )

        val result = snapshot.toCreateModePermissions()

        assertThat(result.map { it.permissionId }).containsExactly(
            TEMPORARY_NEW_PERMISSION_ID,
            TEMPORARY_NEW_PERMISSION_ID,
        )
    }

    @Test
    fun `user profile data is mapped to permission user`() {
        val snapshot =
            snapshot(
                permissions = listOf(userPermission(USER_ID, ResourcePermission.UPDATE)),
                users = listOf(userProfile(USER_ID)),
            )

        val result = snapshot.toCreateModePermissions()

        val user = result.filterIsInstance<PermissionModelUi.UserPermissionModel>().single { it.user.userId == USER_ID }.user
        assertThat(user.firstName).isEqualTo("first-$USER_ID")
        assertThat(user.lastName).isEqualTo("last-$USER_ID")
        assertThat(user.userName).isEqualTo("$USER_ID@passbolt.com")
        assertThat(user.isDisabled).isFalse()
    }

    @Test
    fun `user permissions without a fetched profile are skipped`() {
        val snapshot =
            snapshot(
                permissions =
                    listOf(
                        userPermission(USER_ID, ResourcePermission.READ),
                        userPermission(OPERATOR_ID, ResourcePermission.OWNER),
                    ),
                users = listOf(userProfile(OPERATOR_ID)),
            )

        val result = snapshot.toCreateModePermissions()

        val userIds = result.filterIsInstance<PermissionModelUi.UserPermissionModel>().map { it.user.userId }
        assertThat(userIds).containsExactly(OPERATOR_ID)
    }

    private fun snapshot(
        permissions: List<PermissionModel>,
        users: List<UserProfile>,
    ) = PermissionsSnapshot(
        permissions = permissions,
        groupsMembers = emptyMap(),
        users = users.associateBy { it.id },
        created = ZonedDateTime.now(),
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

    private fun userProfile(userId: String) =
        UserProfile(
            id = userId,
            username = "$userId@passbolt.com",
            disabled = false,
            role = null,
            firstName = "first-$userId",
            lastName = "last-$userId",
            avatarUrl = null,
            gpgKey = null,
        )

    private companion object {
        const val OPERATOR_ID = "operator-id"
        const val USER_ID = "user-id"
        const val GROUP_ID = "group-id"
    }
}
