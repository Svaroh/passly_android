package net.svaroh.passly.feature.authentication.mfa.duo

import android.content.Context
import net.svaroh.passly.feature.authentication.mfa.duo.AuthWithDuoSideEffect.SnackbarErrorType
import net.svaroh.passly.feature.authentication.mfa.duo.AuthWithDuoSideEffect.SnackbarErrorType.GENERIC
import net.svaroh.passly.feature.authentication.mfa.duo.AuthWithDuoSideEffect.SnackbarErrorType.SESSION_EXPIRED
import net.svaroh.passly.core.localization.R as LocalizationR

internal fun getSnackbarMessage(
    context: Context,
    kind: SnackbarErrorType,
): String =
    when (kind) {
        GENERIC -> context.getString(LocalizationR.string.unknown_error)
        SESSION_EXPIRED -> context.getString(LocalizationR.string.session_expired)
    }
