package net.svaroh.passly.permissions.grouppermissionsdetails

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.commongroups.usecase.db.GetGroupWithUsersUseCase
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.CancelPermissionDelete
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.ConfirmPermissionDelete
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.DeletePermission
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.GoBack
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.Save
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.SeeGroupMembers
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsIntent.SelectPermission
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateBack
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateToGroupMembers
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetDeletePermissionResult
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetUpdatedPermissionResult
import net.svaroh.passly.ui.GpgKeyModel
import net.svaroh.passly.ui.GroupModel
import net.svaroh.passly.ui.GroupWithUsersModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.PermissionsMode.EDIT
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.ResourcePermission.UPDATE
import net.svaroh.passly.ui.UserModel
import net.svaroh.passly.ui.UserProfileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
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
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class GroupPermissionsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetGroupWithUsersUseCase>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factory { (permission: PermissionModelUi.GroupPermissionModel, mode: PermissionsMode) ->
                            GroupPermissionsViewModel(
                                mode = mode,
                                permission = permission,
                                getGroupWithUsersUseCase = get(),
                                coroutineLaunchContext = get(),
                            )
                        }
                        singleOf(::SessionRefreshTrackingFlow)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: GroupPermissionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getGroupWithUsersUseCase = get<GetGroupWithUsersUseCase>()
        getGroupWithUsersUseCase.stub {
            onBlocking { execute(GetGroupWithUsersUseCase.Input(GROUP.groupId)) }
                .doReturn(GetGroupWithUsersUseCase.Output(GROUP_WITH_USERS))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize in view mode should set state correctly`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, PermissionsMode.VIEW) }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.groupPermission).isEqualTo(GROUP_PERMISSION)
                assertThat(state.isEditMode).isFalse()
            }
        }

    @Test
    fun `initialize in edit mode should set state correctly`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.groupPermission).isEqualTo(GROUP_PERMISSION)
                assertThat(state.isEditMode).isTrue()
            }
        }

    @Test
    fun `selecting permission should update group permission in state`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(SelectPermission(UPDATE))
                assertThat(awaitItem().groupPermission.permission).isEqualTo(UPDATE)
            }
        }

    @Test
    fun `save should emit updated permission result`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.onIntent(SelectPermission(UPDATE))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)

                val setResult = awaitItem()
                assertIs<SetUpdatedPermissionResult>(setResult)
                assertThat(setResult.permission.permission).isEqualTo(UPDATE)
            }
        }

    @Test
    fun `delete permission should show confirmation then emit result`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DeletePermission)
                assertThat(awaitItem().isDeleteConfirmationVisible).isTrue()
            }

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmPermissionDelete)
                assertThat(viewModel.viewState.value.isDeleteConfirmationVisible).isFalse()

                val deleteResult = awaitItem()
                assertIs<SetDeletePermissionResult>(deleteResult)
                assertThat(deleteResult.permission).isEqualTo(GROUP_PERMISSION)
            }
        }

    @Test
    fun `cancel delete should hide confirmation dialog`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, EDIT) }

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DeletePermission)
                assertThat(awaitItem().isDeleteConfirmationVisible).isTrue()

                viewModel.onIntent(CancelPermissionDelete)
                assertThat(awaitItem().isDeleteConfirmationVisible).isFalse()
            }
        }

    @Test
    fun `go back should emit navigate back side effect`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, PermissionsMode.VIEW) }

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)

                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `see group members should emit navigate to group members side effect`() =
        runTest {
            viewModel = get { parametersOf(GROUP_PERMISSION, PermissionsMode.VIEW) }

            viewModel.sideEffect.test {
                viewModel.onIntent(SeeGroupMembers)

                val effect = awaitItem()
                assertIs<NavigateToGroupMembers>(effect)
                assertThat(effect.groupId).isEqualTo(GROUP.groupId)
            }
        }

    private companion object {
        private val USER =
            UserModel(
                id = "userId",
                userName = "userName",
                disabled = false,
                gpgKey =
                    GpgKeyModel(
                        armoredKey = "keyData",
                        fingerprint = "fingerprint",
                        bits = 1,
                        uid = "uid",
                        keyId = "keyid",
                        type = "rsa",
                        keyExpirationDate = ZonedDateTime.now(),
                        keyCreationDate = ZonedDateTime.now(),
                        id = UUID.randomUUID().toString(),
                    ),
                profile =
                    UserProfileModel(
                        username = "username",
                        firstName = "first",
                        lastName = "last",
                        avatarUrl = "avatarUrl",
                    ),
            )
        private val GROUP = GroupModel("grId", "grName")
        private val GROUP_PERMISSION = PermissionModelUi.GroupPermissionModel(ResourcePermission.READ, "permId", GROUP)
        private val GROUP_WITH_USERS = GroupWithUsersModel(GROUP, listOf(USER))
    }
}
