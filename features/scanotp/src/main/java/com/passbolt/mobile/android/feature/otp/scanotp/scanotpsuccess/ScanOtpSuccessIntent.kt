package net.svaroh.passly.feature.otp.scanotp.scanotpsuccess

import net.svaroh.passly.ui.ResourceModel

sealed interface ScanOtpSuccessIntent {
    data object CreateStandaloneOtpClick : ScanOtpSuccessIntent

    data object LinkToResourceClick : ScanOtpSuccessIntent

    data class LinkedResourceReceived(
        val resource: ResourceModel,
    ) : ScanOtpSuccessIntent

    data object TrustNewMetadataKey : ScanOtpSuccessIntent

    data object TrustedMetadataKeyDeleted : ScanOtpSuccessIntent

    data object DismissNewMetadataTrustDialog : ScanOtpSuccessIntent

    data object DismissTrustedMetadataKeyDeletedDialog : ScanOtpSuccessIntent
}
