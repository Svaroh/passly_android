package net.svaroh.passly.feature.otp.scanotp.compose

import net.svaroh.passly.ui.OtpParseResult

internal sealed interface ScanOtpSideEffect {
    data object RequestCameraPermission : ScanOtpSideEffect

    data class NavigateToSuccess(
        val totpQr: OtpParseResult.OtpQr.TotpQr,
    ) : ScanOtpSideEffect

    data class SetResultAndNavigateBack(
        val totpQr: OtpParseResult.OtpQr.TotpQr,
    ) : ScanOtpSideEffect

    data object SetManualCreationResultAndNavigateBack : ScanOtpSideEffect

    data object NavigateToAppSettings : ScanOtpSideEffect

    data object NavigateBack : ScanOtpSideEffect
}
