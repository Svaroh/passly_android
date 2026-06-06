package net.svaroh.passly.feature.otp.scanotp.scanotpsuccess

import net.svaroh.passly.ui.NewMetadataKeyToTrustModel
import net.svaroh.passly.ui.TrustedKeyDeletedModel

data class ScanOtpSuccessState(
    val showProgress: Boolean = false,
    val metadataKeyToTrust: NewMetadataKeyToTrustModel? = null,
    val metadataKeyDeleted: TrustedKeyDeletedModel? = null,
    val showNewMetadataTrustDialog: Boolean = false,
    val showTrustedMetadataKeyDeletedDialog: Boolean = false,
)
