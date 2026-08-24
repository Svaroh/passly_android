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
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ResourceFormPermissionsConfirmationTestSetup : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testResourceFormModule)
        }

    protected val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        resetAndStubMocks()
        mockGetSelectedAccountDataUseCase.stub {
            on { execute(Unit) }.thenReturn(selectedAccountData())
        }
        mockEntropyCalculator.stub {
            on { getSecretEntropy(any()) }.thenReturn(0.0)
        }
        mockGetDefaultCreateContentTypeUseCase.stub {
            on { execute(any()) }.thenReturn(
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
        resetAndStubMocks()
    }

    private fun resetAndStubMocks() {
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
            on { execute(Unit) }.thenReturn(GetFeatureFlagsUseCase.Output(DEFAULT_TEST_FEATURE_FLAGS))
        }
        mockGetPermissionsConfirmationOptOutUseCase.stub {
            on { execute(Unit) }.thenReturn(GetPermissionsConfirmationOptOutUseCase.Output(isOptedOut = false))
        }
    }

    protected fun createModeViewModel(): ResourceFormViewModel {
        val mode =
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = FOLDER_ID,
            )
        return get<ResourceFormViewModel> { parametersOf(mode) }.awaitInitialized()
    }

    protected fun editModeViewModel(): ResourceFormViewModel {
        val mode = ResourceFormMode.Edit(resourceId = RESOURCE_ID, resourceName = "Test")
        return get<ResourceFormViewModel> { parametersOf(mode) }.awaitInitialized()
    }

    private fun ResourceFormViewModel.awaitInitialized(): ResourceFormViewModel {
        testDispatcher.scheduler.advanceUntilIdle()
        return this
    }

    protected fun stubEditMode() {
        mockGetMetadataTypesSettingsUseCase.stub {
            on { execute(Unit) }.thenReturn(
                GetMetadataTypesSettingsUseCase.Output(DEFAULT_METADATA_TYPES_SETTINGS),
            )
        }
        mockGetLocalResourceUseCase.stub {
            on { execute(any()) }.thenReturn(GetLocalResourceUseCase.Output(editedResourceModel()))
        }
        mockGetEditContentTypeUseCase.stub {
            on { execute(any()) }.thenReturn(
                GetEditContentTypeUseCase.Output(
                    contentType = ContentType.PasswordAndDescription,
                    metadataType = MetadataTypeModel.V4,
                ),
            )
        }
        val secretInteractorMock = mock<SecretPropertiesActionsInteractor>()
        secretInteractorMock.stub {
            on { provideDecryptedSecret() }.thenReturn(
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

    protected fun stubCreateSuccess() {
        mockResourceCreateActionsInteractor.stub {
            on { createGenericResource(any(), anyOrNull(), any(), any()) }
                .thenReturn(flowOf(ResourceCreateActionResult.Success("id", "name")))
        }
    }

    protected fun stubUpdateSuccess() {
        mockResourceUpdateActionsInteractor.stub {
            on { updateGenericResource(any(), any(), any(), any(), any()) }
                .thenReturn(flowOf(ResourceUpdateActionResult.Success(RESOURCE_ID, "name")))
        }
    }

    protected fun stubResourcePermissions(permissions: List<PermissionModel>) {
        mockFetchResourcePermissionsUseCase.stub {
            on { execute(FetchResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .thenReturn(FetchResourcePermissionsUseCase.Output.Success(permissions))
        }
    }

    protected fun stubResourcePermissionsFetchFailure() {
        mockFetchResourcePermissionsUseCase.stub {
            on { execute(FetchResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .thenReturn(
                    FetchResourcePermissionsUseCase.Output.Failure(
                        DomainResult.Incomplete.Error(DomainResult.Incomplete.Error.Reason.OFFLINE, "offline"),
                    ),
                )
        }
    }

    protected fun stubLocalResourcePermissions(permissions: List<PermissionModelUi>) {
        mockGetLocalResourcePermissionsUseCase.stub {
            on { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .thenReturn(GetLocalResourcePermissionsUseCase.Output(permissions))
        }
    }

    protected fun stubFolderPermissions(permissions: List<PermissionModel>) {
        mockFetchFolderPermissionsUseCase.stub {
            on { execute(FetchFolderPermissionsUseCase.Input(FOLDER_ID)) }
                .thenReturn(FetchFolderPermissionsUseCase.Output.Success(permissions))
        }
    }

    protected fun stubFolderPermissionsFetchFailure() {
        mockFetchFolderPermissionsUseCase.stub {
            on { execute(FetchFolderPermissionsUseCase.Input(FOLDER_ID)) }
                .thenReturn(
                    FetchFolderPermissionsUseCase.Output.Failure(
                        DomainResult.Incomplete.Error(DomainResult.Incomplete.Error.Reason.OFFLINE, "offline"),
                    ),
                )
        }
    }

    protected fun stubLocalFolderPermissions(permissions: List<PermissionModelUi>) {
        mockGetLocalFolderPermissionsUseCase.stub {
            on { execute(GetLocalFolderPermissionsUseCase.Input(FOLDER_ID)) }
                .thenReturn(GetLocalFolderPermissionsUseCase.Output(permissions))
        }
    }

    protected fun operatorOwnerPermission() =
        PermissionModelUi.UserPermissionModel(
            permission = ResourcePermission.OWNER,
            permissionId = "perm-operator",
            user = userWithAvatar(OPERATOR_SERVER_ID),
        )

    protected fun otherUserPermission() =
        PermissionModelUi.UserPermissionModel(
            permission = ResourcePermission.READ,
            permissionId = "perm-user",
            user = userWithAvatar("other-user-id"),
        )

    protected fun operatorOwnerPermissionModel() =
        PermissionModel.UserPermissionModel(
            permission = ResourcePermission.OWNER,
            permissionId = "perm-operator",
            userId = OPERATOR_SERVER_ID,
        )

    protected fun otherUserPermissionModel() =
        PermissionModel.UserPermissionModel(
            permission = ResourcePermission.READ,
            permissionId = "perm-user",
            userId = "other-user-id",
        )

    protected fun groupPermissionModel() =
        PermissionModel.GroupPermissionModel(
            permission = ResourcePermission.UPDATE,
            permissionId = "perm-group",
            group = GroupModel("group-id", "group-name"),
        )

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

    protected companion object {
        const val FOLDER_ID = "folder-id"
        const val RESOURCE_ID = "resource-id"
        const val OPERATOR_SERVER_ID = "operator-server-id"

        val mockResourceUpdateActionsInteractor = mock<ResourceUpdateActionsInteractor>()
    }
}
