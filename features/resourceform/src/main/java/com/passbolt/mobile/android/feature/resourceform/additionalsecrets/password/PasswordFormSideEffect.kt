package net.svaroh.passly.feature.resourceform.additionalsecrets.password

import net.svaroh.passly.ui.PassphraseGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel
import net.svaroh.passly.ui.PasswordUiModel

internal sealed interface PasswordFormSideEffect {
    data object NavigateBack : PasswordFormSideEffect

    data class ApplyAndGoBack(
        val model: PasswordUiModel,
    ) : PasswordFormSideEffect

    data class NavigateToAdvancedSecretGeneration(
        val selectedTab: PasswordGeneratorTypeUiModel,
        val passwordSettings: PasswordGeneratorSettingsUiModel,
        val passphraseSettings: PassphraseGeneratorSettingsUiModel,
    ) : PasswordFormSideEffect
}
