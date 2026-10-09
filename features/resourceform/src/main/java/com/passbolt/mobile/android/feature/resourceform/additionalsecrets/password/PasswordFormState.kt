package net.svaroh.passly.feature.resourceform.additionalsecrets.password

import net.svaroh.passly.ui.PassphraseGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel
import net.svaroh.passly.ui.PasswordStrength
import net.svaroh.passly.ui.ResourceFormMode

internal data class PasswordFormState(
    val resourceFormMode: ResourceFormMode? = null,
    val password: String = "",
    val passwordStrength: PasswordStrength = PasswordStrength.Empty,
    val entropy: Double = 0.0,
    val mainUri: String = "",
    val username: String = "",
    val isUnableToGeneratePasswordDialogVisible: Boolean = false,
    val minimumEntropyBits: Int = 0,
    val generatorType: PasswordGeneratorTypeUiModel? = null,
    val passwordGeneratorSettings: PasswordGeneratorSettingsUiModel? = null,
    val passphraseGeneratorSettings: PassphraseGeneratorSettingsUiModel? = null,
)
