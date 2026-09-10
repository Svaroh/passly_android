package com.passbolt.mobile.android.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.navigation.compose.NavigationHostFeatures
import com.passbolt.mobile.android.core.navigation.compose.base.Feature
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.featureEntryProvider
import com.passbolt.mobile.android.core.navigation.compose.keys.GroupDetailsNavigationKey.GroupMemberDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.GroupDetailsNavigationKey.GroupMembers
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.ConfirmPermissions
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.GroupPermissionDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.PermissionRecipients
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.UserPermissionDetails
import com.passbolt.mobile.android.initializers.KoinInitializer
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode.Create
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode.Edit
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import org.junit.Test
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication

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

class NavigationHostRegistrationTest {
    private val koin = koinApplication { modules(KoinInitializer.appModules) }.koin

    @Test
    fun `otp host routes permissions confirmation when linking a scanned totp to a shared resource`() {
        assertRoutes(NavigationHostFeatures.otp, ConfirmPermissions(Edit(RESOURCE_ID)))
    }

    @Test
    fun `otp host routes permissions confirmation when creating a standalone totp in a shared folder`() {
        assertRoutes(NavigationHostFeatures.otp, ConfirmPermissions(Create(FOLDER_ID)))
    }

    @Test
    fun `otp host routes every screen reachable from permissions confirmation`() {
        PERMISSIONS_CONFIRMATION_FLOW.forEach { assertRoutes(NavigationHostFeatures.otp, it) }
    }

    @Test
    fun `home host routes every screen reachable from permissions confirmation`() {
        PERMISSIONS_CONFIRMATION_FLOW.forEach { assertRoutes(NavigationHostFeatures.home, it) }
    }

    @Test
    fun `key not registered by any host feature is reported`() {
        val error =
            runCatching {
                assertRoutes(NavigationHostFeatures.authentication, ConfirmPermissions(Edit(RESOURCE_ID)))
            }.exceptionOrNull()

        assertThat(error).isInstanceOf(UnregisteredDestination::class.java)
    }

    private fun assertRoutes(
        hostFeatures: Set<Feature>,
        key: NavKey,
    ) {
        val featureModulesNavigation =
            hostFeatures.mapTo(mutableSetOf()) { koin.get<FeatureModuleNavigation>(named(it)) }
        val entryProvider =
            featureEntryProvider(featureModulesNavigation) { throw UnregisteredDestination(it, hostFeatures) }

        entryProvider(key)
    }

    private class UnregisteredDestination(
        key: NavKey,
        hostFeatures: Set<Feature>,
    ) : AssertionError("${key::class.simpleName} is not registered by any of $hostFeatures")

    private companion object {
        private const val RESOURCE_ID = "resource-id"
        private const val FOLDER_ID = "folder-id"
        private const val GROUP_ID = "group-id"
        private const val USER_ID = "user-id"

        private val userPermission =
            UserPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "user-permission-id",
                user =
                    UserWithAvatar(
                        userId = USER_ID,
                        firstName = "first",
                        lastName = "last",
                        userName = "user@passbolt.com",
                        isDisabled = false,
                        avatarUrl = null,
                    ),
            )

        private val groupPermission =
            GroupPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "group-permission-id",
                group = GroupModel(groupId = GROUP_ID, groupName = "group"),
            )

        private val PERMISSIONS_CONFIRMATION_FLOW: List<NavKey> =
            listOf(
                ConfirmPermissions(Edit(RESOURCE_ID)),
                ConfirmPermissions(Create(FOLDER_ID)),
                UserPermissionDetails(permission = userPermission, mode = PermissionsMode.EDIT, fromSnapshot = true),
                GroupPermissionDetails(permission = groupPermission, mode = PermissionsMode.EDIT, fromSnapshot = true),
                PermissionRecipients(userPermissions = listOf(userPermission), groupPermissions = listOf(groupPermission)),
                GroupMembers(groupId = GROUP_ID, fromSnapshot = true),
                GroupMemberDetails(userId = USER_ID, fromSnapshot = true),
            )
    }
}
