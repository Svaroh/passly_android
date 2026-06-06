package net.svaroh.passly.feature.resourceform.additionalsecrets.password

import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.entropy.EntropyCalculator
import net.svaroh.passly.domain.passwordpolicies.usecase.GetPasswordPoliciesUseCase
import net.svaroh.passly.feature.resourceform.main.GetOrLoadGeneratorSettingsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.mockito.Mockito.mock

internal val mockGetPasswordPoliciesUseCase = mock<GetPasswordPoliciesUseCase>()
internal val mockSecretGenerator = mock<SecretGenerator>()
internal val mockEntropyCalculator = mock<EntropyCalculator>()

internal val testPasswordFormModule =
    module {
        single { mockEntropyCalculator }
        single { mockGetPasswordPoliciesUseCase }
        single { mockSecretGenerator }
        factoryOf(::GetOrLoadGeneratorSettingsUseCase)
        factory { params ->
            PasswordFormViewModel(
                mode = params.get(),
                passwordModel = params.get(),
                entropyCalculator = get(),
                getOrLoadGeneratorSettingsUseCase = get(),
                secretGenerator = get(),
            )
        }
    }
