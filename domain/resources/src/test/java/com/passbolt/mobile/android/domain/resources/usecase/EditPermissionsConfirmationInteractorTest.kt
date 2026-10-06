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

package net.svaroh.passly.domain.resources.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.commontest.session.validSessionTestModule
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.ui.GroupModel
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.UserWithAvatar
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

@ExperimentalCoroutinesApi
class EditPermissionsConfirmationInteractorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<FetchResourcePermissionsUseCase>() }
                    single { mock<GetLocalResourcePermissionsUseCase>() }
                    single { mock<GetSelectedAccountDataUseCase>() }
                    singleOf(::EditPermissionsConfirmationInteractor)
                },
                validSessionTestModule,
            )
        }

    @Before
    fun setUp() {
        get<GetSelectedAccountDataUseCase>().stub {
            on { execute(Unit) } doReturn selectedAccountData()
        }
    }

    @Test
    fun `shared resource requires the confirmation`() =
        runTest {
            stubFetchedPermissions(operatorOwnerPermission(), otherUserPermission())

            assertThat(get<EditPermissionsConfirmationInteractor>().shouldConfirmPermissions(RESOURCE_ID)).isTrue()
        }

    @Test
    fun `private resource does not require the confirmation`() =
        runTest {
            stubFetchedPermissions(operatorOwnerPermission())

            assertThat(get<EditPermissionsConfirmationInteractor>().shouldConfirmPermissions(RESOURCE_ID)).isFalse()
        }

    @Test
    fun `group permission requires the confirmation`() =
        runTest {
            stubFetchedPermissions(groupPermission())

            assertThat(get<EditPermissionsConfirmationInteractor>().shouldConfirmPermissions(RESOURCE_ID)).isTrue()
        }

    @Test
    fun `permissions fetch failure falls back to the local permissions`() =
        runTest {
            get<FetchResourcePermissionsUseCase>().stub {
                on { execute(any()) } doReturn
                    FetchResourcePermissionsUseCase.Output.Failure(DomainResult.Incomplete.Error(UNKNOWN, "error"))
            }
            get<GetLocalResourcePermissionsUseCase>().stub {
                on { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) } doReturn
                    GetLocalResourcePermissionsUseCase.Output(
                        listOf(localOperatorOwnerPermission(), localOtherUserPermission()),
                    )
            }

            assertThat(get<EditPermissionsConfirmationInteractor>().shouldConfirmPermissions(RESOURCE_ID)).isTrue()
        }

    private fun stubFetchedPermissions(vararg permissions: PermissionModel) {
        get<FetchResourcePermissionsUseCase>().stub {
            on { execute(FetchResourcePermissionsUseCase.Input(RESOURCE_ID)) } doReturn
                FetchResourcePermissionsUseCase.Output.Success(permissions.toList())
        }
    }

    private companion object {
        private const val RESOURCE_ID = "resource-id"
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
