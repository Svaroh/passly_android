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

package com.passbolt.mobile.android.domain.resources.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.session.validSessionTestModule
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.domain.folders.usecase.FetchFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsConfirmationOptOutUseCase
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoInteractions

@ExperimentalCoroutinesApi
class CreatePermissionsConfirmationInteractorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetPermissionsConfirmationOptOutUseCase>() }
                    single { mock<FetchFolderPermissionsUseCase>() }
                    single { mock<GetLocalFolderPermissionsUseCase>() }
                    single { mock<GetSelectedAccountDataUseCase>() }
                    singleOf(::CreatePermissionsConfirmationInteractor)
                },
                validSessionTestModule,
            )
        }

    @Before
    fun setUp() {
        get<GetPermissionsConfirmationOptOutUseCase>().stub {
            on { execute(Unit) } doReturn GetPermissionsConfirmationOptOutUseCase.Output(isOptedOut = false)
        }
        get<GetSelectedAccountDataUseCase>().stub {
            on { execute(Unit) } doReturn selectedAccountData()
        }
    }

    @Test
    fun `no parent folder does not require the confirmation`() =
        runTest {
            assertThat(get<CreatePermissionsConfirmationInteractor>().shouldConfirmPermissions(null)).isFalse()
            verifyNoInteractions(get<FetchFolderPermissionsUseCase>())
        }

    @Test
    fun `session opt out skips the confirmation without fetching permissions`() =
        runTest {
            get<GetPermissionsConfirmationOptOutUseCase>().stub {
                on { execute(Unit) } doReturn GetPermissionsConfirmationOptOutUseCase.Output(isOptedOut = true)
            }

            assertThat(get<CreatePermissionsConfirmationInteractor>().shouldConfirmPermissions(FOLDER_ID)).isFalse()
            verifyNoInteractions(get<FetchFolderPermissionsUseCase>())
        }

    @Test
    fun `shared parent folder requires the confirmation`() =
        runTest {
            stubFetchedPermissions(operatorOwnerPermission(), otherUserPermission())

            assertThat(get<CreatePermissionsConfirmationInteractor>().shouldConfirmPermissions(FOLDER_ID)).isTrue()
        }

    @Test
    fun `private parent folder does not require the confirmation`() =
        runTest {
            stubFetchedPermissions(operatorOwnerPermission())

            assertThat(get<CreatePermissionsConfirmationInteractor>().shouldConfirmPermissions(FOLDER_ID)).isFalse()
        }

    @Test
    fun `group permission on the parent folder requires the confirmation`() =
        runTest {
            stubFetchedPermissions(groupPermission())

            assertThat(get<CreatePermissionsConfirmationInteractor>().shouldConfirmPermissions(FOLDER_ID)).isTrue()
        }

    @Test
    fun `permissions fetch failure falls back to the local permissions`() =
        runTest {
            get<FetchFolderPermissionsUseCase>().stub {
                on { execute(any()) } doReturn
                    FetchFolderPermissionsUseCase.Output.Failure(DomainResult.Incomplete.Error(UNKNOWN, "error"))
            }
            get<GetLocalFolderPermissionsUseCase>().stub {
                on { execute(GetLocalFolderPermissionsUseCase.Input(FOLDER_ID)) } doReturn
                    GetLocalFolderPermissionsUseCase.Output(
                        listOf(localOperatorOwnerPermission(), localOtherUserPermission()),
                    )
            }

            assertThat(get<CreatePermissionsConfirmationInteractor>().shouldConfirmPermissions(FOLDER_ID)).isTrue()
        }

    private fun stubFetchedPermissions(vararg permissions: PermissionModel) {
        get<FetchFolderPermissionsUseCase>().stub {
            on { execute(FetchFolderPermissionsUseCase.Input(FOLDER_ID)) } doReturn
                FetchFolderPermissionsUseCase.Output.Success(permissions.toList())
        }
    }

    private companion object {
        private const val FOLDER_ID = "folder-id"
        private const val OPERATOR_SERVER_ID = "operator-server-id"
        private const val USER_ID = "user-id"

        private fun operatorOwnerPermission() =
            PermissionModel.UserPermissionModel(ResourcePermission.OWNER, "perm-operator", OPERATOR_SERVER_ID)

        private fun otherUserPermission() = PermissionModel.UserPermissionModel(ResourcePermission.READ, "perm-user", USER_ID)

        private fun groupPermission() =
            PermissionModel.GroupPermissionModel(ResourcePermission.READ, "perm-group", GroupModel("group-id", "group"))

        private fun localOperatorOwnerPermission() =
            PermissionModelUi.UserPermissionModel(
                permission = ResourcePermission.OWNER,
                permissionId = "perm-operator",
                user = userWithAvatar(OPERATOR_SERVER_ID),
            )

        private fun localOtherUserPermission() =
            PermissionModelUi.UserPermissionModel(
                permission = ResourcePermission.READ,
                permissionId = "perm-user",
                user = userWithAvatar(USER_ID),
            )

        private fun userWithAvatar(userId: String) =
            UserWithAvatar(
                userId = userId,
                firstName = "first-$userId",
                lastName = "last-$userId",
                userName = "$userId@passbolt.com",
                isDisabled = false,
                avatarUrl = null,
            )

        private fun selectedAccountData() =
            GetSelectedAccountDataUseCase.Output(
                firstName = "first",
                lastName = "last",
                email = "email@passbolt.com",
                avatarUrl = null,
                url = "https://passbolt.com",
                serverId = OPERATOR_SERVER_ID,
                label = "label",
                role = "user",
            )
    }
}
