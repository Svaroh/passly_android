package net.svaroh.passly.feature.otp.scanotp.scanotpsuccess

import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.ResourceUiModel

sealed interface ScanOtpSuccessIntent {
    data object CreateStandaloneOtpClick : ScanOtpSuccessIntent

    data object LinkToResourceClick : ScanOtpSuccessIntent

    data class LinkedResourceReceived(
        val resource: ResourceUiModel,
    ) : ScanOtpSuccessIntent

    data class ConfirmedPermissionsResult(
        val permissions: List<PermissionModelUi>,
    ) : ScanOtpSuccessIntent

    data object TrustNewMetadataKey : ScanOtpSuccessIntent

    data object TrustedMetadataKeyDeleted : ScanOtpSuccessIntent

    data object DismissNewMetadataTrustDialog : ScanOtpSuccessIntent

    data object DismissTrustedMetadataKeyDeletedDialog : ScanOtpSuccessIntent
}
