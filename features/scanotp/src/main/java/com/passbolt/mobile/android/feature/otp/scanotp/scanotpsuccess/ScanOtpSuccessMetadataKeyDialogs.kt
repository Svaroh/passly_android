package net.svaroh.passly.feature.otp.scanotp.scanotpsuccess

import androidx.compose.runtime.Composable
import net.svaroh.passly.feature.metadatakeytrust.NewMetadataKeyTrustDialog
import net.svaroh.passly.feature.metadatakeytrust.TrustedMetadataKeyDeletedDialog
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.DismissNewMetadataTrustDialog
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.DismissTrustedMetadataKeyDeletedDialog
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.TrustNewMetadataKey
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.TrustedMetadataKeyDeleted

@Composable
internal fun MetadataKeyDialogs(
    state: ScanOtpSuccessState,
    onIntent: (ScanOtpSuccessIntent) -> Unit,
) {
    if (state.showNewMetadataTrustDialog && state.metadataKeyToTrust != null) {
        NewMetadataKeyTrustDialog(
            newKeyToTrustModel = state.metadataKeyToTrust,
            onTrustClick = { onIntent(TrustNewMetadataKey) },
            onDismiss = { onIntent(DismissNewMetadataTrustDialog) },
        )
    }

    if (state.showTrustedMetadataKeyDeletedDialog && state.metadataKeyDeleted != null) {
        TrustedMetadataKeyDeletedDialog(
            trustedKeyDeletedModel = state.metadataKeyDeleted,
            onTrustClick = { onIntent(TrustedMetadataKeyDeleted) },
            onDismiss = { onIntent(DismissTrustedMetadataKeyDeletedDialog) },
        )
    }
}
