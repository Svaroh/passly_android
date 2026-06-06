package net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill

import net.svaroh.passly.core.autofill.AutofillInformationProvider
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.Close
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.DismissAutofillNotSupported
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.EnableAutofillService
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.SettingsResult
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.Skip
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.AutofillEnabled
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.NavigateBack
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.OpenAutofillSettings

internal class EncourageNativeAutofillViewModel(
    private val autofillInformationProvider: AutofillInformationProvider,
) : SideEffectViewModel<EncourageNativeAutofillState, EncourageNativeAutofillSideEffect>(
        EncourageNativeAutofillState(),
    ) {
    fun onIntent(intent: EncourageNativeAutofillIntent) {
        when (intent) {
            EnableAutofillService -> {
                if (!autofillInformationProvider.isAutofillServiceSupported()) {
                    updateViewState { copy(showAutofillNotSupported = true) }
                } else {
                    emitSideEffect(OpenAutofillSettings)
                }
            }
            SettingsResult -> {
                if (autofillInformationProvider.isPassboltAutofillServiceSet()) {
                    emitSideEffect(AutofillEnabled)
                }
            }
            Skip -> emitSideEffect(NavigateBack)
            Close -> emitSideEffect(NavigateBack)
            DismissAutofillNotSupported -> updateViewState { copy(showAutofillNotSupported = false) }
        }
    }
}
