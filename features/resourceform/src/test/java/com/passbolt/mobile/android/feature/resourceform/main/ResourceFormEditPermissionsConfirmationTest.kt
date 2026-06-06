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

package net.svaroh.passly.feature.resourceform.main

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.resources.actions.ResourceUpdateActionResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.ConfirmedPermissionsResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.NoteChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.UpdateResource
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithEditSuccess
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToConfirmPermissions
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.ShowSnackbar
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.ui.ConfirmPermissionsMode
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceFormEditPermissionsConfirmationTest : ResourceFormPermissionsConfirmationTestSetup() {
    @Test
    fun `edit of a shared resource should navigate to permissions confirmation`() =
        runTest {
            stubEditMode()
            stubResourcePermissions(listOf(operatorOwnerPermissionModel(), otherUserPermissionModel()))
            val viewModel = editModeViewModel()
            viewModel.onIntent(NoteChanged("changed-note"))

            viewModel.sideEffect.test {
                viewModel.onIntent(UpdateResource)

                assertThat(awaitItem()).isEqualTo(
                    NavigateToConfirmPermissions(ConfirmPermissionsMode.Edit(RESOURCE_ID)),
                )
            }
            verify(mockResourceUpdateActionsInteractor, never()).updateGenericResource(any(), any(), any(), any(), any())
        }

    @Test
    fun `edit of a private resource should update directly`() =
        runTest {
            stubEditMode()
            stubResourcePermissions(listOf(operatorOwnerPermissionModel()))
            stubUpdateSuccess()
            val viewModel = editModeViewModel()
            viewModel.onIntent(NoteChanged("changed-note"))

            viewModel.sideEffect.test {
                viewModel.onIntent(UpdateResource)

                assertIs<NavigateBackWithEditSuccess>(awaitItem())
            }
            verify(mockResourceUpdateActionsInteractor).updateGenericResource(any(), any(), any(), any(), any())
        }

    @Test
    fun `edit of a shared resource without secret change should update directly`() =
        runTest {
            stubEditMode()
            stubUpdateSuccess()
            val viewModel = editModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpdateResource)

                assertIs<NavigateBackWithEditSuccess>(awaitItem())
            }
            verify(mockResourceUpdateActionsInteractor).updateGenericResource(any(), any(), any(), any(), any())
            verifyNoInteractions(mockFetchResourcePermissionsUseCase)
        }

    @Test
    fun `resource permissions fetch failure should fall back to local permissions for the confirmation decision`() =
        runTest {
            stubEditMode()
            stubResourcePermissionsFetchFailure()
            stubLocalResourcePermissions(listOf(operatorOwnerPermission(), otherUserPermission()))
            val viewModel = editModeViewModel()
            viewModel.onIntent(NoteChanged("changed-note"))

            viewModel.sideEffect.test {
                viewModel.onIntent(UpdateResource)

                assertThat(awaitItem()).isEqualTo(
                    NavigateToConfirmPermissions(ConfirmPermissionsMode.Edit(RESOURCE_ID)),
                )
            }
        }

    @Test
    fun `confirmed permissions in edit mode should update with the confirmed list`() =
        runTest {
            val confirmedPermissions = listOf(operatorOwnerPermission(), otherUserPermission())
            stubEditMode()
            mockResourceUpdateActionsInteractor.stub {
                on {
                    updateGenericResourceWithConfirmedPermissions(any<ContentType>(), any(), any(), any())
                }.thenReturn(flowOf(ResourceUpdateActionResult.Success(RESOURCE_ID, "name")))
            }
            val viewModel = editModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(confirmedPermissions))

                assertIs<NavigateBackWithEditSuccess>(awaitItem())
            }
            verify(mockResourceUpdateActionsInteractor)
                .updateGenericResourceWithConfirmedPermissions(any<ContentType>(), eq(confirmedPermissions), any(), any())
        }

    @Test
    fun `permissions drift on confirmed edit should reopen the confirmation with fresh data`() =
        runTest {
            stubEditMode()
            mockResourceUpdateActionsInteractor.stub {
                on {
                    updateGenericResourceWithConfirmedPermissions(any<ContentType>(), any(), any(), any())
                }.thenReturn(flowOf(ResourceUpdateActionResult.PermissionsDrifted(listOf("drifted-user"))))
            }
            val viewModel = editModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(listOf(operatorOwnerPermission())))

                assertThat(awaitItem()).isEqualTo(
                    NavigateToConfirmPermissions(
                        ConfirmPermissionsMode.Edit(RESOURCE_ID),
                        driftedEntityNames = listOf("drifted-user"),
                    ),
                )
            }
        }

    @Test
    fun `share failure on confirmed edit should show an error and stay on the form`() =
        runTest {
            stubEditMode()
            mockResourceUpdateActionsInteractor.stub {
                on {
                    updateGenericResourceWithConfirmedPermissions(any<ContentType>(), any(), any(), any())
                }.thenReturn(flowOf(ResourceUpdateActionResult.ShareFailure("error")))
            }
            val viewModel = editModeViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(listOf(operatorOwnerPermission())))

                assertThat(awaitItem()).isEqualTo(ShowSnackbar(SnackbarMessage.RESOURCE_EDITED_SHARE_FAILED))
            }
        }
}
