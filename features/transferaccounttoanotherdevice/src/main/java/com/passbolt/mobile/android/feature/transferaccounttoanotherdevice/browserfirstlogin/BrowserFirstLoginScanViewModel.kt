package net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.qrscan.analyzer.BarcodeScanResult
import net.svaroh.passly.dto.response.qrcode.BrowserFirstLoginPageDto
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanIntent.GoBack
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanIntent.StartCameraError
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanSideEffect.NavigateBack
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanSideEffect.NavigateToResult
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanState.TooltipMessage.CAMERA_ERROR
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanState.TooltipMessage.CENTER_CAMERA_ON_QR
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanState.TooltipMessage.PROCESSING
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanState.TooltipMessage.SCAN_ERROR
import net.svaroh.passly.feature.transferaccounttoanotherdevice.usecase.CompleteBrowserFirstLoginUseCase
import net.svaroh.passly.ui.TransferAccountStatusType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

internal class BrowserFirstLoginScanViewModel(
    private val completeBrowserFirstLoginUseCase: CompleteBrowserFirstLoginUseCase,
    private val browserFirstLoginQrParser: BrowserFirstLoginQrParser,
) : SideEffectViewModel<BrowserFirstLoginScanState, BrowserFirstLoginScanSideEffect>(BrowserFirstLoginScanState()) {
    private var scanJob: Job? = null
    private var requestProcessing = false

    fun onIntent(intent: BrowserFirstLoginScanIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            is StartCameraError -> {
                Timber.e(intent.exception)
                updateViewState { copy(tooltipMessage = CAMERA_ERROR) }
            }
            is BrowserFirstLoginScanIntent.Initialize -> initialize(intent.barcodeScanFlow)
        }
    }

    private fun initialize(scanFlow: StateFlow<BarcodeScanResult>) {
        scanJob?.cancel()
        scanJob =
            viewModelScope.launch {
                scanFlow.collect { scanResult ->
                    if (!requestProcessing) {
                        processScanResult(scanResult)
                    }
                }
            }
    }

    private fun processScanResult(scanResult: BarcodeScanResult) {
        when (scanResult) {
            is BarcodeScanResult.Failure -> {
                Timber.e(scanResult.throwable)
                updateViewState { copy(tooltipMessage = SCAN_ERROR) }
            }
            BarcodeScanResult.MultipleBarcodes,
            BarcodeScanResult.NoBarcodeInRange -> updateViewState { copy(tooltipMessage = CENTER_CAMERA_ON_QR) }
            is BarcodeScanResult.SingleBarcode -> {
                val page = browserFirstLoginQrParser.parse(scanResult.data)
                if (page != null) {
                    Timber.i("[BrowserFirstLogin] Parsed QR from scanner")
                    completeBrowserFirstLogin(page)
                } else {
                    Timber.e("[BrowserFirstLogin] Scanner QR is not a browser first-login QR")
                }
            }
        }
    }

    private fun completeBrowserFirstLogin(page: BrowserFirstLoginPageDto) {
        requestProcessing = true
        updateViewState { copy(showProgress = true, tooltipMessage = PROCESSING) }
        viewModelScope.launch {
            Timber.i("[BrowserFirstLogin] Completing scanner QR")
            when (
                val result =
                    completeBrowserFirstLoginUseCase.execute(CompleteBrowserFirstLoginUseCase.Input(page))
            ) {
                CompleteBrowserFirstLoginUseCase.Output.Success ->
                    emitSideEffect(NavigateToResult(TransferAccountStatusType.SUCCESS))
                is CompleteBrowserFirstLoginUseCase.Output.DomainMismatch -> {
                    emitSideEffect(
                        ShowErrorSnackbar(
                            "QR code domain ${result.qrDomain} does not match this account ${result.accountDomain}.",
                        ),
                    )
                    requestProcessing = false
                    updateViewState { copy(showProgress = false, tooltipMessage = CENTER_CAMERA_ON_QR) }
                }
                is CompleteBrowserFirstLoginUseCase.Output.Failure -> {
                    emitSideEffect(ShowErrorSnackbar(result.message ?: "Could not complete browser login."))
                    requestProcessing = false
                    updateViewState { copy(showProgress = false, tooltipMessage = CENTER_CAMERA_ON_QR) }
                }
            }
        }
    }

    override fun onCleared() {
        scanJob?.cancel()
        super.onCleared()
    }
}
