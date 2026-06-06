package net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility

import net.svaroh.passly.core.autofill.AutofillInformationProvider
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.Close
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.EnableAccessibilityService
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.GrantOverlayPermission
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.RefreshState
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.NavigateBack
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.OpenAccessibilitySettings
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.OpenOverlaySettings

internal class EncourageAccessibilityViewModel(
    private val autofillInformationProvider: AutofillInformationProvider,
) : SideEffectViewModel<EncourageAccessibilityState, EncourageAccessibilitySideEffect>(
        EncourageAccessibilityState(),
    ) {
    init {
        refreshState()
    }

    fun onIntent(intent: EncourageAccessibilityIntent) {
        when (intent) {
            RefreshState -> refreshState()
            EnableAccessibilityService -> emitSideEffect(OpenAccessibilitySettings)
            GrantOverlayPermission -> emitSideEffect(OpenOverlaySettings)
            Close -> emitSideEffect(NavigateBack)
        }
    }

    private fun refreshState() {
        updateViewState {
            copy(
                isAccessibilityServiceEnabled = autofillInformationProvider.isAccessibilityServiceEnabled(),
                isOverlayPermissionGranted = autofillInformationProvider.isAccessibilityOverlayEnabled(),
            )
        }
    }
}
