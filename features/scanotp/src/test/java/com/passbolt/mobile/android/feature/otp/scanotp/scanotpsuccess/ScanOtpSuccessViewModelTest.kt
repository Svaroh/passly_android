package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.domain.resources.actions.ResourceCreateActionResult
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.domain.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.ConfirmedPermissionsResult
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.CreateStandaloneOtpClick
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkToResourceClick
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkedResourceReceived
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.NavigateToConfirmPermissions
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.NavigateToOtpList
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.NavigateToResourcePicker
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.ShowToast
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ScanOtpSuccessViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testScanOtpSuccessModule)
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockEditPermissionsConfirmationInteractor.stub {
            on { shouldConfirmPermissions(any()) } doReturn false
        }
        mockCreatePermissionsConfirmationInteractor.stub {
            on { shouldConfirmPermissions(anyOrNull()) } doReturn false
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create standalone totp should create totp and navigate to otp list`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.doReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        ContentType.V5TotpStandalone,
                        MetadataTypeModel.V5,
                    ),
                )
            }
            val mockResourceId = UUID.randomUUID()
            val mockResourceName = "mockResourceName"
            mockResourceCreateActionsInteractor.stub {
                on {
                    createGenericResource(any(), anyOrNull(), any(), any())
                }.doReturn(flowOf(ResourceCreateActionResult.Success(mockResourceId.toString(), mockResourceName)))
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateStandaloneOtpClick)

                val sideEffect = awaitItem()
                assertIs<NavigateToOtpList>(sideEffect)
                assertThat(sideEffect.totp).isEqualTo(mockScannedTotp)
                assertThat(sideEffect.otpCreated).isTrue()
                assertThat(sideEffect.resourceId).isEqualTo(mockResourceId.toString())
            }
        }

    @Test
    fun `create standalone totp should show and hide progress`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.doReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        ContentType.V5TotpStandalone,
                        MetadataTypeModel.V5,
                    ),
                )
            }
            val mockResourceId = UUID.randomUUID()
            mockResourceCreateActionsInteractor.stub {
                on {
                    createGenericResource(any(), anyOrNull(), any(), any())
                }.doReturn(flowOf(ResourceCreateActionResult.Success(mockResourceId.toString(), "name")))
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState.showProgress).isFalse()

                viewModel.onIntent(CreateStandaloneOtpClick)

                val progressShown = awaitItem()
                assertThat(progressShown.showProgress).isTrue()

                val progressHidden = awaitItem()
                assertThat(progressHidden.showProgress).isFalse()
            }
        }

    @Test
    fun `link to resource click should navigate to resource picker`() =
        runTest {
            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.sideEffect.test {
                viewModel.onIntent(LinkToResourceClick)

                val sideEffect = awaitItem()
                assertIs<NavigateToResourcePicker>(sideEffect)
                assertThat(sideEffect.suggestedUri).isEqualTo(mockScannedTotp.issuer)
            }
        }

    @Test
    fun `link totp to linked resource should update and navigate to otp list`() =
        runTest {
            val mockResourceId = UUID.randomUUID()
            val mockResourceTypeId = UUID.randomUUID()
            val mockResourceName = "mockResourceName"
            val mockMetadataJsonModel =
                mock<MetadataJsonModel> {
                    on { name } doReturn mockResourceName
                }
            val mockLinkResourceModel =
                mock<ResourceUiModel> {
                    on { resourceId } doReturn mockResourceId.toString()
                    on { resourceTypeId } doReturn mockResourceTypeId.toString()
                    on { metadataJsonModel } doReturn mockMetadataJsonModel
                }

            mockResourceUpdateActionsInteractor.stub {
                on {
                    updateGenericResource(eq(UpdateAction.ADD_TOTP), any(), any())
                }.doReturn(flowOf(ResourceUpdateActionResult.Success(mockResourceId.toString(), mockResourceName)))
            }
            mockIdToSlugMappingProvider.stub {
                on { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(mockResourceTypeId to ContentType.V5DefaultWithTotp.slug),
                )
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    LinkedResourceReceived(mockLinkResourceModel),
                )

                val sideEffect = awaitItem()
                assertIs<NavigateToOtpList>(sideEffect)
                assertThat(sideEffect.totp).isEqualTo(mockScannedTotp)
                assertThat(sideEffect.otpCreated).isTrue()
                assertThat(sideEffect.resourceId).isEqualTo(mockResourceId.toString())
            }
        }

    @Test
    fun `link totp to a shared resource should navigate to permissions confirmation`() =
        runTest {
            val mockResourceId = UUID.randomUUID()
            val mockLinkResourceModel =
                mock<ResourceUiModel> {
                    on { resourceId } doReturn mockResourceId.toString()
                }
            mockEditPermissionsConfirmationInteractor.stub {
                on { shouldConfirmPermissions(mockResourceId.toString()) } doReturn true
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.sideEffect.test {
                viewModel.onIntent(LinkedResourceReceived(mockLinkResourceModel))

                assertThat(awaitItem())
                    .isEqualTo(NavigateToConfirmPermissions(ConfirmPermissionsMode.Edit(mockResourceId.toString())))
            }
            verifyNoInteractions(mockResourceUpdateActionsInteractor)
        }

    @Test
    fun `confirmed permissions should link totp with the confirmed list`() =
        runTest {
            val mockResourceId = UUID.randomUUID()
            val mockResourceTypeId = UUID.randomUUID()
            val mockLinkResourceModel =
                mock<ResourceUiModel> {
                    on { resourceId } doReturn mockResourceId.toString()
                    on { resourceTypeId } doReturn mockResourceTypeId.toString()
                }
            mockEditPermissionsConfirmationInteractor.stub {
                on { shouldConfirmPermissions(mockResourceId.toString()) } doReturn true
            }
            mockIdToSlugMappingProvider.stub {
                on { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(mockResourceTypeId to ContentType.V5DefaultWithTotp.slug),
                )
            }
            mockResourceUpdateActionsInteractor.stub {
                on {
                    updateGenericResourceWithConfirmedPermissions(eq(UpdateAction.ADD_TOTP), any(), any(), any())
                }.doReturn(flowOf(ResourceUpdateActionResult.Success(mockResourceId.toString(), "name")))
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }
            viewModel.sideEffect.test {
                viewModel.onIntent(LinkedResourceReceived(mockLinkResourceModel))
                awaitItem()

                viewModel.onIntent(ConfirmedPermissionsResult(emptyList()))

                assertIs<NavigateToOtpList>(awaitItem())
            }
            verify(mockResourceUpdateActionsInteractor)
                .updateGenericResourceWithConfirmedPermissions(eq(UpdateAction.ADD_TOTP), any(), any(), any())
        }

    @Test
    fun `link totp to password resource should update and navigate to otp list`() =
        runTest {
            val mockResourceId = UUID.randomUUID()
            val mockResourceTypeId = UUID.randomUUID()
            val mockResourceName = "mockResourceName"
            val mockMetadataJsonModel =
                mock<MetadataJsonModel> {
                    on { name } doReturn mockResourceName
                }
            val mockLinkResourceModel =
                mock<ResourceUiModel> {
                    on { resourceId } doReturn mockResourceId.toString()
                    on { resourceTypeId } doReturn mockResourceTypeId.toString()
                    on { metadataJsonModel } doReturn mockMetadataJsonModel
                }

            mockResourceUpdateActionsInteractor.stub {
                on {
                    updateGenericResource(eq(UpdateAction.ADD_TOTP), any(), any())
                }.doReturn(flowOf(ResourceUpdateActionResult.Success(mockResourceId.toString(), mockResourceName)))
            }
            mockIdToSlugMappingProvider.stub {
                on { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(mockResourceTypeId to ContentType.V5Default.slug),
                )
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    LinkedResourceReceived(mockLinkResourceModel),
                )

                val sideEffect = awaitItem()
                assertIs<NavigateToOtpList>(sideEffect)
                assertThat(sideEffect.totp).isEqualTo(mockScannedTotp)
                assertThat(sideEffect.otpCreated).isTrue()
                assertThat(sideEffect.resourceId).isEqualTo(mockResourceId.toString())
            }
        }

    @Test
    fun `create standalone totp in a shared folder should navigate to permissions confirmation`() =
        runTest {
            mockCreatePermissionsConfirmationInteractor.stub {
                on { shouldConfirmPermissions(PARENT_FOLDER_ID) } doReturn true
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, PARENT_FOLDER_ID) }

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateStandaloneOtpClick)

                assertThat(awaitItem())
                    .isEqualTo(NavigateToConfirmPermissions(ConfirmPermissionsMode.Create(PARENT_FOLDER_ID)))
            }
        }

    @Test
    fun `confirmed permissions should create standalone totp with the confirmed list`() =
        runTest {
            mockCreatePermissionsConfirmationInteractor.stub {
                on { shouldConfirmPermissions(PARENT_FOLDER_ID) } doReturn true
            }
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.doReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        ContentType.V5TotpStandalone,
                        MetadataTypeModel.V5,
                    ),
                )
            }
            val mockResourceId = UUID.randomUUID()
            mockResourceCreateActionsInteractor.stub {
                on {
                    createGenericResourceWithConfirmedPermissions(any(), anyOrNull(), any(), any(), any())
                }.doReturn(flowOf(ResourceCreateActionResult.Success(mockResourceId.toString(), "name")))
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, PARENT_FOLDER_ID) }

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateStandaloneOtpClick)
                awaitItem()

                viewModel.onIntent(ConfirmedPermissionsResult(emptyList()))

                val sideEffect = awaitItem()
                assertIs<NavigateToOtpList>(sideEffect)
                assertThat(sideEffect.otpCreated).isTrue()
                assertThat(sideEffect.resourceId).isEqualTo(mockResourceId.toString())
            }
            verify(mockResourceCreateActionsInteractor)
                .createGenericResourceWithConfirmedPermissions(any(), anyOrNull(), any(), any(), any())
        }

    @Test
    fun `permissions drift during confirmed standalone totp creation should inform and navigate to otp list`() =
        runTest {
            mockCreatePermissionsConfirmationInteractor.stub {
                on { shouldConfirmPermissions(PARENT_FOLDER_ID) } doReturn true
            }
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.doReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        ContentType.V5TotpStandalone,
                        MetadataTypeModel.V5,
                    ),
                )
            }
            mockResourceCreateActionsInteractor.stub {
                on {
                    createGenericResourceWithConfirmedPermissions(any(), anyOrNull(), any(), any(), any())
                }.doReturn(flowOf(ResourceCreateActionResult.PermissionsDrifted))
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, PARENT_FOLDER_ID) }

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateStandaloneOtpClick)
                awaitItem()

                viewModel.onIntent(ConfirmedPermissionsResult(emptyList()))

                assertThat(awaitItem()).isEqualTo(ShowToast(ToastType.OTP_CREATED_PERMISSIONS_CHANGED))
                val sideEffect = awaitItem()
                assertIs<NavigateToOtpList>(sideEffect)
                assertThat(sideEffect.otpCreated).isTrue()
            }
        }

    private companion object {
        private const val PARENT_FOLDER_ID = "parent-folder-id"

        val mockScannedTotp =
            OtpParseResult.OtpQr.TotpQr(
                label = "label",
                secret = "secret",
                issuer = "issuer",
                algorithm = OtpParseResult.OtpQr.Algorithm.SHA512,
                digits = 6,
                period = 30,
            )
    }
}
