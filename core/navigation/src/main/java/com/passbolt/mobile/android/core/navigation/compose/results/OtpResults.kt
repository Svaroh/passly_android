package net.svaroh.passly.core.navigation.compose.results

import net.svaroh.passly.ui.OtpParseResult
import net.svaroh.passly.ui.ResourceModel

data class OtpScanCompleteResult(
    val otpCreated: Boolean,
    val otpManualCreationChosen: Boolean,
)

data class ResourceFormCompleteResult(
    val resourceCreated: Boolean,
    val resourceEdited: Boolean,
    val resourceName: String?,
)

data class ResourcePickerResultEvent(
    val pickAction: String,
    val resource: ResourceModel,
)

data class ScanOtpResultEvent(
    val isManualCreationChosen: Boolean,
    val scannedTotp: OtpParseResult.OtpQr.TotpQr?,
)
