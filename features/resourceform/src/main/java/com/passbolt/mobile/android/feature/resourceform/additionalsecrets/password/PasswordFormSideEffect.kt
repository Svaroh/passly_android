package net.svaroh.passly.feature.resourceform.additionalsecrets.password

import net.svaroh.passly.ui.PasswordUiModel

internal sealed interface PasswordFormSideEffect {
    data object NavigateBack : PasswordFormSideEffect

    data class ApplyAndGoBack(
        val model: PasswordUiModel,
    ) : PasswordFormSideEffect

    data class ShowUnableToGeneratePassword(
        val minimumEntropyBits: Int,
    ) : PasswordFormSideEffect
}
