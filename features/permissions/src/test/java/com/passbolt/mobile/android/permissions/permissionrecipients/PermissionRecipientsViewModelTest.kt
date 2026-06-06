package net.svaroh.passly.permissions.permissionrecipients

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.common.search.SearchableMatcher
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.commongroups.usecase.db.GetLocalGroupsUseCase
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode
import net.svaroh.passly.core.users.usecase.db.GetLocalUsersUseCase
import net.svaroh.passly.mappers.GroupsModelMapper
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.mappers.UsersModelMapper
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.GoBack
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.Save
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.Search
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.ToggleGroupSelection
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.ToggleUserSelection
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsSideEffect.NavigateBack
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsSideEffect.NavigateBackWithResult
import net.svaroh.passly.ui.GpgKeyModel
import net.svaroh.passly.ui.GroupModel
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.UserModel
import net.svaroh.passly.ui.UserProfileModel
import net.svaroh.passly.ui.UserWithAvatar
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
class PermissionRecipientsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetLocalGroupsUseCase>() }
                    single { mock<GetLocalUsersUseCase>() }
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    factory { GroupsModelMapper() }
                    factory { UsersModelMapper() }
                    factory { PermissionsModelMapper(groupsModelMapper = get(), usersModelMapper = get()) }
                    factory { SearchableMatcher() }
                    singleOf(::SessionRefreshTrackingFlow)
                    factory { params ->
                        PermissionRecipientsViewModel(
                            alreadyAddedGroupPermissions = params.get(),
                            alreadyAddedUserPermissions = params.get(),
                            getLocalGroupsUseCase = get(),
                            getLocalUsersUseCase = get(),
                            permissionsModelMapper = get(),
                            searchableMatcher = get(),
                            coroutineLaunchContext = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PermissionRecipientsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        get<GetLocalGroupsUseCase>().stub {
            onBlocking { execute(GetLocalGroupsUseCase.Input(emptyList())) }
                .doReturn(GetLocalGroupsUseCase.Output(listOf(GROUP)))
        }
        get<GetLocalUsersUseCase>().stub {
            onBlocking { execute(GetLocalUsersUseCase.Input(emptyList())) }
                .doReturn(GetLocalUsersUseCase.Output(listOf(USER)))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load recipients excluding already added`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.filteredGroups).containsExactly(GROUP)
                assertThat(state.filteredUsers).containsExactly(USER)
            }
        }

    @Test
    fun `search should filter groups and users and update icon mode`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search("grName"))
                val state = awaitItem()
                assertThat(state.filteredGroups).containsExactly(GROUP)
                assertThat(state.filteredUsers).isEmpty()
                assertThat(state.searchInputEndIconMode).isEqualTo(SearchInputEndIconMode.CLEAR)
            }
        }

    @Test
    fun `empty search should reset icon mode to NONE`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search("abc"))
                awaitItem()

                viewModel.onIntent(Search(""))
                val state = awaitItem()
                assertThat(state.searchInputEndIconMode).isEqualTo(SearchInputEndIconMode.NONE)
                assertThat(state.filteredExistingPermissions).isEmpty()
            }
        }

    @Test
    fun `search with no results should show empty state`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search("nonexistent"))
                val state = awaitItem()
                assertThat(state.showEmptyState).isTrue()
            }
        }

    @Test
    fun `toggle group selection should update selected ids and display permissions`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ToggleGroupSelection(GROUP))
                val state = awaitItem()
                assertThat(state.selectedGroupIds).contains(GROUP.groupId)
                val recomputed = awaitItem()
                assertThat(recomputed.currentPermissions).hasSize(1)
            }
        }

    @Test
    fun `toggle group selection twice should deselect`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ToggleGroupSelection(GROUP))
                skipItems(1) // recomputed
                awaitItem()

                viewModel.onIntent(ToggleGroupSelection(GROUP))
                val state = awaitItem()
                assertThat(state.selectedGroupIds).isEmpty()
                val recomputed = awaitItem()
                assertThat(recomputed.currentPermissions).isEmpty()
            }
        }

    @Test
    fun `toggle user selection should update selected ids and display permissions`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ToggleUserSelection(USER))
                val state = awaitItem()
                assertThat(state.selectedUserIds).contains(USER.id)
                val recomputed = awaitItem()
                assertThat(recomputed.currentPermissions).hasSize(1)
            }
        }

    @Test
    fun `save should emit navigate back with result`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.onIntent(ToggleGroupSelection(GROUP))

            viewModel.sideEffect.test {
                viewModel.onIntent(Save)
                val effect = awaitItem()
                assertIs<NavigateBackWithResult>(effect)
                assertThat(effect.permissions).hasSize(1)
                assertThat(effect.permissions[0].permission).isEqualTo(ResourcePermission.READ)
            }
        }

    @Test
    fun `go back should emit navigate back side effect`() =
        runTest {
            viewModel =
                get(parameters = {
                    parametersOf(emptyArray<GroupPermissionModel>(), emptyArray<UserPermissionModel>())
                })

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `search should show existing permissions that match`() =
        runTest {
            val existingGroupPermission =
                GroupPermissionModel(
                    permission = ResourcePermission.READ,
                    permissionId = "existingPermId",
                    group = EXISTING_GROUP,
                )

            get<GetLocalGroupsUseCase>().stub {
                onBlocking { execute(GetLocalGroupsUseCase.Input(listOf(EXISTING_GROUP.groupId))) }
                    .doReturn(GetLocalGroupsUseCase.Output(listOf(GROUP)))
            }

            viewModel =
                get(parameters = {
                    parametersOf(arrayOf(existingGroupPermission), emptyArray<UserPermissionModel>())
                })

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search(EXISTING_GROUP.groupName))
                val state = awaitItem()
                assertThat(state.filteredExistingPermissions).hasSize(1)
            }
        }

    private companion object {
        private val GROUP = GroupModel(groupId = "grId", groupName = "grName")
        private val EXISTING_GROUP = GroupModel(groupId = "existingGrId", groupName = "existingGroup")

        private val USER_WITH_AVATAR =
            UserWithAvatar(
                userId = "userId",
                firstName = "first",
                lastName = "last",
                userName = "userName",
                isDisabled = false,
                avatarUrl = "avatarUrl",
            )
        private val USER =
            UserModel(
                id = USER_WITH_AVATAR.userId,
                userName = USER_WITH_AVATAR.userName,
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
                        firstName = USER_WITH_AVATAR.firstName,
                        lastName = USER_WITH_AVATAR.lastName,
                        avatarUrl = USER_WITH_AVATAR.avatarUrl,
                    ),
            )
    }
}
