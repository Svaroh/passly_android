package net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin

import net.svaroh.passly.core.qrscan.analyzer.BarcodeScanResult
import kotlinx.coroutines.flow.StateFlow

internal sealed interface BrowserFirstLoginScanIntent {
    data class Initialize(
        val barcodeScanFlow: StateFlow<BarcodeScanResult>,
    ) : BrowserFirstLoginScanIntent

    data object GoBack : BrowserFirstLoginScanIntent

    data class StartCameraError(
        val exception: Exception,
    ) : BrowserFirstLoginScanIntent
}
