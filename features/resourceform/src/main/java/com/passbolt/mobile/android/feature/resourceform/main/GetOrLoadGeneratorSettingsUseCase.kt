package net.svaroh.passly.feature.resourceform.main

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.domain.passwordpolicies.usecase.GetPasswordPoliciesUseCase
import net.svaroh.passly.ui.PassphraseGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel

class GetOrLoadGeneratorSettingsUseCase(
    private val getPasswordPoliciesUseCase: GetPasswordPoliciesUseCase,
) : AsyncUseCase<GetOrLoadGeneratorSettingsUseCase.Input, GetOrLoadGeneratorSettingsUseCase.Output> {
    override suspend fun execute(input: Input): Output =
        if (input.type != null && input.passwordSettings != null && input.passphraseSettings != null) {
            Output(
                settings = GeneratorSettings(input.type, input.passwordSettings, input.passphraseSettings),
                wasLoaded = false,
            )
        } else {
            val policies = getPasswordPoliciesUseCase.execute(Unit)
            Output(
                settings =
                    GeneratorSettings(
                        type = policies.defaultGenerator,
                        passwordSettings = policies.passwordGeneratorSettings,
                        passphraseSettings = policies.passphraseGeneratorSettings,
                    ),
                wasLoaded = true,
            )
        }

    data class Input(
        val type: PasswordGeneratorTypeUiModel?,
        val passwordSettings: PasswordGeneratorSettingsUiModel?,
        val passphraseSettings: PassphraseGeneratorSettingsUiModel?,
    )

    data class Output(
        val settings: GeneratorSettings,
        val wasLoaded: Boolean,
    )
}
