package net.svaroh.passly.permissions.permissions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.commontest.session.validSessionTestModule
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderDetailsUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import net.svaroh.passly.domain.metadata.interactor.ResourceAccessInteractor
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.jsonmodel.JSON_MODEL_GSON
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathJsonPathOps
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathsOps
import net.svaroh.passly.permissions.common.PermissionsListMapper
import net.svaroh.passly.permissions.permissions.PermissionsIntent.MainButtonIntent
import net.svaroh.passly.ui.GroupModel
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionsItem
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.ResourceUiModel
import net.svaroh.passly.ui.UserWithAvatar
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.ZonedDateTime
import java.util.EnumSet
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsViewModelTest : KoinTest {
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
                    single { mock<ResourceAccessInteractor>() }
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    singleOf(::SessionRefreshTrackingFlow)
                    singleOf(::DataRefreshTrackingFlow)
                    factory { PermissionModelUiComparator() }
                    factory { PermissionsListMapper(get()) }
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
                    factory { params ->
                        PermissionsViewModel(
                            permissionsItem = params.get(),
                            id = params.get(),
                            mode = params.get(),
                            getLocalResourcePermissionsUseCase = get(),
                            getLocalResourceUseCase = get(),
                            getLocalFolderPermissionsUseCase = get(),
                            getLocalFolderUseCase = get(),
                            permissionsListMapper = get(),
                            resourceAccessInteractor = get(),
                            dataRefreshTrackingFlow = get(),
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

        get<GetLocalResourcePermissionsUseCase>().stub {
            on { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourcePermissionsUseCase.Output(GROUP_PERMISSIONS + USER_PERMISSIONS))
        }
        get<GetLocalResourceUseCase>().stub {
            on { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) }
                .doReturn(GetLocalResourceUseCase.Output(RESOURCE_MODEL))
        }
        get<ResourceAccessInteractor>().stub {
            on { canShareResource() } doReturn true
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `edit button should be shown in view mode and if owner`() =
        runTest {
            get<GetLocalResourceUseCase>().stub {
                on { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) }
                    .doReturn(GetLocalResourceUseCase.Output(RESOURCE_MODEL.copy(permission = ResourcePermission.OWNER)))
            }

            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showEditButton).isTrue()
            }
        }

    @Test
    fun `error should be shown when sharing not possible`() =
        runTest {
            get<ResourceAccessInteractor>().stub {
                on { canShareResource() } doReturn false
            }
            get<GetLocalResourceUseCase>().stub {
                on { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) }
                    .doReturn(GetLocalResourceUseCase.Output(RESOURCE_MODEL.copy(permission = ResourcePermission.OWNER)))
            }

            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.sideEffect.test {
                viewModel.onIntent(MainButtonIntent)
                val effect = awaitItem()
                assertIs<PermissionsSideEffect.ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(SnackbarErrorType.CANNOT_SHARE_RESOURCE)
            }
        }

    @Test
    fun `edit permissions should open the share confirmation`() =
        runTest {
            get<GetLocalResourceUseCase>().stub {
                on { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) }
                    .doReturn(GetLocalResourceUseCase.Output(RESOURCE_MODEL.copy(permission = ResourcePermission.OWNER)))
            }

            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.sideEffect.test {
                viewModel.onIntent(MainButtonIntent)

                assertThat(awaitItem()).isEqualTo(PermissionsSideEffect.NavigateToShareResource(RESOURCE_ID))
            }
        }

    @Test
    fun `empty state should be shown when there are no permissions`() =
        runTest {
            get<GetLocalResourcePermissionsUseCase>().stub {
                on { execute(GetLocalResourcePermissionsUseCase.Input(RESOURCE_ID)) }
                    .doReturn(GetLocalResourcePermissionsUseCase.Output(emptyList()))
            }

            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showEmptyState).isTrue()
            }
        }

    @Test
    fun `existing permissions should be shown initially`() =
        runTest {
            val viewModel =
                get<PermissionsViewModel>(
                    parameters = { parametersOf(RESOURCE_ID, PermissionsMode.VIEW, PermissionsItem.RESOURCE) },
                )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.permissions).containsExactlyElementsIn(GROUP_PERMISSIONS + USER_PERMISSIONS)
            }
        }

    private companion object {
        private const val RESOURCE_ID = "resid"
        private val RESOURCE_TYPE_ID = UUID.randomUUID()
        private val FOLDER_ID = UUID.randomUUID()
        private val RESOURCE_MODEL by lazy {
            ResourceUiModel(
                resourceId = RESOURCE_ID,
                resourceTypeId = RESOURCE_TYPE_ID.toString(),
                slug = "password-and-description",
                folderId = FOLDER_ID.toString(),
                permission = ResourcePermission.READ,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJsonModel =
                    MetadataJsonModel(
                        """{"name":"name","uri":"https://passbolt.com","username":"user","description":"desc"}""",
                    ),
                metadataKeyId = null,
                metadataKeyType = null,
            )
        }
        private val GROUP_PERMISSIONS =
            listOf(
                PermissionModelUi.GroupPermissionModel(
                    permission = ResourcePermission.READ,
                    permissionId = "groupPermId",
                    group = GroupModel(groupId = "groupId", groupName = "groupname"),
                ),
            )
        private val USER_PERMISSIONS =
            listOf(
                PermissionModelUi.UserPermissionModel(
                    permission = ResourcePermission.READ,
                    permissionId = "userPermId",
                    user =
                        UserWithAvatar(
                            userId = "userId",
                            firstName = "first",
                            lastName = "last",
                            userName = "userName",
                            isDisabled = false,
                            avatarUrl = "avatarUrl",
                        ),
                ),
            )
    }
}
