package net.svaroh.passly.feature.authentication.mfa.totp

import android.content.Context
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.GENERIC
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.NETWORK
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.SESSION_EXPIRED
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.WRONG_CODE
import net.svaroh.passly.core.localization.R as LocalizationR

internal fun getSnackbarMessage(
    context: Context,
    kind: SnackbarErrorType,
): String =
    when (kind) {
        GENERIC -> context.getString(LocalizationR.string.unknown_error)
        NETWORK -> context.getString(LocalizationR.string.common_network_failure)
        WRONG_CODE -> context.getString(LocalizationR.string.dialog_mfa_wrong_code)
        SESSION_EXPIRED -> context.getString(LocalizationR.string.session_expired)
    }
