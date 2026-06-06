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

package net.svaroh.passly.feature.home.screen

import androidx.paging.PagingData
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import net.svaroh.passly.common.autofill.DetectAutofillConflict
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.accounts.AccountSwitchFlow
import net.svaroh.passly.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.AVATAR
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.CLEAR
import net.svaroh.passly.entity.home.HomeDisplayView
import net.svaroh.passly.feature.home.screen.HomeIntent.CloseCreateResourceMenu
import net.svaroh.passly.feature.home.screen.HomeIntent.CloseSwitchAccount
import net.svaroh.passly.feature.home.screen.HomeIntent.CreateFolder
import net.svaroh.passly.feature.home.screen.HomeIntent.CreateNote
import net.svaroh.passly.feature.home.screen.HomeIntent.CreatePassword
import net.svaroh.passly.feature.home.screen.HomeIntent.CreateTotp
import net.svaroh.passly.feature.home.screen.HomeIntent.Initialize
import net.svaroh.passly.feature.home.screen.HomeIntent.OpenCreateResourceMenu
import net.svaroh.passly.feature.home.screen.HomeIntent.Search
import net.svaroh.passly.feature.home.screen.HomeIntent.SearchEndIconAction
import net.svaroh.passly.feature.home.screen.HomeSideEffect.InitiateDataRefresh
import net.svaroh.passly.feature.home.screen.HomeSideEffect.NavigateToCreateFolder
import net.svaroh.passly.feature.home.screen.HomeSideEffect.NavigateToCreateResourceForm
import net.svaroh.passly.feature.home.screen.HomeSideEffect.NavigateToCreateTotp
import net.svaroh.passly.feature.home.screen.HomeSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.home.screen.HomeSideEffect.ShowSuccessSnackbar
import net.svaroh.passly.feature.home.screen.ShowSuggestedModel.DoNotShow
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.NO_SHARED_KEY_ACCESS
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.FOLDER_CREATED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.RESOURCE_CREATED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.RESOURCE_DELETED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.RESOURCE_EDITED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.RESOURCE_SHARED
import net.svaroh.passly.feature.home.screen.data.HomeData
import net.svaroh.passly.feature.home.screen.data.HomeDataProvider
import net.svaroh.passly.jsonmodel.JSON_MODEL_GSON
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathJsonPathOps
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathsOps
import net.svaroh.passly.mappers.HomeDisplayViewMapper
import net.svaroh.passly.metadata.usecase.CanCreateResourceUseCase
import net.svaroh.passly.metadata.usecase.CanShareResourceUseCase
import net.svaroh.passly.ui.DefaultFilterModel
import net.svaroh.passly.ui.Folder
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.HomeDisplayViewModel.AllItems
import net.svaroh.passly.ui.HomeDisplayViewModel.NotLoaded
import net.svaroh.passly.ui.LeadingContentType.PASSWORD
import net.svaroh.passly.ui.LeadingContentType.STANDALONE_NOTE
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.ResourceModel
import net.svaroh.passly.ui.ResourcePermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
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
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.EnumSet
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class HomeViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    singleOf(::DataRefreshTrackingFlow)
                    singleOf(::SessionRefreshTrackingFlow)
                    single { mock<GetSelectedAccountDataUseCase>() }
                    single { mock<GetHomeDisplayViewPrefsUseCase>() }
                    single { mock<HomeDisplayViewMapper>() }
                    single { mock<HomeDataProvider>() }
                    single { mock<GetLocalFolderDetailsUseCase>() }
                    single { mock<CanCreateResourceUseCase>() }
                    single { mock<CanShareResourceUseCase>() }
                    single { mock<DetectAutofillConflict>() }
                    single { AccountSwitchFlow(mock { on { execute(any()) } doReturn GetSelectedAccountUseCase.Output("id1") }) }
                    single(named(JSON_MODEL_GSON)) { GsonBuilder().serializeNulls().create() }
                    single {
                        Configuration
                            .builder()
                            .jsonProvider(GsonJsonProvider())
                            .mappingProvider(GsonMappingProvider())
                            .options(EnumSet.noneOf(Option::class.java))
                            .build()
                    }
                    singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
                    factoryOf(::HomeViewModel)
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(get<GetSelectedAccountDataUseCase>().execute(anyOrNull())).thenReturn(
            GetSelectedAccountDataUseCase.Output(
                firstName = "First",
                lastName = "Last",
                email = "first@passbolt.com",
                avatarUrl = "www.passbolt.com/avatar.png",
                url = "www.passbolt.com",
                serverId = "1",
                label = "label",
                role = "user",
            ),
        )

        whenever(get<GetHomeDisplayViewPrefsUseCase>().execute(any())).thenReturn(
            GetHomeDisplayViewPrefsUseCase.Output(
                lastUsedHomeView = HomeDisplayView.ALL_ITEMS,
                userSetHomeView = DefaultFilterModel.ALL_ITEMS,
            ),
        )

        whenever(get<HomeDisplayViewMapper>().map(any(), any())).thenReturn(AllItems)

        get<HomeDataProvider>().stub {
            onBlocking {
                provideData(
                    any(),
                    any(),
                    any(),
                )
            }.doReturn(HomeData())
        }

        get<CanCreateResourceUseCase>().stub {
            onBlocking { execute(any()) }.doReturn(CanCreateResourceUseCase.Output(canCreateResource = true))
        }

        get<CanShareResourceUseCase>().stub {
            onBlocking { execute(any()) }.doReturn(CanShareResourceUseCase.Output(canShareResource = true))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should show user avatar on init`() =
        runTest {
            val avatar = "avatar_url"
            whenever(get<GetSelectedAccountDataUseCase>().execute(anyOrNull())).thenReturn(
                GetSelectedAccountDataUseCase.Output(
                    firstName = "First",
                    lastName = "Last",
                    email = "user@example.com",
                    avatarUrl = avatar,
                    url = "https://passbolt.com",
                    serverId = "server1",
                    label = "Server Label",
                    role = "user",
                ),
            )

            viewModel = get()

            assertThat(viewModel.viewState.value.userAvatar).isEqualTo(avatar)
        }

    @Test
    fun `should update search state when search query changes`() =
        runTest {
            val mockHomeData = mockResourcesData()
            whenever(get<HomeDataProvider>().provideData(any(), any(), any())).thenReturn(mockHomeData)

            viewModel = get()
            viewModel.onIntent(Search("test query"))

            viewModel.viewState.drop(1).test {
                val updatedState = awaitItem()
                assertThat(updatedState.searchQuery).isEqualTo("test query")
                assertThat(updatedState.searchInputEndIconMode).isEqualTo(CLEAR)
                assertThat(updatedState.homeData).isEqualTo(mockHomeData)
            }
        }

    @Test
    fun `should clear search and reset icon mode when search cleared`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Search("test query"))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(SearchEndIconAction)
                val updatedState = awaitItem()
                assertThat(updatedState.searchQuery).isEmpty()
                assertThat(updatedState.searchInputEndIconMode).isEqualTo(AVATAR)
            }
        }

    @Test
    fun `should show and close account switcher when requested`() =
        runTest {
            viewModel = get()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(SearchEndIconAction)
                assertThat(awaitItem().showAccountSwitchBottomSheet).isTrue()
                viewModel.onIntent(CloseSwitchAccount)
                assertThat(awaitItem().showAccountSwitchBottomSheet).isFalse()
            }
        }

    @Test
    fun `should show and hide create resource menu when requested`() =
        runTest {
            viewModel = get()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(OpenCreateResourceMenu)
                assertThat(awaitItem().showCreateResourceBottomSheet).isTrue()
                viewModel.onIntent(CloseCreateResourceMenu)
                assertThat(awaitItem().showCreateResourceBottomSheet).isFalse()
            }
        }

    @Test
    fun `should navigate to correct create resource form when creating resource`() =
        runTest {
            mockCanCreateResource(true)
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreatePassword)
                val createPasswordEffect = awaitItem()
                assertIs<NavigateToCreateResourceForm>(createPasswordEffect)
                assertThat(createPasswordEffect.leadingContentType).isEqualTo(PASSWORD)
                assertThat(createPasswordEffect.folderId).isNull()

                viewModel.onIntent(CreateNote)
                val createNoteEffect = awaitItem()
                assertIs<NavigateToCreateResourceForm>(createNoteEffect)
                assertThat(createNoteEffect.leadingContentType).isEqualTo(STANDALONE_NOTE)
                assertThat(createNoteEffect.folderId).isNull()

                viewModel.onIntent(CreateTotp)
                val createTotpEffect = awaitItem()
                assertIs<NavigateToCreateTotp>(createTotpEffect)
                assertThat(createTotpEffect.folderId).isNull()

                viewModel.onIntent(CreateFolder)
                val createFolderEffect = awaitItem()
                assertIs<NavigateToCreateFolder>(createFolderEffect)
                assertThat(createFolderEffect.folderId).isNull()
            }
        }

    @Test
    fun `should navigate to correct create resource form when creating resource in child folder`() =
        runTest {
            mockCanCreateResource(true)
            viewModel = get()

            viewModel.onIntent(
                HomeIntent.ShowHomeView(
                    HomeDisplayViewModel.Folders(
                        Folder.Child("folderId"),
                    ),
                ),
            )
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreatePassword)
                val createPasswordEffect = awaitItem()
                assertIs<NavigateToCreateResourceForm>(createPasswordEffect)
                assertThat(createPasswordEffect.leadingContentType).isEqualTo(PASSWORD)
                assertThat(createPasswordEffect.folderId).isEqualTo("folderId")

                viewModel.onIntent(CreateNote)
                val createNoteEffect = awaitItem()
                assertIs<NavigateToCreateResourceForm>(createNoteEffect)
                assertThat(createNoteEffect.leadingContentType).isEqualTo(STANDALONE_NOTE)
                assertThat(createNoteEffect.folderId).isEqualTo("folderId")

                viewModel.onIntent(CreateTotp)
                val createTotpEffect = awaitItem()
                assertIs<NavigateToCreateTotp>(createTotpEffect)
                assertThat(createTotpEffect.folderId).isEqualTo("folderId")

                viewModel.onIntent(CreateFolder)
                val createFolderEffect = awaitItem()
                assertIs<NavigateToCreateFolder>(createFolderEffect)
                assertThat(createFolderEffect.folderId).isEqualTo("folderId")
            }
        }

    @Test
    fun `should show error when cannot create resource`() =
        runTest {
            mockCanCreateResource(false)
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreatePassword)
                val createPasswordEffect = awaitItem()
                assertIs<ShowErrorSnackbar>(createPasswordEffect)
                assertThat(createPasswordEffect.type).isEqualTo(NO_SHARED_KEY_ACCESS)

                viewModel.onIntent(CreateNote)
                val createNoteEffect = awaitItem()
                assertIs<ShowErrorSnackbar>(createNoteEffect)
                assertThat(createNoteEffect.type).isEqualTo(NO_SHARED_KEY_ACCESS)

                viewModel.onIntent(CreateTotp)
                val createTotpEffect = awaitItem()
                assertIs<ShowErrorSnackbar>(createTotpEffect)
                assertThat(createTotpEffect.type).isEqualTo(NO_SHARED_KEY_ACCESS)

                viewModel.onIntent(CreateFolder)
                val createFolderEffect = awaitItem()
                assertIs<NavigateToCreateFolder>(createFolderEffect)
                assertThat(createFolderEffect.folderId).isEqualTo(null)
            }
        }

    @Test
    fun `should update state during data refresh`() =
        runTest {
            mockHomeData()
            val dataRefreshFlow: DataRefreshTrackingFlow = get()

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))

            viewModel.viewState.drop(2).test {
                dataRefreshFlow.updateStatus(InProgress)
                val inProgress = awaitItem()
                assertThat(inProgress.isRefreshing).isTrue()
                assertThat(inProgress.canCreateResource).isFalse()

                dataRefreshFlow.updateStatus(FinishedWithSuccess)
                val finished = awaitItem()
                assertThat(finished.isRefreshing).isFalse()
                assertThat(finished.canCreateResource).isTrue()
            }
        }

    @Test
    fun `should show error on refresh failure`() =
        runTest {
            val dataRefreshFlow: DataRefreshTrackingFlow = get()
            mockHomeData()
            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, null))

            viewModel.viewState.drop(2).test {
                dataRefreshFlow.updateStatus(InProgress)
                val inProgress = awaitItem()
                assertThat(inProgress.isRefreshing).isTrue()
                assertThat(inProgress.canCreateResource).isFalse()

                dataRefreshFlow.updateStatus(FinishedWithFailure)
                val finished = awaitItem()
                assertThat(finished.isRefreshing).isFalse()
                assertThat(finished.canCreateResource).isFalse()

                viewModel.sideEffect.test {
                    val effect = awaitItem()
                    assertIs<ShowErrorSnackbar>(effect)
                    assertThat(effect.type).isEqualTo(FAILED_TO_REFRESH_DATA)
                }
            }
        }

    @Test
    fun `should show success snackbar after resource form return`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                // create
                viewModel.onIntent(
                    HomeIntent.ResourceFormReturned(
                        resourceCreated = true,
                        resourceEdited = false,
                        resourceName = "Test Resource",
                    ),
                )

                assertIs<InitiateDataRefresh>(awaitItem())
                val createSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(createSnackbarEffect)
                assertEquals(RESOURCE_CREATED, createSnackbarEffect.type)
                assertEquals("Test Resource", createSnackbarEffect.message)

                // edit
                viewModel.onIntent(
                    HomeIntent.ResourceFormReturned(
                        resourceCreated = false,
                        resourceEdited = true,
                        resourceName = "Test Resource",
                    ),
                )

                assertIs<InitiateDataRefresh>(awaitItem())
                val editSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(editSnackbarEffect)
                assertEquals(RESOURCE_EDITED, editSnackbarEffect.type)
                assertEquals("Test Resource", editSnackbarEffect.message)
            }
        }

    @Test
    fun `should refresh data after resource details return with edit`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    HomeIntent.ResourceDetailsReturned(
                        resourceEdited = true,
                        resourceDeleted = false,
                        resourceName = "Test Resource",
                    ),
                )

                assertIs<InitiateDataRefresh>(awaitItem())
            }
        }

    @Test
    fun `should show success snackbar after resource details return with delete`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    HomeIntent.ResourceDetailsReturned(
                        resourceEdited = false,
                        resourceDeleted = true,
                        resourceName = "Test Resource",
                    ),
                )

                val deleteSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(deleteSnackbarEffect)
                assertEquals(RESOURCE_DELETED, deleteSnackbarEffect.type)
                assertEquals("Test Resource", deleteSnackbarEffect.message)
                assertIs<InitiateDataRefresh>(awaitItem())
            }
        }

    @Test
    fun `should show success snackbar after folder created`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    HomeIntent.FolderCreateReturned(
                        folderName = "Test Folder",
                    ),
                )

                val createSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(createSnackbarEffect)
                assertEquals(FOLDER_CREATED, createSnackbarEffect.type)
                assertEquals("Test Folder", createSnackbarEffect.message)
                assertIs<InitiateDataRefresh>(awaitItem())
            }
        }

    @Test
    fun `should show success snackbar after permissions updated`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    HomeIntent.ResourceShareReturned(
                        resourceShared = true,
                    ),
                )

                val shareSnackbarEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(shareSnackbarEffect)
                assertEquals(RESOURCE_SHARED, shareSnackbarEffect.type)
                assertIs<InitiateDataRefresh>(awaitItem())
            }
        }

    @Test
    fun `should detect autofill conflict when detector returns true on initialize`() =
        runTest {
            mockHomeData()
            val detectAutofillConflict: DetectAutofillConflict = get()
            whenever(detectAutofillConflict.invoke()).thenReturn(true)

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.isAutofillConflictDetected).isTrue()
            verify(detectAutofillConflict).invoke()
        }

    @Test
    fun `should not detect autofill conflict when detector returns false on initialize`() =
        runTest {
            mockHomeData()
            val detectAutofillConflict: DetectAutofillConflict = get()
            whenever(detectAutofillConflict.invoke()).thenReturn(false)

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.isAutofillConflictDetected).isFalse()
            verify(detectAutofillConflict).invoke()
        }

    @Test
    fun `initial state should have autofill conflict as false before initialize`() =
        runTest {
            viewModel = get()

            assertThat(viewModel.viewState.value.isAutofillConflictDetected).isFalse()
        }

    @Test
    fun `should reload home data and avatar when account switches`() =
        runTest {
            mockHomeData()
            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            val newAvatar = "new_avatar_url"
            whenever(get<GetSelectedAccountDataUseCase>().execute(anyOrNull())).thenReturn(
                GetSelectedAccountDataUseCase.Output(
                    firstName = "New",
                    lastName = "User",
                    email = "new@passbolt.com",
                    avatarUrl = newAvatar,
                    url = "www.passbolt.com",
                    serverId = "2",
                    label = "label2",
                    role = "user",
                ),
            )

            val newHomeData = mockResourcesData()
            whenever(get<HomeDataProvider>().provideData(any(), any(), any())).thenReturn(newHomeData)

            val accountSwitchFlow: AccountSwitchFlow = get()
            accountSwitchFlow.notifyAccountSwitch("id2")
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.userAvatar).isEqualTo(newAvatar)
            assertThat(viewModel.viewState.value.homeData).isEqualTo(newHomeData)
        }

    private fun mockCanCreateResource(canCreate: Boolean) {
        get<CanCreateResourceUseCase>().stub {
            onBlocking {
                execute(any())
            }.doReturn(
                CanCreateResourceUseCase.Output(canCreateResource = canCreate),
            )
        }
    }

    private fun mockHomeData() {
        val homeData = mockResourcesData()
        get<HomeDataProvider>().stub {
            onBlocking {
                provideData(
                    any(),
                    any(),
                    any(),
                )
            }.doReturn(homeData)
        }
    }

    private fun mockResourcesData() =
        HomeData(
            resourceList =
                flowOf(
                    PagingData.from(
                        listOf(
                            mockResourceModel("id1", "Resource 1"),
                            mockResourceModel("id2", "Resource 2"),
                        ),
                    ),
                ),
        )

    private fun mockResourceModel(
        id: String,
        name: String,
    ) = ResourceModel(
        resourceId = id,
        resourceTypeId = "resTypeId",
        folderId = "folderId",
        permission = ResourcePermission.READ,
        favouriteId = null,
        modified = ZonedDateTime.now(),
        expiry = null,
        metadataJsonModel =
            MetadataJsonModel(
                """
                {
                    "name": "$name",
                    "uri": "https://example.com",
                    "username": "testuser",
                    "description": "Test description"
                }
                """.trimIndent(),
            ),
        metadataKeyId = null,
        metadataKeyType = null,
    )
}
