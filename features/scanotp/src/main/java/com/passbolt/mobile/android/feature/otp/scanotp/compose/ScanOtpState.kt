package net.svaroh.passly.feature.otp.scanotp.compose

import net.svaroh.passly.feature.otp.scanotp.ScanOtpMode
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpState.TooltipMessage.CENTER_CAMERA_ON_BARCODE

data class ScanOtpState(
    val mode: ScanOtpMode = ScanOtpMode.SCAN_FOR_RESULT,
    val tooltipMessage: TooltipMessage = CENTER_CAMERA_ON_BARCODE,
    val scanErrorMessage: String? = null,
    val showCameraRequiredDialog: Boolean = false,
    val showCameraPermissionRequiredDialog: Boolean = false,
) {
    enum class TooltipMessage {
        CENTER_CAMERA_ON_BARCODE,
        MULTIPLE_BARCODES,
        NOT_A_OTP_QR,
        CAMERA_ERROR,
        SCAN_ERROR,
    }
}
