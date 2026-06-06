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

package net.svaroh.passly.permissions.confirmpermissions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.commontest.session.validSessionTestModule
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.domain.groups.usecase.GetGroupWithUsersUseCase
import net.svaroh.passly.domain.groups.usecase.GroupsInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.domain.permissionsconfirmation.model.PermissionsSnapshot
import net.svaroh.passly.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import net.svaroh.passly.domain.resources.actions.ResourceShareActionsInteractor
import net.svaroh.passly.domain.resources.actions.ShareActionResult
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import net.svaroh.passly.domain.users.model.GpgKey
import net.svaroh.passly.domain.users.model.UserProfile
import net.svaroh.passly.domain.users.usecase.GetLocalCurrentUserUseCase
import net.svaroh.passly.domain.users.usecase.UsersInteractor
import net.svaroh.passly.mappers.UsersModelMapper
import net.svaroh.passly.permissions.common.PermissionsListMapper
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.Confirm
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.GroupPermissionDeleted
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.SeePermission
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.ShareRecipientsAdded
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.UserPermissionDeleted
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.UserPermissionModified
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.CloseWithPermissionsConfirmed
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.CloseWithShareSuccess
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateBack
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToGroupPermissionDetails
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToUserPermissionDetails
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowPermissionsDriftedSnackbar
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowToast
import net.svaroh.passly.permissions.permissions.PermissionModelUiComparator
import net.svaroh.passly.ui.ConfirmPermissionsMode
import net.svaroh.passly.ui.GpgKeyUiModel
import net.svaroh.passly.ui.GroupModel
import net.svaroh.passly.ui.GroupWithUsersModel
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.UserProfileUiModel
import net.svaroh.passly.ui.UserUiModel
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
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.ZonedDateTime
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmPermissionsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<CreatePermissionsSnapshotInteractor>() }
                    single { mock<ResourceShareActionsInteractor>() }
                    single { mock<MetadataPrivateKeysHelperInteractor>() }
                    single { mock<GetPermissionsSnapshotUseCase>() }
                    single { mock<GetGroupWithUsersUseCase>() }
                    single { mock<UsersInteractor>() }
                    single { mock<GroupsInteractor>() }
                    single { mock<GetLocalCurrentUserUseCase>() }
                    single { UsersModelMapper() }
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    singleOf(::SessionRefreshTrackingFlow)
                    factory { PermissionModelUiComparator() }
                    factory { PermissionsListMapper(get()) }
                    factory { params ->
                        ConfirmPermissionsViewModel(
                            confirmMode = params.get(),
                            driftedEntityNames = params.getOrNull(),
                            createPermissionsSnapshotInteractor = get(),
                            resourceShareActionsInteractor = get(),
                            metadataPrivateKeysHelperInteractor = get(),
                            getPermissionsSnapshotUseCase = get(),
                            getGroupWithUsersUseCase = get(),
                            usersInteractor = get(),
                            groupsInteractor = get(),
                            getLocalCurrentUserUseCase = get(),
                            usersModelMapper = get(),
                            permissionsListMapper = get(),
                            coroutineLaunchContext = get(),
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
            on { createForFolder(FOLDER_ID) }
                .doReturn(CreatePermissionsSnapshotInteractor.Output.Success(SNAPSHOT))
        }
        get<GetPermissionsSnapshotUseCase>().stub {
            on { execute(Unit) } doReturn GetPermissionsSnapshotUseCase.Output(SNAPSHOT)
        }
        get<UsersInteractor>().stub {
            on { fetchAndSaveUsers() } doReturn UsersInteractor.Output.Success
        }
        get<GroupsInteractor>().stub {
            on { fetchAndSaveGroups() } doReturn GroupsInteractor.Output.Success
        }
        get<GetLocalCurrentUserUseCase>().stub {
            on { execute(Unit) } doReturn GetLocalCurrentUserUseCase.Output(CURRENT_USER_UI_MODEL)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create mode shows the operator at their folder-derived permission`() =
        runTest {
            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                val state = expectMostRecentItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.permissions).hasSize(3)
                val userPermissions = state.permissions.filterIsInstance<PermissionModelUi.UserPermissionModel>()
                assertThat(userPermissions.single { it.user.userId == OPERATOR_ID }.permission)
                    .isEqualTo(ResourcePermission.UPDATE)
                assertThat(userPermissions.single { it.user.userId == USER_ID }.permission)
                    .isEqualTo(ResourcePermission.READ)
            }
        }

    @Test
    fun `create mode with group-derived operator access shows no direct operator permission`() =
        runTest {
            stubFolderSnapshot(groupOnlySnapshot())

            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                val permissions = expectMostRecentItem().permissions
                assertThat(permissions.filterIsInstance<PermissionModelUi.UserPermissionModel>()).isEmpty()
                assertThat(permissions.filterIsInstance<PermissionModelUi.GroupPermissionModel>()).hasSize(1)
            }
        }

    @Test
    fun `confirm publishes the confirmed permissions`() =
        runTest {
            stubFolderSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmCreateViewModel()
            viewModel.viewState.test { expectMostRecentItem() }

            viewModel.sideEffect.test {
                viewModel.onIntent(Confirm)

                val effect = assertIs<CloseWithPermissionsConfirmed>(awaitItem())
                assertThat(effect.permissions).hasSize(3)
            }
        }

    @Test
    fun `snapshot creation failure closes the screen with a failure message`() =
        runTest {
            get<CreatePermissionsSnapshotInteractor>().stub {
                on { createForFolder(FOLDER_ID) }
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
    fun `users and groups are refreshed when the confirmation opens`() =
        runTest {
            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                val state = expectMostRecentItem()
                assertThat(state.isPreparingPermissions).isFalse()
                assertThat(state.isRefreshingUsersAndGroups).isFalse()
                assertThat(state.isLoading).isFalse()
            }
            verify(get<UsersInteractor>()).fetchAndSaveUsers()
            verify(get<GroupsInteractor>()).fetchAndSaveGroups()
        }

    @Test
    fun `refresh failure does not block the confirmation`() =
        runTest {
            get<UsersInteractor>().stub {
                on { fetchAndSaveUsers() } doReturn
                    UsersInteractor.Output.Failure(AuthenticationState.Authenticated)
            }

            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                val state = expectMostRecentItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.permissions).isNotEmpty()
            }
        }

    @Test
    fun `create mode operator permission is editable and not locked when the operator owns the folder`() =
        runTest {
            stubFolderSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmCreateViewModel()
            val operatorPermission = operatorPermission(viewModel)

            viewModel.onIntent(UserPermissionModified(operatorPermission.copy(permission = ResourcePermission.UPDATE)))

            assertThat(operatorPermission(viewModel).permission).isEqualTo(ResourcePermission.UPDATE)
        }

    @Test
    fun `other user permission opens details in edit mode`() =
        runTest {
            stubFolderSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmCreateViewModel()
            val userPermission =
                viewModel.viewState.value.permissions
                    .filterIsInstance<PermissionModelUi.UserPermissionModel>()
                    .single { it.user.userId == USER_ID }

            viewModel.sideEffect.test {
                viewModel.onIntent(SeePermission(userPermission))

                val effect = assertIs<NavigateToUserPermissionDetails>(awaitItem())
                assertThat(effect.mode).isEqualTo(PermissionsMode.EDIT)
            }
        }

    @Test
    fun `create mode with parent folder owner operator is editable`() =
        runTest {
            stubFolderSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().isEditable).isTrue()
            }
        }

    @Test
    fun `create mode with non-owner parent folder operator is read only`() =
        runTest {
            val viewModel = confirmCreateViewModel()
            val groupPermission =
                viewModel.viewState.value.permissions
                    .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
                    .single()

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().isEditable).isFalse()
            }
            viewModel.sideEffect.test {
                viewModel.onIntent(SeePermission(otherUserPermission(viewModel)))
                assertThat(assertIs<NavigateToUserPermissionDetails>(awaitItem()).mode).isEqualTo(PermissionsMode.VIEW)

                viewModel.onIntent(SeePermission(groupPermission))
                assertThat(assertIs<NavigateToGroupPermissionDetails>(awaitItem()).mode).isEqualTo(PermissionsMode.VIEW)
            }
        }

    @Test
    fun `group permission opens group details`() =
        runTest {
            val viewModel = confirmCreateViewModel()
            val groupPermission =
                viewModel.viewState.value.permissions
                    .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
                    .single()

            viewModel.sideEffect.test {
                viewModel.onIntent(SeePermission(groupPermission))

                val effect = assertIs<NavigateToGroupPermissionDetails>(awaitItem())
                assertThat(effect.permission).isEqualTo(groupPermission)
            }
        }

    @Test
    fun `indirect access warning is shown for a solo user who is also a group member`() =
        runTest {
            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().indirectAccessWarning)
                    .isEqualTo(IndirectAccessWarning.SingleUser("first-$USER_ID last-$USER_ID", "group"))
            }
        }

    @Test
    fun `indirect access warning is cleared when the group permission is removed`() =
        runTest {
            val viewModel = confirmCreateViewModel()
            val groupPermission =
                viewModel.viewState.value.permissions
                    .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
                    .single()

            viewModel.onIntent(GroupPermissionDeleted(groupPermission))

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().indirectAccessWarning).isNull()
            }
        }

    @Test
    fun `indirect access warning lists all solo users with group access`() =
        runTest {
            stubFolderSnapshot(
                snapshot(
                    operatorPermission = ResourcePermission.UPDATE,
                    groupMembers = listOf(USER_ID, OPERATOR_ID),
                ),
            )

            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                val warning = expectMostRecentItem().indirectAccessWarning
                assertIs<IndirectAccessWarning.MultipleUsers>(warning)
                assertThat(warning.userNames)
                    .containsExactly("first-$USER_ID last-$USER_ID", "first-$OPERATOR_ID last-$OPERATOR_ID")
            }
        }

    @Test
    fun `no indirect access warning when solo users are not group members`() =
        runTest {
            stubFolderSnapshot(
                snapshot(operatorPermission = ResourcePermission.UPDATE, groupMembers = emptyList()),
            )

            val viewModel = confirmCreateViewModel()

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().indirectAccessWarning).isNull()
            }
        }

    @Test
    fun `indirect access warning covers groups added during the confirmation`() =
        runTest {
            stubFolderSnapshot(
                snapshot(operatorPermission = ResourcePermission.UPDATE, groupMembers = emptyList()),
            )
            get<GetGroupWithUsersUseCase>().stub {
                on { execute(GetGroupWithUsersUseCase.Input(ADDED_GROUP_ID)) } doReturn
                    GetGroupWithUsersUseCase.Output(
                        GroupWithUsersModel(
                            group = GroupModel(ADDED_GROUP_ID, "added-group"),
                            users = listOf(userUiModel(USER_ID)),
                        ),
                    )
            }
            val viewModel = confirmCreateViewModel()
            val addedGroup =
                PermissionModelUi.GroupPermissionModel(
                    permission = ResourcePermission.READ,
                    permissionId = "temporary-permission-id",
                    group = GroupModel(ADDED_GROUP_ID, "added-group"),
                )

            viewModel.onIntent(ShareRecipientsAdded(viewModel.viewState.value.permissions + addedGroup))

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().indirectAccessWarning)
                    .isEqualTo(IndirectAccessWarning.SingleUser("first-$USER_ID last-$USER_ID", "added-group"))
            }
        }

    @Test
    fun `edit mode shows snapshot permissions with real permission ids`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmEditViewModel()

            viewModel.viewState.test {
                val state = expectMostRecentItem()
                assertThat(state.permissions).hasSize(3)
                val operatorRow = operatorPermission(viewModel)
                assertThat(operatorRow.permission).isEqualTo(ResourcePermission.OWNER)
                assertThat(operatorRow.permissionId).isEqualTo("perm-operator")
                val groupRow = state.permissions.filterIsInstance<PermissionModelUi.GroupPermissionModel>().single()
                assertThat(groupRow.permissionId).isEqualTo("perm-group")
            }
        }

    @Test
    fun `edit mode with owner operator is editable`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmEditViewModel()
            val groupPermission =
                viewModel.viewState.value.permissions
                    .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
                    .single()

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().isEditable).isTrue()
            }
            viewModel.sideEffect.test {
                viewModel.onIntent(SeePermission(otherUserPermission(viewModel)))
                assertThat(assertIs<NavigateToUserPermissionDetails>(awaitItem()).mode).isEqualTo(PermissionsMode.EDIT)

                viewModel.onIntent(SeePermission(groupPermission))
                assertThat(assertIs<NavigateToGroupPermissionDetails>(awaitItem()).mode).isEqualTo(PermissionsMode.EDIT)

                viewModel.onIntent(SeePermission(operatorPermission(viewModel)))
                assertThat(assertIs<NavigateToUserPermissionDetails>(awaitItem()).mode).isEqualTo(PermissionsMode.EDIT)
            }
        }

    @Test
    fun `edit mode with operator owning through a group is editable`() =
        runTest {
            stubResourceSnapshot(
                snapshot(
                    operatorPermission = ResourcePermission.UPDATE,
                    groupPermission = ResourcePermission.OWNER,
                    groupMembers = listOf(USER_ID, OPERATOR_ID),
                ),
            )

            val viewModel = confirmEditViewModel()

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().isEditable).isTrue()
            }
        }

    @Test
    fun `edit mode with non-owner operator is read only`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.UPDATE))

            val viewModel = confirmEditViewModel()
            val groupPermission =
                viewModel.viewState.value.permissions
                    .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
                    .single()

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().isEditable).isFalse()
            }
            viewModel.sideEffect.test {
                viewModel.onIntent(SeePermission(operatorPermission(viewModel)))
                assertThat(assertIs<NavigateToUserPermissionDetails>(awaitItem()).mode).isEqualTo(PermissionsMode.VIEW)

                viewModel.onIntent(SeePermission(groupPermission))
                assertThat(assertIs<NavigateToGroupPermissionDetails>(awaitItem()).mode).isEqualTo(PermissionsMode.VIEW)
            }
        }

    @Test
    fun `edit mode operator permission can be downgraded`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmEditViewModel()
            val operatorRow = operatorPermission(viewModel)

            viewModel.onIntent(UserPermissionModified(operatorRow.copy(permission = ResourcePermission.READ)))

            val modifiedOperatorRow = operatorPermission(viewModel)
            assertThat(modifiedOperatorRow.permission).isEqualTo(ResourcePermission.READ)
            assertThat(modifiedOperatorRow.permissionId).isEqualTo("perm-operator")
        }

    @Test
    fun `edit mode operator permission can be removed`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmEditViewModel()

            viewModel.onIntent(UserPermissionDeleted(operatorPermission(viewModel)))

            val userIds =
                viewModel.viewState.value.permissions
                    .filterIsInstance<PermissionModelUi.UserPermissionModel>()
                    .map { it.user.userId }
            assertThat(userIds).doesNotContain(OPERATOR_ID)
        }

    @Test
    fun `reopening edit mode after drift shows the drift snackbar`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmEditViewModel(driftedEntityNames = listOf("drifted-user"))

            viewModel.sideEffect.test {
                assertThat(awaitItem()).isEqualTo(ShowPermissionsDriftedSnackbar(listOf("drifted-user")))
            }
        }

    @Test
    fun `resource snapshot creation failure closes the screen with a failure message`() =
        runTest {
            get<CreatePermissionsSnapshotInteractor>().stub {
                on { createForResource(RESOURCE_ID) }
                    .doReturn(
                        CreatePermissionsSnapshotInteractor.Output.Failure(
                            DomainResult.Incomplete.Error(UNKNOWN, "error"),
                        ),
                    )
            }

            val viewModel = confirmEditViewModel()

            viewModel.sideEffect.test {
                assertThat(awaitItem()).isEqualTo(ShowToast(ToastType.PERMISSIONS_FETCH_FAILURE))
                assertThat(awaitItem()).isEqualTo(NavigateBack)
            }
        }

    @Test
    fun `share mode shows snapshot permissions`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))

            val viewModel = confirmShareViewModel()

            viewModel.viewState.test {
                val state = expectMostRecentItem()
                assertThat(state.permissions).hasSize(3)
                assertThat(state.isEditable).isTrue()
            }
        }

    @Test
    fun `share mode confirm applies the share and closes on success`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))
            get<ResourceShareActionsInteractor>().stub {
                on { shareWithConfirmedPermissions(eq(RESOURCE_ID), any()) } doReturn ShareActionResult.Success
            }

            val viewModel = confirmShareViewModel()
            viewModel.viewState.test { expectMostRecentItem() }

            viewModel.sideEffect.test {
                viewModel.onIntent(Confirm)

                assertThat(awaitItem()).isEqualTo(CloseWithShareSuccess)
            }
        }

    @Test
    fun `share mode drift reloads the snapshot and informs`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))
            get<ResourceShareActionsInteractor>().stub {
                on { shareWithConfirmedPermissions(eq(RESOURCE_ID), any()) } doReturn
                    ShareActionResult.PermissionsDrifted(listOf("drifted-user"))
            }

            val viewModel = confirmShareViewModel()
            viewModel.viewState.test { expectMostRecentItem() }

            viewModel.sideEffect.test {
                viewModel.onIntent(Confirm)

                assertThat(awaitItem()).isEqualTo(ShowPermissionsDriftedSnackbar(listOf("drifted-user")))
            }
            verify(get<CreatePermissionsSnapshotInteractor>(), times(2)).createForResource(RESOURCE_ID)
        }

    @Test
    fun `share mode failure shows an error and stays on the screen`() =
        runTest {
            stubResourceSnapshot(snapshot(operatorPermission = ResourcePermission.OWNER))
            get<ResourceShareActionsInteractor>().stub {
                on { shareWithConfirmedPermissions(eq(RESOURCE_ID), any()) } doReturn
                    ShareActionResult.ShareFailure("error")
            }

            val viewModel = confirmShareViewModel()
            viewModel.viewState.test { expectMostRecentItem() }

            viewModel.sideEffect.test {
                viewModel.onIntent(Confirm)

                assertThat(awaitItem()).isEqualTo(ShowErrorSnackbar(SnackbarErrorType.SHARE_FAILED))
            }
            viewModel.viewState.test {
                assertThat(expectMostRecentItem().isApplyingShare).isFalse()
            }
        }

    private fun stubResourceSnapshot(snapshot: PermissionsSnapshot) {
        get<CreatePermissionsSnapshotInteractor>().stub {
            on { createForResource(RESOURCE_ID) }
                .doReturn(CreatePermissionsSnapshotInteractor.Output.Success(snapshot))
        }
        stubStoredSnapshot(snapshot)
    }

    private fun stubFolderSnapshot(snapshot: PermissionsSnapshot) {
        get<CreatePermissionsSnapshotInteractor>().stub {
            on { createForFolder(FOLDER_ID) }
                .doReturn(CreatePermissionsSnapshotInteractor.Output.Success(snapshot))
        }
        stubStoredSnapshot(snapshot)
    }

    private fun stubStoredSnapshot(snapshot: PermissionsSnapshot) {
        get<GetPermissionsSnapshotUseCase>().stub {
            on { execute(Unit) } doReturn GetPermissionsSnapshotUseCase.Output(snapshot)
        }
    }

    private fun operatorPermission(viewModel: ConfirmPermissionsViewModel) =
        viewModel.viewState.value.permissions
            .filterIsInstance<PermissionModelUi.UserPermissionModel>()
            .single { it.user.userId == OPERATOR_ID }

    private fun otherUserPermission(viewModel: ConfirmPermissionsViewModel) =
        viewModel.viewState.value.permissions
            .filterIsInstance<PermissionModelUi.UserPermissionModel>()
            .single { it.user.userId == USER_ID }

    private fun confirmCreateViewModel() =
        get<ConfirmPermissionsViewModel>(
            parameters = { parametersOf(ConfirmPermissionsMode.Create(FOLDER_ID), null) },
        )

    private fun confirmEditViewModel(driftedEntityNames: List<String>? = null) =
        get<ConfirmPermissionsViewModel>(
            parameters = { parametersOf(ConfirmPermissionsMode.Edit(RESOURCE_ID), driftedEntityNames) },
        )

    private fun confirmShareViewModel() =
        get<ConfirmPermissionsViewModel>(
            parameters = { parametersOf(ConfirmPermissionsMode.Share(RESOURCE_ID), null) },
        )

    private companion object {
        private const val FOLDER_ID = "folder-id"
        private const val RESOURCE_ID = "resource-id"
        private const val GROUP_ID = "group-id"
        private const val OPERATOR_ID = "operator-id"
        private const val ADDED_GROUP_ID = "added-group-id"
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

        private fun snapshot(
            operatorPermission: ResourcePermission,
            groupPermission: ResourcePermission = ResourcePermission.UPDATE,
            groupMembers: List<String> = listOf(USER_ID),
        ) = PermissionsSnapshot(
            permissions =
                listOf(
                    PermissionModel.UserPermissionModel(operatorPermission, "perm-operator", OPERATOR_ID),
                    PermissionModel.UserPermissionModel(ResourcePermission.READ, "perm-user", USER_ID),
                    PermissionModel.GroupPermissionModel(groupPermission, "perm-group", GroupModel(GROUP_ID, "group")),
                ),
            groupsMembers = mapOf(GROUP_ID to groupMembers),
            users = listOf(userProfile(OPERATOR_ID), userProfile(USER_ID)).associateBy { it.id },
            created = ZonedDateTime.now(),
        )

        private fun groupOnlySnapshot() =
            PermissionsSnapshot(
                permissions =
                    listOf(
                        PermissionModel.GroupPermissionModel(
                            ResourcePermission.OWNER,
                            "perm-group",
                            GroupModel(GROUP_ID, "group"),
                        ),
                    ),
                groupsMembers = mapOf(GROUP_ID to listOf(OPERATOR_ID)),
                users = emptyMap(),
                created = ZonedDateTime.now(),
            )

        private val SNAPSHOT = snapshot(operatorPermission = ResourcePermission.UPDATE)

        private fun userUiModel(userId: String) =
            UserUiModel(
                id = userId,
                userName = "$userId@passbolt.com",
                disabled = false,
                gpgKey =
                    GpgKeyUiModel(
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
                profile =
                    UserProfileUiModel(
                        username = "$userId@passbolt.com",
                        firstName = "first-$userId",
                        lastName = "last-$userId",
                        avatarUrl = null,
                    ),
            )

        private val CURRENT_USER_UI_MODEL = userUiModel(OPERATOR_ID)
    }
}
