package net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin

import android.content.Context
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginScanState.TooltipMessage
import net.svaroh.passly.core.localization.R as LocalizationR

internal fun getTooltipMessage(
    context: Context,
    message: TooltipMessage,
) = when (message) {
    TooltipMessage.CENTER_CAMERA_ON_QR -> context.getString(LocalizationR.string.browser_first_login_scan_tooltip)
    TooltipMessage.PROCESSING -> context.getString(LocalizationR.string.browser_first_login_processing)
    TooltipMessage.CAMERA_ERROR -> context.getString(LocalizationR.string.browser_first_login_camera_error)
    TooltipMessage.SCAN_ERROR -> context.getString(LocalizationR.string.browser_first_login_scan_error)
}
