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
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ConfirmedPermissionsResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.UpgradeResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToConfirmPermissions
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowSnackbar
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceFormUpgradePermissionsConfirmationTest : ResourceFormPermissionsConfirmationTestSetup() {
    @Test
    fun `upgrade of a shared resource should navigate to permissions confirmation`() =
        runTest {
            stubEditMode()
            stubResourcePermissions(listOf(operatorOwnerPermissionModel(), otherUserPermissionModel()))
            mockResourceUpdateActionsInteractor.stub {
                on { doesUpgradeToV5ReEncryptSecret() }.thenReturn(true)
            }
            val viewModel = editModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpgradeResource)

                assertThat(awaitItem()).isEqualTo(
                    NavigateToConfirmPermissions(ConfirmPermissionsMode.Edit(RESOURCE_ID)),
                )
            }
            verify(mockResourceUpdateActionsInteractor, never()).upgradeToV5()
        }

    @Test
    fun `upgrade of a private resource should upgrade directly`() =
        runTest {
            stubEditMode()
            stubResourcePermissions(listOf(operatorOwnerPermissionModel()))
            mockResourceUpdateActionsInteractor.stub {
                on { doesUpgradeToV5ReEncryptSecret() }.thenReturn(true)
                onBlocking { upgradeToV5() }
                    .thenReturn(flowOf(ResourceUpdateActionResult.Success(RESOURCE_ID, "name")))
            }
            val viewModel = editModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpgradeResource)

                assertThat(awaitItem()).isEqualTo(ShowSnackbar(SnackbarMessage.RESOURCE_UPGRADED))
            }
            verify(mockResourceUpdateActionsInteractor).upgradeToV5()
        }

    @Test
    fun `upgrade that does not re-encrypt the secret should skip the confirmation even for a shared resource`() =
        runTest {
            stubEditMode()
            mockResourceUpdateActionsInteractor.stub {
                on { doesUpgradeToV5ReEncryptSecret() }.thenReturn(false)
                onBlocking { upgradeToV5() }
                    .thenReturn(flowOf(ResourceUpdateActionResult.Success(RESOURCE_ID, "name")))
            }
            val viewModel = editModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpgradeResource)

                assertThat(awaitItem()).isEqualTo(ShowSnackbar(SnackbarMessage.RESOURCE_UPGRADED))
            }
            verify(mockResourceUpdateActionsInteractor).upgradeToV5()
            verifyNoInteractions(mockFetchResourcePermissionsUseCase)
        }

    @Test
    fun `confirmed permissions after upgrade confirmation should upgrade with the confirmed list`() =
        runTest {
            val confirmedPermissions = listOf(operatorOwnerPermission(), otherUserPermission())
            stubEditMode()
            stubResourcePermissions(listOf(operatorOwnerPermissionModel(), otherUserPermissionModel()))
            mockResourceUpdateActionsInteractor.stub {
                on { doesUpgradeToV5ReEncryptSecret() }.thenReturn(true)
                onBlocking { upgradeToV5WithConfirmedPermissions(any()) }
                    .thenReturn(flowOf(ResourceUpdateActionResult.Success(RESOURCE_ID, "name")))
            }
            val viewModel = editModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpgradeResource)
                awaitItem()

                viewModel.onIntent(ConfirmedPermissionsResult(confirmedPermissions))

                assertThat(awaitItem()).isEqualTo(ShowSnackbar(SnackbarMessage.RESOURCE_UPGRADED))
            }
            verify(mockResourceUpdateActionsInteractor).upgradeToV5WithConfirmedPermissions(eq(confirmedPermissions))
            verify(mockResourceUpdateActionsInteractor, never())
                .updateGenericResourceWithConfirmedPermissions(any<ContentType>(), any(), any(), any())
        }

    @Test
    fun `permissions drift on confirmed upgrade should reopen the confirmation with fresh data`() =
        runTest {
            stubEditMode()
            stubResourcePermissions(listOf(operatorOwnerPermissionModel(), otherUserPermissionModel()))
            mockResourceUpdateActionsInteractor.stub {
                on { doesUpgradeToV5ReEncryptSecret() }.thenReturn(true)
                onBlocking { upgradeToV5WithConfirmedPermissions(any()) }
                    .thenReturn(flowOf(ResourceUpdateActionResult.PermissionsDrifted(listOf("drifted-user"))))
            }
            val viewModel = editModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpgradeResource)
                awaitItem()

                viewModel.onIntent(ConfirmedPermissionsResult(listOf(operatorOwnerPermission())))

                assertThat(awaitItem()).isEqualTo(
                    NavigateToConfirmPermissions(
                        ConfirmPermissionsMode.Edit(RESOURCE_ID),
                        driftedEntityNames = listOf("drifted-user"),
                    ),
                )
            }
        }
}
