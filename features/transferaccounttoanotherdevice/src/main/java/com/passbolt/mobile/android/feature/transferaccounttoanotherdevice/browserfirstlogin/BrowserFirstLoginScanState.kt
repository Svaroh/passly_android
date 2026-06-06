package net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin

internal data class BrowserFirstLoginScanState(
    val showProgress: Boolean = false,
    val tooltipMessage: TooltipMessage = TooltipMessage.CENTER_CAMERA_ON_QR,
) {
    enum class TooltipMessage {
        CENTER_CAMERA_ON_QR,
        PROCESSING,
        CAMERA_ERROR,
        SCAN_ERROR,
    }
}
