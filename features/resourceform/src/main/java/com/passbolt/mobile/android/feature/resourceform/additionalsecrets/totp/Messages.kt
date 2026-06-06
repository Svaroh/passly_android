package net.svaroh.passly.feature.resourceform.additionalsecrets.totp

import android.content.Context
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustBeBase32
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustNotBeEmpty
import net.svaroh.passly.core.localization.R as LocalizationR

internal fun getSecretErrorMessage(
    context: Context,
    errors: List<TotpSecretValidationError>,
): String =
    when (errors.first()) {
        MustNotBeEmpty -> context.getString(LocalizationR.string.validation_is_required)
        MustBeBase32 -> context.getString(LocalizationR.string.validation_invalid_totp_secret)
    }
