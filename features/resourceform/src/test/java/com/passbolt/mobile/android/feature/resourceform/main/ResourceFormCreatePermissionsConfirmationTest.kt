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

package com.passbolt.mobile.android.feature.resourceform.main

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsConfirmationOptOutUseCase
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ConfirmedPermissionsResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.CreateResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithCreateSuccess
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToConfirmPermissions
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowToast
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceFormCreatePermissionsConfirmationTest : ResourceFormPermissionsConfirmationTestSetup() {
    @Test
    fun `create in a shared folder should navigate to permissions confirmation`() =
        runTest {
            stubFolderPermissions(listOf(operatorOwnerPermissionModel(), otherUserPermissionModel()))
            val viewModel = createModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)

                assertThat(awaitItem()).isEqualTo(NavigateToConfirmPermissions(ConfirmPermissionsMode.Create(FOLDER_ID)))
            }
            verify(mockResourceCreateActionsInteractor, never())
                .createGenericResource(any(), anyOrNull(), any(), any())
        }

    @Test
    fun `create in a group shared folder should navigate to permissions confirmation`() =
        runTest {
            stubFolderPermissions(listOf(groupPermissionModel()))
            val viewModel = createModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)

                assertThat(awaitItem()).isEqualTo(NavigateToConfirmPermissions(ConfirmPermissionsMode.Create(FOLDER_ID)))
            }
        }

    @Test
    fun `create in a private folder should create directly`() =
        runTest {
            stubFolderPermissions(listOf(operatorOwnerPermissionModel()))
            stubCreateSuccess()
            val viewModel = createModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)

                assertIs<NavigateBackWithCreateSuccess>(awaitItem())
            }
            verify(mockResourceCreateActionsInteractor).createGenericResource(any(), anyOrNull(), any(), any())
        }

    @Test
    fun `create in a shared folder with session opt out should create directly`() =
        runTest {
            stubFolderPermissions(listOf(operatorOwnerPermissionModel(), otherUserPermissionModel()))
            mockGetPermissionsConfirmationOptOutUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(GetPermissionsConfirmationOptOutUseCase.Output(isOptedOut = true))
            }
            stubCreateSuccess()
            val viewModel = createModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)

                assertIs<NavigateBackWithCreateSuccess>(awaitItem())
            }
            verify(mockResourceCreateActionsInteractor).createGenericResource(any(), anyOrNull(), any(), any())
        }

    @Test
    fun `permissions fetch failure should fall back to local permissions for the confirmation decision`() =
        runTest {
            stubFolderPermissionsFetchFailure()
            stubLocalFolderPermissions(listOf(operatorOwnerPermission(), otherUserPermission()))
            val viewModel = createModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)

                assertThat(awaitItem()).isEqualTo(NavigateToConfirmPermissions(ConfirmPermissionsMode.Create(FOLDER_ID)))
            }
        }

    @Test
    fun `confirmed permissions should create with the confirmed list`() =
        runTest {
            val confirmedPermissions = listOf(operatorOwnerPermission(), otherUserPermission())
            mockResourceCreateActionsInteractor.stub {
                onBlocking {
                    createGenericResourceWithConfirmedPermissions(any(), anyOrNull(), any(), any(), any())
                }.thenReturn(flowOf(ResourceCreateActionResult.Success("id", "name")))
            }
            val viewModel = createModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(confirmedPermissions))

                assertIs<NavigateBackWithCreateSuccess>(awaitItem())
            }
            verify(mockResourceCreateActionsInteractor)
                .createGenericResourceWithConfirmedPermissions(any(), anyOrNull(), any(), any(), eq(confirmedPermissions))
        }

    @Test
    fun `permissions drift after create should inform and navigate back`() =
        runTest {
            mockResourceCreateActionsInteractor.stub {
                onBlocking {
                    createGenericResourceWithConfirmedPermissions(any(), anyOrNull(), any(), any(), any())
                }.thenReturn(flowOf(ResourceCreateActionResult.PermissionsDrifted))
            }
            val viewModel = createModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(listOf(operatorOwnerPermission())))

                assertThat(awaitItem()).isEqualTo(ShowToast(ToastMessage.RESOURCE_CREATED_PERMISSIONS_CHANGED))
                assertIs<NavigateBackWithCreateSuccess>(awaitItem())
            }
        }

    @Test
    fun `share failure after create should inform and navigate back`() =
        runTest {
            mockResourceCreateActionsInteractor.stub {
                onBlocking {
                    createGenericResourceWithConfirmedPermissions(any(), anyOrNull(), any(), any(), any())
                }.thenReturn(flowOf(ResourceCreateActionResult.ShareFailure("error")))
            }
            val viewModel = createModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(listOf(operatorOwnerPermission())))

                assertThat(awaitItem()).isEqualTo(ShowToast(ToastMessage.RESOURCE_CREATED_SHARE_FAILED))
                assertIs<NavigateBackWithCreateSuccess>(awaitItem())
            }
        }
}
