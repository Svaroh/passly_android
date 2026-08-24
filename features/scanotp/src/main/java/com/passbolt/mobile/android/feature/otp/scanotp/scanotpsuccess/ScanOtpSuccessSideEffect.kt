package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.passbolt.mobile.android.ui.ConfirmPermissionsMode
import com.passbolt.mobile.android.ui.OtpParseResult

internal sealed interface ScanOtpSuccessSideEffect {
    data class NavigateToOtpList(
        val totp: OtpParseResult.OtpQr.TotpQr,
        val otpCreated: Boolean,
        val resourceId: String,
    ) : ScanOtpSuccessSideEffect

    data class NavigateToResourcePicker(
        val suggestedUri: String?,
    ) : ScanOtpSuccessSideEffect

    data class NavigateToConfirmPermissions(
        val confirmMode: ConfirmPermissionsMode,
        val driftedEntityNames: List<String>? = null,
    ) : ScanOtpSuccessSideEffect

    data class ShowErrorSnackbar(
        val type: ErrorSnackbarType,
        val message: String? = null,
    ) : ScanOtpSuccessSideEffect

    data class ShowSuccessSnackbar(
        val type: SuccessSnackbarType,
    ) : ScanOtpSuccessSideEffect

    data class ShowToast(
        val type: ToastType,
    ) : ScanOtpSuccessSideEffect
}

internal enum class ErrorSnackbarType {
    GENERIC_ERROR,
    ENCRYPTION_ERROR,
    JSON_RESOURCE_SCHEMA_VALIDATION_ERROR,
    JSON_SECRET_SCHEMA_VALIDATION_ERROR,
    CANNOT_CREATE_WITH_CURRENT_CONFIG,
    SHARE_FAILED,
    FAILED_TO_VERIFY_METADATA_KEY,
    FAILED_TO_TRUST_METADATA_KEY,
}

internal enum class SuccessSnackbarType {
    NEW_METADATA_KEY_IS_TRUSTED,
}

internal enum class ToastType {
    OTP_CREATED_SHARE_FAILED,
    OTP_CREATED_PERMISSIONS_CHANGED,
}
