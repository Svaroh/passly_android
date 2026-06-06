package net.svaroh.passly.feature.settings.navigation

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.SettingsNavigationKey
import net.svaroh.passly.feature.accessibilitypolicies.AccessibilityPoliciesFlow
import net.svaroh.passly.feature.accessibilitypolicies.AccessibilityPoliciesScreen
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.autofillenabled.AutofillEnabledScreen
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityScreen
import net.svaroh.passly.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillScreen

class AutofillEncouragementsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<SettingsNavigationKey.EncourageNativeAutofill> {
                PassboltTheme { EncourageNativeAutofillScreen(dismissBehavior = it.dismissBehavior) }
            }
            entry<SettingsNavigationKey.AutofillEnabled> {
                PassboltTheme { AutofillEnabledScreen() }
            }
            entry<SettingsNavigationKey.EncourageAccessibilityAutofill> {
                PassboltTheme { EncourageAccessibilityScreen() }
            }
            entry<SettingsNavigationKey.AccessibilityPoliciesConsent> {
                PassboltTheme { AccessibilityPoliciesScreen(flow = AccessibilityPoliciesFlow.SETTINGS) }
            }
        }
}
