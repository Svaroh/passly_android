package net.svaroh.passly.feature.otp.scanotp.compose

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.qrscan.CameraInformationProvider
import net.svaroh.passly.core.qrscan.analyzer.BarcodeScanResult
import net.svaroh.passly.feature.otp.scanotp.ScanOtpMode
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.CreateTotpManually
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.DismissCameraPermissionRequiredDialog
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.DismissCameraRequiredDialog
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.GoBack
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.GoToSettings
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.GrantCameraPermission
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.Initialize
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.RejectCameraPermission
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpIntent.StartCameraError
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateBack
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateToAppSettings
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.NavigateToSuccess
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.RequestCameraPermission
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.SetManualCreationResultAndNavigateBack
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpSideEffect.SetResultAndNavigateBack
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpState.TooltipMessage
import net.svaroh.passly.feature.otp.scanotp.parser.OtpQrParser
import net.svaroh.passly.ui.OtpParseResult
import timber.log.Timber

internal class ScanOtpViewModel(
    private val otpQrParser: OtpQrParser,
    private val cameraInformationProvider: CameraInformationProvider,
) : SideEffectViewModel<ScanOtpState, ScanOtpSideEffect>(ScanOtpState()) {
    private var qrScanningJob: Job? = null
    private lateinit var barcodeScanFlow: StateFlow<BarcodeScanResult>

    fun onIntent(intent: ScanOtpIntent) {
        when (intent) {
            is Initialize -> initialize(intent)
            is StartCameraError -> {
                Timber.e(intent.exception)
                updateViewState { copy(tooltipMessage = TooltipMessage.CAMERA_ERROR) }
            }
            GrantCameraPermission -> startQrScanning()
            RejectCameraPermission -> updateViewState { copy(showCameraPermissionRequiredDialog = true) }
            DismissCameraRequiredDialog -> updateViewState { copy(showCameraRequiredDialog = false) }
            DismissCameraPermissionRequiredDialog -> updateViewState { copy(showCameraPermissionRequiredDialog = false) }
            CreateTotpManually -> emitSideEffect(SetManualCreationResultAndNavigateBack)
            GoToSettings -> emitSideEffect(NavigateToAppSettings)
            GoBack -> emitSideEffect(NavigateBack)
        }
    }

    private fun initialize(intent: Initialize) {
        barcodeScanFlow = intent.barcodeScanFlow
        updateViewState { copy(mode = intent.mode) }
        startQrScanning()
    }

    private fun startQrScanning() {
        when {
            !cameraInformationProvider.isCameraAvailable() ->
                updateViewState { copy(showCameraRequiredDialog = true) }
            !cameraInformationProvider.isCameraPermissionGranted() ->
                emitSideEffect(RequestCameraPermission)
            else -> initQrScanning()
        }
    }

    private fun initQrScanning() {
        qrScanningJob?.cancel()
        qrScanningJob =
            viewModelScope.launch {
                launch { otpQrParser.startParsing(barcodeScanFlow) }
                launch { otpQrParser.parseResultFlow.collect { processParseResult(it) } }
            }
    }

    private fun processParseResult(parserResult: OtpParseResult) {
        when (parserResult) {
            is OtpParseResult.Failure -> showScanError(parserResult.exception)
            is OtpParseResult.OtpQr -> processOtpQr(parserResult)
            is OtpParseResult.UserResolvableError -> processUserResolvableError(parserResult)
            is OtpParseResult.ScanFailure -> showScanError(parserResult.exception)
            is OtpParseResult.IncompleteOtpParameters.IncompleteHotpParametrs -> {
                // HOTP is not supported yet
            }
            is OtpParseResult.IncompleteOtpParameters.IncompleteTotpParameters -> {
                Timber.d("Incomplete TOTP parameters")
            }
        }
    }

    private fun showScanError(exception: Throwable?) {
        exception?.let { Timber.e(it) }
        updateViewState {
            copy(
                tooltipMessage = TooltipMessage.SCAN_ERROR,
                scanErrorMessage = exception?.message,
            )
        }
    }

    private fun processOtpQr(otpQr: OtpParseResult.OtpQr) {
        when (otpQr) {
            is OtpParseResult.OtpQr.TotpQr -> {
                when (viewState.value.mode) {
                    ScanOtpMode.SCAN_FOR_RESULT -> emitSideEffect(SetResultAndNavigateBack(otpQr))
                    ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN -> emitSideEffect(NavigateToSuccess(otpQr))
                }
            }
            is OtpParseResult.OtpQr.HotpQr -> {
                // HOTP is not supported yet
            }
        }
    }

    private fun processUserResolvableError(error: OtpParseResult.UserResolvableError) {
        when (error.errorType) {
            OtpParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES ->
                updateViewState { copy(tooltipMessage = TooltipMessage.MULTIPLE_BARCODES) }
            OtpParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE ->
                updateViewState { copy(tooltipMessage = TooltipMessage.CENTER_CAMERA_ON_BARCODE) }
            OtpParseResult.UserResolvableError.ErrorType.NOT_A_OTP_QR ->
                updateViewState { copy(tooltipMessage = TooltipMessage.NOT_A_OTP_QR) }
        }
    }
}
