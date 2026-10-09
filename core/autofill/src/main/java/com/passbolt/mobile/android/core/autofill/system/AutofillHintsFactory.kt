package net.svaroh.passly.core.autofill.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.annotation.ArrayRes
import java.util.Locale
import net.svaroh.passly.core.localization.R as LocalizationR

class AutofillHintsFactory(
    private val resources: Resources,
    private val appContext: Context,
) {
    private val englishHintsByResId: Map<Int, Array<String>> =
        listOf(
            LocalizationR.array.username_hint_values,
            LocalizationR.array.password_hint_values,
            LocalizationR.array.totp_hint_values,
        ).associateWith(::loadEnglishHints)

    fun getHintValues(field: AutofillField) =
        when (field) {
            AutofillField.USERNAME ->
                getLocalizedOrEnglishValues(LocalizationR.array.username_hint_values) +
                    View.AUTOFILL_HINT_USERNAME
            AutofillField.PASSWORD ->
                getLocalizedOrEnglishValues(LocalizationR.array.password_hint_values) +
                    View.AUTOFILL_HINT_PASSWORD
            AutofillField.TOTP ->
                getLocalizedOrEnglishValues(LocalizationR.array.totp_hint_values)
        }

    private fun getLocalizedOrEnglishValues(
        @ArrayRes stringArrayResId: Int,
    ) = getLocalized(stringArrayResId) + requireNotNull(englishHintsByResId[stringArrayResId])

    @SuppressLint("AppBundleLocaleChanges")
    private fun loadEnglishHints(
        @ArrayRes stringArrayResId: Int,
    ): Array<String> {
        val config = Configuration(appContext.resources.configuration)
        config.setLocale(Locale.ENGLISH)
        return appContext.createConfigurationContext(config).resources.getStringArray(stringArrayResId)
    }

    private fun getLocalized(
        @ArrayRes stringArrayResId: Int,
    ) = resources.getStringArray(stringArrayResId)
}
