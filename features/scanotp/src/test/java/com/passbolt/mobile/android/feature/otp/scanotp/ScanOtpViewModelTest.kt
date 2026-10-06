package net.svaroh.passly.feature.otp.scanotp

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.CreateTotpManually
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.GoToSettings
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.GrantCameraPermission
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.Initialize
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.RejectCameraPermission
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateToAppSettings
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.RequestCameraPermission
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.SetManualCreationResultAndNavigateBack
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.SetResultAndNavigateBack
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpState.TooltipMessage
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpViewModel
import net.svaroh.passly.ui.OtpParseResult
import net.svaroh.passly.ui.OtpParseResult.UserResolvableError
import net.svaroh.passly.ui.OtpParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES
import net.svaroh.passly.ui.OtpParseResult.UserResolvableError.ErrorType.NOT_A_OTP_QR
import net.svaroh.passly.ui.OtpParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ScanOtpViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(scanOtpTestModule)
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        qrParser.stub {
            on { startParsing(any()) }.then { }
            on { parseResultFlow }.doReturn(parseFlow)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `missing camera should show camera required dialog`() =
        runTest {
            whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(false)
            val viewModel = get<ScanOtpViewModel>()

            viewModel.onIntent(Initialize(scanningFlow, ScanOtpMode.SCAN_FOR_RESULT))

            viewModel.viewState.test {
                assertThat(awaitItem().showCameraRequiredDialog).isTrue()
            }
        }

    @Test
    fun `missing camera permission should request camera permission`() =
        runTest {
            whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
            whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(false)
            val viewModel = get<ScanOtpViewModel>()

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(scanningFlow, ScanOtpMode.SCAN_FOR_RESULT))

                val sideEffect = awaitItem()
                assertIs<RequestCameraPermission>(sideEffect)
            }
        }

    @Test
    fun `granting camera permission after initialize should start qr scanning`() =
        runTest {
            whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
            whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(false)
            val viewModel = get<ScanOtpViewModel>()

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(scanningFlow, ScanOtpMode.SCAN_FOR_RESULT))
                testDispatcher.scheduler.advanceUntilIdle()
                assertIs<RequestCameraPermission>(awaitItem())

                whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)
                viewModel.onIntent(GrantCameraPermission)

                parseFlow.emit(mockTotpQr)
                testDispatcher.scheduler.advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<SetResultAndNavigateBack>(sideEffect)
                assertThat(sideEffect.totpQr).isEqualTo(mockTotpQr)
            }
        }

    @Test
    fun `rejecting camera permission should show information dialog`() =
        runTest {
            val viewModel = get<ScanOtpViewModel>()

            viewModel.onIntent(RejectCameraPermission)

            viewModel.viewState.test {
                assertThat(awaitItem().showCameraPermissionRequiredDialog).isTrue()
            }
        }

    @Test
    fun `go to settings should emit navigate to app settings side effect`() =
        runTest {
            val viewModel = get<ScanOtpViewModel>()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToSettings)

                assertIs<NavigateToAppSettings>(awaitItem())
            }
        }

    @Test
    fun `create totp manually should emit manual creation side effect`() =
        runTest {
            val viewModel = get<ScanOtpViewModel>()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateTotpManually)

                assertIs<SetManualCreationResultAndNavigateBack>(awaitItem())
            }
        }

    @Test
    fun `view should show correct user resolvable error tooltips`() =
        runTest {
            whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
            whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)
            val viewModel = get<ScanOtpViewModel>()

            viewModel.onIntent(Initialize(scanningFlow, ScanOtpMode.SCAN_FOR_RESULT))
            testDispatcher.scheduler.advanceUntilIdle()

            parseFlow.emit(UserResolvableError(MULTIPLE_BARCODES))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.viewState.test {
                assertThat(awaitItem().tooltipMessage).isEqualTo(TooltipMessage.MULTIPLE_BARCODES)
            }

            parseFlow.emit(UserResolvableError(NOT_A_OTP_QR))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.viewState.test {
                assertThat(awaitItem().tooltipMessage).isEqualTo(TooltipMessage.NOT_A_OTP_QR)
            }

            parseFlow.emit(UserResolvableError(NO_BARCODES_IN_RANGE))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.viewState.test {
                assertThat(awaitItem().tooltipMessage).isEqualTo(TooltipMessage.CENTER_CAMERA_ON_BARCODE)
            }
        }

    @Test
    fun `successful scan in SCAN_FOR_RESULT mode should set result and navigate back`() =
        runTest {
            whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
            whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)
            val viewModel = get<ScanOtpViewModel>()

            val successfulResult = mockTotpQr

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(scanningFlow, ScanOtpMode.SCAN_FOR_RESULT))
                testDispatcher.scheduler.advanceUntilIdle()

                parseFlow.emit(successfulResult)
                testDispatcher.scheduler.advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<SetResultAndNavigateBack>(sideEffect)
                assertThat(sideEffect.totpQr).isEqualTo(successfulResult)
            }
        }

    @Test
    fun `successful scan in SCAN_WITH_SUCCESS_SCREEN mode should navigate to success`() =
        runTest {
            whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
            whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)
            val viewModel = get<ScanOtpViewModel>()

            val successfulResult = mockTotpQr

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(scanningFlow, ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN))
                testDispatcher.scheduler.advanceUntilIdle()

                parseFlow.emit(successfulResult)
                testDispatcher.scheduler.advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<ScanOtpSideEffect.NavigateToSuccess>(sideEffect)
                assertThat(sideEffect.totpQr).isEqualTo(successfulResult)
            }
        }

    @Test
    fun `scan failure should show scan error tooltip`() =
        runTest {
            whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
            whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)
            val viewModel = get<ScanOtpViewModel>()

            val errorMessage = "Exception occurred"

            viewModel.onIntent(Initialize(scanningFlow, ScanOtpMode.SCAN_FOR_RESULT))
            testDispatcher.scheduler.advanceUntilIdle()

            parseFlow.emit(OtpParseResult.Failure(RuntimeException(errorMessage)))
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.tooltipMessage).isEqualTo(TooltipMessage.SCAN_ERROR)
                assertThat(state.scanErrorMessage).isEqualTo(errorMessage)
            }
        }

    private companion object {
        val mockTotpQr =
            OtpParseResult.OtpQr.TotpQr(
                label = "label",
                secret = "secret",
                issuer = "issuer",
                algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
                digits = 6,
                period = 30,
            )
    }
}
