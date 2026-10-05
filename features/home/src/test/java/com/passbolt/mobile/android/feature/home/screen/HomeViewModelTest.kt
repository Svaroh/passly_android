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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.common.autofill.DetectAutofillConflict
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.commontest.session.validSessionTestModule
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.domain.resources.usecase.ResourceContentTypeProvider
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.AVATAR
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.CLEAR
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderDetailsUseCase
import net.svaroh.passly.domain.metadata.interactor.ResourceAccessInteractor
import net.svaroh.passly.domain.preferences.usecase.GetHomeDisplayViewPreferencesUseCase
import net.svaroh.passly.domain.users.profile.UserProfileInteractor
import net.svaroh.passly.domain.users.profile.UserProfileRefreshTrackingFlow
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
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.PROFILE_FETCH_FAILURE
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
import net.svaroh.passly.ui.DefaultFilterUiModel
import net.svaroh.passly.ui.Folder
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.HomeDisplayViewModel.NotLoaded
import net.svaroh.passly.ui.HomeDisplayViewPreferencesUiModel
import net.svaroh.passly.ui.HomeDisplayViewUiModel
import net.svaroh.passly.ui.LeadingContentType.PASSWORD
import net.svaroh.passly.ui.LeadingContentType.STANDALONE_NOTE
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.ResourceUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.EnumSet
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
@Suppress("LargeClass")
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
                    single { mock<GetHomeDisplayViewPreferencesUseCase>() }
                    single { mock<HomeDataProvider>() }
                    single { mock<GetLocalFolderDetailsUseCase>() }
                    single { mock<ResourceAccessInteractor>() }
                    single { mock<DetectAutofillConflict>() }
                    single {
                        mock<UserProfileInteractor> {
                            on { fetchAndUpdateUserProfile() } doReturn UserProfileInteractor.Output.Success
                        }
                    }
                    singleOf(::UserProfileRefreshTrackingFlow)
                    single { mock<ResourceContentTypeProvider>() }
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
                validSessionTestModule,
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

        whenever(get<GetHomeDisplayViewPreferencesUseCase>().execute(Unit)).thenReturn(
            HomeDisplayViewPreferencesUiModel(
                lastUsedHomeView = HomeDisplayViewUiModel.ALL_ITEMS,
                userSetHomeView = DefaultFilterUiModel.ALL_ITEMS,
            ),
        )

        get<HomeDataProvider>().stub {
            on {
                provideData(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }.doReturn(HomeData())
        }

        get<ResourceAccessInteractor>().stub {
            on { canCreateResource(anyOrNull()) }.doReturn(true)
            on { canShareResource() }.doReturn(true)
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
    fun `should show error snackbar when profile refresh fails on init`() =
        runTest {
            val errorMessage = "profile fetch failed"
            get<UserProfileInteractor>().stub {
                on { fetchAndUpdateUserProfile() } doReturn
                    UserProfileInteractor.Output.Failure(DomainResult.Incomplete.Error(UNKNOWN, errorMessage))
            }

            viewModel = get()

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(PROFILE_FETCH_FAILURE)
                assertThat(effect.message).isEqualTo(errorMessage)
            }
        }

    @Test
    fun `should update search state when search query changes`() =
        runTest {
            val mockHomeData = mockResourcesData()
            whenever(get<HomeDataProvider>().provideData(any(), any(), any(), any())).thenReturn(mockHomeData)

            viewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().isSearching).isFalse()

                viewModel.onIntent(Search("test query"))

                val typedState = awaitItem()
                assertThat(typedState.searchQuery).isEqualTo("test query")
                assertThat(typedState.searchInputEndIconMode).isEqualTo(CLEAR)
                assertThat(typedState.isSearching).isTrue()

                advanceTimeBy(HomeViewModel.SEARCH_DEBOUNCE + 1.milliseconds)

                val appliedState = awaitItem()
                assertThat(appliedState.homeData).isEqualTo(mockHomeData)
                assertThat(appliedState.isSearching).isFalse()
            }
        }

    @Test
    fun `should apply the search query only once when typing multiple characters quickly`() =
        runTest {
            viewModel = get()
            clearInvocations(get<HomeDataProvider>())

            viewModel.onIntent(Search("t"))
            viewModel.onIntent(Search("te"))
            viewModel.onIntent(Search("tes"))
            advanceUntilIdle()

            verify(get<HomeDataProvider>()).provideData(eq("tes"), any(), any(), any())
        }

    @Test
    fun `should not re-run the search when the query text did not change`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Search("test query"))
            advanceUntilIdle()
            clearInvocations(get<HomeDataProvider>())

            viewModel.onIntent(Search("test query"))
            advanceUntilIdle()

            verify(get<HomeDataProvider>(), never()).provideData(any(), any(), any(), any())
        }

    @Test
    fun `should clear search and reset icon mode when search cleared`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Search("test query"))
            advanceUntilIdle()

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
                dataRefreshFlow.startTracking(isUserInitiated = true)
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
    fun `should show create button from local data without refresh in autofill context`() =
        runTest {
            mockHomeData()
            mockCanCreateResource(true)

            viewModel = get()

            viewModel.viewState.test {
                viewModel.onIntent(Initialize(DoNotShow, NotLoaded, appContext = AppContext.AUTOFILL))

                var state = awaitItem()
                while (!state.canCreateResource) {
                    state = awaitItem()
                }
                assertThat(state.isRefreshing).isFalse()
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
                dataRefreshFlow.startTracking(isUserInitiated = true)
                val inProgress = awaitItem()
                assertThat(inProgress.isRefreshing).isTrue()
                assertThat(inProgress.canCreateResource).isFalse()

                dataRefreshFlow.updateStatus(FinishedWithFailure)
                val finished = awaitItem()
                assertThat(finished.isRefreshing).isFalse()
                // whether a resource can be created is decided by the local replica, so a refresh that could not
                // reach the server must not take the create button away
                assertThat(finished.canCreateResource).isTrue()

                viewModel.sideEffect.test {
                    val effect = awaitItem()
                    assertIs<ShowErrorSnackbar>(effect)
                    assertThat(effect.type).isEqualTo(FAILED_TO_REFRESH_DATA)
                }
            }
        }

    @Test
    fun `should stay silent when a background refresh fails`() =
        runTest {
            val dataRefreshFlow: DataRefreshTrackingFlow = get()
            mockHomeData()
            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, null))

            viewModel.sideEffect.test {
                dataRefreshFlow.startTracking(isUserInitiated = false)
                // no spinner: the list is served from the local replica, so there is nothing to wait for
                assertThat(viewModel.viewState.value.isRefreshing).isFalse()

                dataRefreshFlow.updateStatus(FinishedWithFailure)
                assertThat(viewModel.viewState.value.isRefreshing).isFalse()

                // and no error snackbar: an unreachable server only means the data is less fresh
                expectNoEvents()
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
    fun `should not regenerate homeData on refresh complete`() =
        runTest {
            mockHomeData()
            val dataRefreshFlow: DataRefreshTrackingFlow = get()
            val provider: HomeDataProvider = get()

            viewModel = get()
            viewModel.onIntent(Initialize(DoNotShow, NotLoaded))
            advanceUntilIdle()

            clearInvocations(provider)

            dataRefreshFlow.updateStatus(InProgress(progress = 0f))
            dataRefreshFlow.updateStatus(FinishedWithSuccess)
            advanceUntilIdle()

            verify(provider, never()).provideData(any(), any(), any(), any())
        }

    private fun mockCanCreateResource(canCreate: Boolean) {
        get<ResourceAccessInteractor>().stub {
            on { canCreateResource(anyOrNull()) }.doReturn(canCreate)
        }
    }

    private fun mockHomeData() {
        val homeData = mockResourcesData()
        get<HomeDataProvider>().stub {
            on {
                provideData(
                    any(),
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
    ) = ResourceUiModel(
        resourceId = id,
        resourceTypeId = "resTypeId",
        slug = "password-and-description",
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
