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
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.domain.folders.usecase.FetchFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.GetMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsConfirmationOptOutUseCase
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.domain.resources.usecase.FetchResourcePermissionsUseCase
import com.passbolt.mobile.android.domain.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.domain.resources.usecase.GetEditContentTypeUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.domain.secrets.model.SecretJsonModel
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ConfirmedPermissionsResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.CreateResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.UpdateResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithCreateSuccess
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithEditSuccess
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToConfirmPermissions
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowSnackbar
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowToast
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourceUiModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceFormPermissionsConfirmationTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testResourceFormModule)
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        reset(
            mockGetFeatureFlagsUseCase,
            mockResourceCreateActionsInteractor,
            mockResourceUpdateActionsInteractor,
            mockResourceUpdateActionsInteractorFactory,
            mockGetLocalFolderPermissionsUseCase,
            mockFetchFolderPermissionsUseCase,
            mockFetchResourcePermissionsUseCase,
            mockGetLocalResourcePermissionsUseCase,
            mockGetPermissionsConfirmationOptOutUseCase,
            mockGetSelectedAccountDataUseCase,
            mockEntropyCalculator,
        )
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(GetFeatureFlagsUseCase.Output(DEFAULT_TEST_FEATURE_FLAGS))
        }
        mockGetPermissionsConfirmationOptOutUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(GetPermissionsConfirmationOptOutUseCase.Output(isOptedOut = false))
        }
        mockGetSelectedAccountDataUseCase.stub {
            on { execute(Unit) }.thenReturn(selectedAccountData())
        }
        mockEntropyCalculator.stub {
            onBlocking { getSecretEntropy(any()) }.thenReturn(0.0)
        }
        mockGetDefaultCreateContentTypeUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                    metadataType = MetadataTypeModel.V4,
                    contentType = ContentType.PasswordAndDescription,
                ),
            )
        }

        val passphraseMemoryCache: PassphraseMemoryCache = get()
        whenever(passphraseMemoryCache.getSessionDurationSeconds()) doReturn 5 * 60

        val getSessionExpiryUseCase: GetSessionExpiryUseCase = get()
        whenever(getSessionExpiryUseCase.execute(Unit)) doReturn
            GetSessionExpiryUseCase.Output.JwtWillExpire(ZonedDateTime.now().plusMinutes(5))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            mockGetFeatureFlagsUseCase,
            mockResourceCreateActionsInteractor,
            mockResourceUpdateActionsInteractor,
            mockResourceUpdateActionsInteractorFactory,
            mockGetLocalFolderPermissionsUseCase,
            mockFetchFolderPermissionsUseCase,
            mockFetchResourcePermissionsUseCase,
            mockGetLocalResourcePermissionsUseCase,
            mockGetPermissionsConfirmationOptOutUseCase,
            mockGetSelectedAccountDataUseCase,
            mockEntropyCalculator,
        )
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(GetFeatureFlagsUseCase.Output(DEFAULT_TEST_FEATURE_FLAGS))
        }
        mockGetPermissionsConfirmationOptOutUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(GetPermissionsConfirmationOptOutUseCase.Output(isOptedOut = false))
        }
    }

    @Test
    fun `create in a shared folder should navigate to permissions confirmation`() =
        runTest {
            stubFolderPermissions(listOf(operatorOwnerPermissionModel(), otherUserPermissionModel()))
            val viewModel = createModeViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

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
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

                assertThat(awaitItem()).isEqualTo(NavigateToConfirmPermissions(ConfirmPermissionsMode.Create(FOLDER_ID)))
            }
        }

    @Test
    fun `create in a private folder should create directly`() =
        runTest {
            stubFolderPermissions(listOf(operatorOwnerPermissionModel()))
            stubCreateSuccess()
            val viewModel = createModeViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

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
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

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
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateResource)
                advanceUntilIdle()

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
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(confirmedPermissions))
                advanceUntilIdle()

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
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(listOf(operatorOwnerPermission())))
                advanceUntilIdle()

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
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(listOf(operatorOwnerPermission())))
                advanceUntilIdle()

                assertThat(awaitItem()).isEqualTo(ShowToast(ToastMessage.RESOURCE_CREATED_SHARE_FAILED))
                assertIs<NavigateBackWithCreateSuccess>(awaitItem())
            }
        }

    @Test
    fun `edit of a shared resource should navigate to permissions confirmation`() =
        runTest {
            stubEditMode()
            stubResourcePermissions(listOf(operatorOwnerPermissionModel(), otherUserPermissionModel()))
            val viewModel = editModeViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpdateResource)
                advanceUntilIdle()

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
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpdateResource)
                advanceUntilIdle()

                assertIs<NavigateBackWithEditSuccess>(awaitItem())
            }
            verify(mockResourceUpdateActionsInteractor).updateGenericResource(any(), any(), any(), any(), any())
        }

    @Test
    fun `edit of a shared resource with session opt out should update directly`() =
        runTest {
            stubEditMode()
            stubResourcePermissions(listOf(operatorOwnerPermissionModel(), otherUserPermissionModel()))
            mockGetPermissionsConfirmationOptOutUseCase.stub {
                onBlocking { execute(Unit) }.thenReturn(GetPermissionsConfirmationOptOutUseCase.Output(isOptedOut = true))
            }
            stubUpdateSuccess()
            val viewModel = editModeViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpdateResource)
                advanceUntilIdle()

                assertIs<NavigateBackWithEditSuccess>(awaitItem())
            }
        }

    @Test
    fun `resource permissions fetch failure should fall back to local permissions for the confirmation decision`() =
        runTest {
            stubEditMode()
            stubResourcePermissionsFetchFailure()
            stubLocalResourcePermissions(listOf(operatorOwnerPermission(), otherUserPermission()))
            val viewModel = editModeViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(UpdateResource)
                advanceUntilIdle()

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
                onBlocking {
                    updateGenericResourceWithConfirmedPermissions(any(), any(), any(), any())
                }.thenReturn(flowOf(ResourceUpdateActionResult.Success(RESOURCE_ID, "name")))
            }
            val viewModel = editModeViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(confirmedPermissions))
                advanceUntilIdle()

                assertIs<NavigateBackWithEditSuccess>(awaitItem())
            }
            verify(mockResourceUpdateActionsInteractor)
                .updateGenericResourceWithConfirmedPermissions(any(), eq(confirmedPermissions), any(), any())
        }

    @Test
    fun `permissions drift on confirmed edit should reopen the confirmation with fresh data`() =
        runTest {
            stubEditMode()
            mockResourceUpdateActionsInteractor.stub {
                onBlocking {
                    updateGenericResourceWithConfirmedPermissions(any(), any(), any(), any())
                }.thenReturn(flowOf(ResourceUpdateActionResult.PermissionsDrifted))
            }
            val viewModel = editModeViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(listOf(operatorOwnerPermission())))
                advanceUntilIdle()

                assertThat(awaitItem()).isEqualTo(
                    NavigateToConfirmPermissions(ConfirmPermissionsMode.Edit(RESOURCE_ID), driftDetected = true),
                )
            }
        }

    @Test
    fun `share failure on confirmed edit should show an error and stay on the form`() =
        runTest {
            stubEditMode()
            mockResourceUpdateActionsInteractor.stub {
                onBlocking {
                    updateGenericResourceWithConfirmedPermissions(any(), any(), any(), any())
                }.thenReturn(flowOf(ResourceUpdateActionResult.ShareFailure("error")))
            }
            val viewModel = editModeViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmedPermissionsResult(listOf(operatorOwnerPermission())))
                advanceUntilIdle()

                assertThat(awaitItem()).isEqualTo(ShowSnackbar(SnackbarMessage.RESOURCE_EDITED_SHARE_FAILED))
            }
        }

    private fun createModeViewModel(): ResourceFormViewModel {
        val mode =
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = FOLDER_ID,
            )
        return get<ResourceFormViewModel> { parametersOf(mode) }
    }

    private fun editModeViewModel(): ResourceFormViewModel {
        val mode = ResourceFormMode.Edit(resourceId = RESOURCE_ID, resourceName = "Test")
        return get<ResourceFormViewModel> { parametersOf(mode) }
    }

    private fun stubEditMode() {
        mockGetMetadataTypesSettingsUseCase.stub {
            onBlocking { execute(Unit) }.thenReturn(
                GetMetadataTypesSettingsUseCase.Output(DEFAULT_METADATA_TYPES_SETTINGS),
            )
        }
        mockGetLocalResourceUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(GetLocalResourceUseCase.Output(editedResourceModel()))
        }
        mockGetEditContentTypeUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                GetEditContentTypeUseCase.Output(
                    contentType = ContentType.PasswordAndDescription,
                    metadataType = MetadataTypeModel.V4,
                ),
            )
        }
        val secretInteractorMock = mock<SecretPropertiesActionsInteractor>()
        secretInteractorMock.stub {
            onBlocking { provideDecryptedSecret() }.thenReturn(
                flowOf(
                    SecretPropertyActionResult.Success(
                        label = "secret",
                        isSecret = true,
                        result = SecretJsonModel("""{"password": ""}"""),
                    ),
                ),
            )
        }
        mockSecretPropertiesActionsInteractorSecretPropertiesActionsInteractorFactory.stub {
            on { create(any()) }.thenReturn(secretInteractorMock)
        }
        mockResourceUpdateActionsInteractorFactory.stub {
            on { create(any()) }.thenReturn(mockResourceUpdateActionsInteractor)
        }
    }

    private fun stubUpdateSuccess() {
        mockResourceUpdateActionsInteractor.stub {
            onBlocking { updateGenericResource(any(), any(), any(), any(), any()) }
                .thenReturn(flowOf(ResourceUpdateActionResult.Success(RESOURCE_ID, "name")))
        }
    }

    private fun stubResourcePermissions(permissions: List<PermissionModel>) {
        mockFetchResourcePermissionsUseCase.stub {
            onBlocking { execute(FetchResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .thenReturn(FetchResourcePermissionsUseCase.Output.Success(permissions))
        }
    }

    private fun stubResourcePermissionsFetchFailure() {
        mockFetchResourcePermissionsUseCase.stub {
            onBlocking { execute(FetchResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .thenReturn(
                    FetchResourcePermissionsUseCase.Output.Failure(
                        DomainResult.Incomplete.Error(DomainResult.Incomplete.Error.Reason.OFFLINE, "offline"),
                    ),
                )
        }
    }

    private fun stubLocalResourcePermissions(permissions: List<PermissionModelUi>) {
        mockGetLocalResourcePermissionsUseCase.stub {
            onBlocking { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .thenReturn(GetLocalResourcePermissionsUseCase.Output(permissions))
        }
    }

    private fun editedResourceModel() =
        ResourceUiModel(
            resourceId = RESOURCE_ID,
            resourceTypeId = "resourceTypeId",
            slug = ContentType.PasswordAndDescription.slug,
            folderId = null,
            permission = ResourcePermission.OWNER,
            favouriteId = null,
            modified = ZonedDateTime.now(),
            expiry = null,
            metadataKeyId = null,
            metadataKeyType = MetadataKeyTypeModel.PERSONAL,
            metadataJsonModel = MetadataJsonModel("""{"name": "Test"}"""),
        )

    private fun stubFolderPermissions(permissions: List<PermissionModel>) {
        mockFetchFolderPermissionsUseCase.stub {
            onBlocking { execute(FetchFolderPermissionsUseCase.Input(FOLDER_ID)) }
                .thenReturn(FetchFolderPermissionsUseCase.Output.Success(permissions))
        }
    }

    private fun stubFolderPermissionsFetchFailure() {
        mockFetchFolderPermissionsUseCase.stub {
            onBlocking { execute(FetchFolderPermissionsUseCase.Input(FOLDER_ID)) }
                .thenReturn(
                    FetchFolderPermissionsUseCase.Output.Failure(
                        DomainResult.Incomplete.Error(DomainResult.Incomplete.Error.Reason.OFFLINE, "offline"),
                    ),
                )
        }
    }

    private fun stubLocalFolderPermissions(permissions: List<PermissionModelUi>) {
        mockGetLocalFolderPermissionsUseCase.stub {
            onBlocking { execute(GetLocalFolderPermissionsUseCase.Input(FOLDER_ID)) }
                .thenReturn(GetLocalFolderPermissionsUseCase.Output(permissions))
        }
    }

    private fun stubCreateSuccess() {
        mockResourceCreateActionsInteractor.stub {
            onBlocking { createGenericResource(any(), anyOrNull(), any(), any()) }
                .thenReturn(flowOf(ResourceCreateActionResult.Success("id", "name")))
        }
    }

    private fun operatorOwnerPermission() =
        PermissionModelUi.UserPermissionModel(
            permission = ResourcePermission.OWNER,
            permissionId = "perm-operator",
            user = userWithAvatar(OPERATOR_SERVER_ID),
        )

    private fun otherUserPermission() =
        PermissionModelUi.UserPermissionModel(
            permission = ResourcePermission.READ,
            permissionId = "perm-user",
            user = userWithAvatar("other-user-id"),
        )

    private fun operatorOwnerPermissionModel() =
        PermissionModel.UserPermissionModel(
            permission = ResourcePermission.OWNER,
            permissionId = "perm-operator",
            userId = OPERATOR_SERVER_ID,
        )

    private fun otherUserPermissionModel() =
        PermissionModel.UserPermissionModel(
            permission = ResourcePermission.READ,
            permissionId = "perm-user",
            userId = "other-user-id",
        )

    private fun groupPermissionModel() =
        PermissionModel.GroupPermissionModel(
            permission = ResourcePermission.UPDATE,
            permissionId = "perm-group",
            group = GroupModel("group-id", "group-name"),
        )

    private fun userWithAvatar(userId: String) =
        UserWithAvatar(
            userId = userId,
            firstName = "first",
            lastName = "last",
            userName = "user@passbolt.com",
            isDisabled = false,
            avatarUrl = null,
        )

    private fun selectedAccountData() =
        GetSelectedAccountDataUseCase.Output(
            firstName = "first",
            lastName = "last",
            email = "user@passbolt.com",
            avatarUrl = null,
            url = "https://passbolt.com",
            serverId = OPERATOR_SERVER_ID,
            label = "label",
            role = "user",
        )

    private companion object {
        private const val FOLDER_ID = "folder-id"
        private const val RESOURCE_ID = "resource-id"
        private const val OPERATOR_SERVER_ID = "operator-server-id"

        private val mockResourceUpdateActionsInteractor = mock<ResourceUpdateActionsInteractor>()
    }
}
