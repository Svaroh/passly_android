package net.svaroh.passly.feature.autofill.resources

import android.content.Context
import net.svaroh.passly.feature.autofill.resources.ToastType.DECRYPTION_FAILURE
import net.svaroh.passly.feature.autofill.resources.ToastType.FETCH_FAILURE
import net.svaroh.passly.feature.autofill.resources.ToastType.INVALID_TOTP_PARAMETERS
import net.svaroh.passly.core.localization.R as LocalizationR

internal fun getToastMessage(
    context: Context,
    type: ToastType,
): String =
    when (type) {
        DECRYPTION_FAILURE -> context.getString(LocalizationR.string.common_decryption_failure)
        FETCH_FAILURE -> context.getString(LocalizationR.string.common_fetch_failure)
        INVALID_TOTP_PARAMETERS -> context.getString(LocalizationR.string.common_invalid_totp_parameters)
    }
