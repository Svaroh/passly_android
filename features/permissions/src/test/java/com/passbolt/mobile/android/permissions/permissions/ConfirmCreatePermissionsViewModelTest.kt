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

package com.passbolt.mobile.android.permissions.permissions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.commontest.session.validSessionTestModule
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.ResourceAccessInteractor
import com.passbolt.mobile.android.domain.permissionsconfirmation.model.PermissionsSnapshot
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.CreatePermissionsSnapshotInteractor
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.SetPermissionsConfirmationOptOutUseCase
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionsInteractorFactory
import com.passbolt.mobile.android.domain.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.domain.users.model.GpgKey
import com.passbolt.mobile.android.domain.users.model.UserProfile
import com.passbolt.mobile.android.domain.users.usecase.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.MainButtonIntent
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.SeePermission
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.SkipConfirmationToggled
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.UserPermissionDeleted
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.UserPermissionModified
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.CloseWithPermissionsConfirmed
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToGroupPermissionDetails
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToUserPermissionDetails
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.ShowToast
import com.passbolt.mobile.android.ui.GpgKeyUiModel
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.UserProfileUiModel
import com.passbolt.mobile.android.ui.UserUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.ZonedDateTime
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmCreatePermissionsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetLocalResourcePermissionsUseCase>() }
                    single { mock<GetLocalFolderPermissionsUseCase>() }
                    single { mock<GetLocalResourceUseCase>() }
                    single { mock<GetLocalFolderDetailsUseCase>() }
                    single { mock<ResourceShareInteractor>() }
                    single { mock<MetadataPrivateKeysHelperInteractor>() }
                    single { mock<ResourceUpdateActionsInteractorFactory>() }
                    single { mock<ResourceAccessInteractor>() }
                    single { mock<CreatePermissionsSnapshotInteractor>() }
                    single { mock<GetLocalCurrentUserUseCase>() }
                    single { mock<GetFeatureFlagsUseCase>() }
                    single { mock<SetPermissionsConfirmationOptOutUseCase>() }
                    single { UsersModelMapper() }
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    singleOf(::SessionRefreshTrackingFlow)
                    singleOf(::DataRefreshTrackingFlow)
                    factory { PermissionModelUiComparator() }
                    factory { params ->
                        PermissionsViewModel(
                            permissionsItem = params.get(),
                            id = params.get(),
                            mode = params.get(),
                            flow = params.get(),
                            getLocalResourcePermissionsUseCase = get(),
                            getLocalResourceUseCase = get(),
                            getLocalFolderPermissionsUseCase = get(),
                            getLocalFolderUseCase = get(),
                            permissionModelUiComparator = get(),
                            resourceShareInteractor = get(),
                            metadataPrivateKeysHelperInteractor = get(),
                            resourceAccessInteractor = get(),
                            dataRefreshTrackingFlow = get(),
                            coroutineLaunchContext = get(),
                            resourceUpdateActionsInteractorFactory = get(),
                            createPermissionsSnapshotInteractor = get(),
                            getLocalCurrentUserUseCase = get(),
                            getFeatureFlagsUseCase = get(),
                            setPermissionsConfirmationOptOutUseCase = get(),
                            usersModelMapper = get(),
                        )
                    }
                },
                validSessionTestModule,
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        get<CreatePermissionsSnapshotInteractor>().stub {
            onBlocking { createForFolder(FOLDER_ID) }
                .doReturn(CreatePermissionsSnapshotInteractor.Output.Success(SNAPSHOT))
        }
        get<GetLocalCurrentUserUseCase>().stub {
            onBlocking { execute(Unit) } doReturn GetLocalCurrentUserUseCase.Output(CURRENT_USER_UI_MODEL)
        }
        get<GetFeatureFlagsUseCase>().stub {
            onBlocking { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(featureFlags(isOptOutAvailable = true))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `snapshot permissions are shown with operator as owner`() =
        runTest {
            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                val state = expectMostRecentItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.permissions).hasSize(3)
                val userPermissions = state.permissions.filterIsInstance<PermissionModelUi.UserPermissionModel>()
                assertThat(userPermissions.single { it.user.userId == OPERATOR_ID }.permission)
                    .isEqualTo(ResourcePermission.OWNER)
                assertThat(userPermissions.single { it.user.userId == USER_ID }.permission)
                    .isEqualTo(ResourcePermission.READ)
            }
        }

    @Test
    fun `add user button, save button and skip switch are shown`() =
        runTest {
            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                val state = expectMostRecentItem()
                assertThat(state.showAddUserButton).isTrue()
                assertThat(state.showSaveButton).isTrue()
                assertThat(state.showSkipConfirmationSwitch).isTrue()
            }
        }

    @Test
    fun `skip switch is hidden when opt out feature flag is off`() =
        runTest {
            get<GetFeatureFlagsUseCase>().stub {
                onBlocking { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(featureFlags(isOptOutAvailable = false))
            }

            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().showSkipConfirmationSwitch).isFalse()
            }
        }

    @Test
    fun `save publishes confirmed permissions and does not share`() =
        runTest {
            val viewModel = confirmCreateViewModel()
            viewModel.viewState.test { expectMostRecentItem() }

            viewModel.sideEffect.test {
                viewModel.onIntent(MainButtonIntent)

                val effect = assertIs<CloseWithPermissionsConfirmed>(awaitItem())
                assertThat(effect.permissions).hasSize(3)
            }
            verifyNoInteractions(get<ResourceShareInteractor>())
            verifyNoInteractions(get<SetPermissionsConfirmationOptOutUseCase>())
        }

    @Test
    fun `save stores session opt out when skip switch is checked`() =
        runTest {
            val viewModel = confirmCreateViewModel()
            viewModel.viewState.test { expectMostRecentItem() }

            viewModel.onIntent(SkipConfirmationToggled(isChecked = true))
            viewModel.sideEffect.test {
                viewModel.onIntent(MainButtonIntent)

                assertIs<CloseWithPermissionsConfirmed>(awaitItem())
            }
            verify(get<SetPermissionsConfirmationOptOutUseCase>())
                .execute(SetPermissionsConfirmationOptOutUseCase.Input(isOptedOut = true))
        }

    @Test
    fun `snapshot creation failure closes the screen with a failure message`() =
        runTest {
            get<CreatePermissionsSnapshotInteractor>().stub {
                onBlocking { createForFolder(FOLDER_ID) }
                    .doReturn(
                        CreatePermissionsSnapshotInteractor.Output.Failure(
                            DomainResult.Incomplete.Error(UNKNOWN, "error"),
                        ),
                    )
            }

            val viewModel = confirmCreateViewModel()

            viewModel.sideEffect.test {
                assertThat(awaitItem()).isEqualTo(ShowToast(ToastType.PERMISSIONS_FETCH_FAILURE))
                assertThat(awaitItem()).isEqualTo(NavigateBack)
            }
        }

    @Test
    fun `operator permission cannot be downgraded or removed`() =
        runTest {
            val viewModel = confirmCreateViewModel()
            val operatorPermission = operatorPermission(viewModel)

            viewModel.onIntent(UserPermissionModified(operatorPermission.copy(permission = ResourcePermission.READ)))
            viewModel.onIntent(UserPermissionDeleted(operatorPermission))

            assertThat(operatorPermission(viewModel).permission).isEqualTo(ResourcePermission.OWNER)
        }

    @Test
    fun `operator permission opens details in view mode from snapshot`() =
        runTest {
            val viewModel = confirmCreateViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(SeePermission(operatorPermission(viewModel)))

                val effect = assertIs<NavigateToUserPermissionDetails>(awaitItem())
                assertThat(effect.mode).isEqualTo(PermissionsMode.VIEW)
                assertThat(effect.fromSnapshot).isTrue()
            }
        }

    @Test
    fun `other user permission opens details in edit mode from snapshot`() =
        runTest {
            val viewModel = confirmCreateViewModel()
            val userPermission =
                viewModel.viewState.value.permissions
                    .filterIsInstance<PermissionModelUi.UserPermissionModel>()
                    .single { it.user.userId == USER_ID }

            viewModel.sideEffect.test {
                viewModel.onIntent(SeePermission(userPermission))

                val effect = assertIs<NavigateToUserPermissionDetails>(awaitItem())
                assertThat(effect.mode).isEqualTo(PermissionsMode.EDIT)
                assertThat(effect.fromSnapshot).isTrue()
            }
        }

    @Test
    fun `group permission opens details from snapshot`() =
        runTest {
            val viewModel = confirmCreateViewModel()
            val groupPermission =
                viewModel.viewState.value.permissions
                    .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
                    .single()

            viewModel.sideEffect.test {
                viewModel.onIntent(SeePermission(groupPermission))

                val effect = assertIs<NavigateToGroupPermissionDetails>(awaitItem())
                assertThat(effect.mode).isEqualTo(PermissionsMode.EDIT)
                assertThat(effect.fromSnapshot).isTrue()
            }
        }

    private fun operatorPermission(viewModel: PermissionsViewModel) =
        viewModel.viewState.value.permissions
            .filterIsInstance<PermissionModelUi.UserPermissionModel>()
            .single { it.user.userId == OPERATOR_ID }

    private fun confirmCreateViewModel() =
        get<PermissionsViewModel>(
            parameters = {
                parametersOf(FOLDER_ID, PermissionsMode.EDIT, PermissionsItem.FOLDER, PermissionsFlow.CONFIRM_CREATE)
            },
        )

    private fun featureFlags(isOptOutAvailable: Boolean) =
        FeatureFlagsModel(
            privacyPolicyUrl = null,
            termsAndConditionsUrl = null,
            isPreviewPasswordAvailable = true,
            areFoldersAvailable = true,
            areTagsAvailable = false,
            isTotpAvailable = false,
            isRbacAvailable = false,
            isPasswordExpiryAvailable = false,
            arePasswordPoliciesAvailable = false,
            canUpdatePasswordPolicies = false,
            isV5MetadataAvailable = false,
            isPermissionsConfirmationOptOutAvailable = isOptOutAvailable,
        )

    private companion object {
        private const val FOLDER_ID = "folder-id"
        private const val GROUP_ID = "group-id"
        private const val OPERATOR_ID = "operator-id"
        private const val USER_ID = "user-id"

        private fun userProfile(userId: String) =
            UserProfile(
                id = userId,
                username = "$userId@passbolt.com",
                disabled = false,
                role = null,
                firstName = "first-$userId",
                lastName = "last-$userId",
                avatarUrl = null,
                gpgKey =
                    GpgKey(
                        id = "gpg-$userId",
                        armoredKey = "armored-key-$userId",
                        fingerprint = "fingerprint-$userId",
                        bits = 2048,
                        uid = null,
                        keyId = "key-$userId",
                        type = null,
                        keyExpirationDate = null,
                        keyCreationDate = null,
                    ),
            )

        private val SNAPSHOT =
            PermissionsSnapshot(
                permissions =
                    listOf(
                        PermissionModel.UserPermissionModel(ResourcePermission.UPDATE, "perm-operator", OPERATOR_ID),
                        PermissionModel.UserPermissionModel(ResourcePermission.READ, "perm-user", USER_ID),
                        PermissionModel.GroupPermissionModel(ResourcePermission.UPDATE, "perm-group", GroupModel(GROUP_ID, "group")),
                    ),
                groupsMembers = mapOf(GROUP_ID to listOf(USER_ID)),
                users = listOf(userProfile(OPERATOR_ID), userProfile(USER_ID)).associateBy { it.id },
                created = ZonedDateTime.now(),
            )

        private val CURRENT_USER_UI_MODEL =
            UserUiModel(
                id = OPERATOR_ID,
                userName = "$OPERATOR_ID@passbolt.com",
                disabled = false,
                gpgKey =
                    GpgKeyUiModel(
                        id = "gpg-$OPERATOR_ID",
                        armoredKey = "armored-key-$OPERATOR_ID",
                        fingerprint = "fingerprint-$OPERATOR_ID",
                        bits = 2048,
                        uid = null,
                        keyId = "key-$OPERATOR_ID",
                        type = null,
                        keyExpirationDate = null,
                        keyCreationDate = null,
                    ),
                profile =
                    UserProfileUiModel(
                        username = "$OPERATOR_ID@passbolt.com",
                        firstName = "first-$OPERATOR_ID",
                        lastName = "last-$OPERATOR_ID",
                        avatarUrl = null,
                    ),
            )
    }
}
