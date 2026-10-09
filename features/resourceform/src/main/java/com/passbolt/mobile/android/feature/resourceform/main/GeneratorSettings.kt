package net.svaroh.passly.feature.resourceform.main

import net.svaroh.passly.ui.PassphraseGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel

data class GeneratorSettings(
    val type: PasswordGeneratorTypeUiModel,
    val passwordSettings: PasswordGeneratorSettingsUiModel,
    val passphraseSettings: PassphraseGeneratorSettingsUiModel,
)
