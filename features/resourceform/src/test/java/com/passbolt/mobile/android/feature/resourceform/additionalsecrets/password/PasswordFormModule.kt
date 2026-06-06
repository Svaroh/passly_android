package net.svaroh.passly.feature.resourceform.additionalsecrets.password

import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.entropy.EntropyCalculator
import net.svaroh.passly.core.policies.usecase.GetPasswordPoliciesUseCase
import net.svaroh.passly.mappers.EntropyViewMapper
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.mockito.Mockito.mock

internal val mockGetPasswordPoliciesUseCase = mock<GetPasswordPoliciesUseCase>()
internal val mockSecretGenerator = mock<SecretGenerator>()
internal val mockEntropyCalculator = mock<EntropyCalculator>()

internal val testPasswordFormModule =
    module {
        factoryOf(::EntropyViewMapper)
        single { mockEntropyCalculator }
        single { mockGetPasswordPoliciesUseCase }
        single { mockSecretGenerator }
        factory { params ->
            PasswordFormViewModel(
                mode = params.get(),
                passwordModel = params.get(),
                entropyViewMapper = get(),
                entropyCalculator = get(),
                getPasswordPoliciesUseCase = get(),
                secretGenerator = get(),
            )
        }
    }
