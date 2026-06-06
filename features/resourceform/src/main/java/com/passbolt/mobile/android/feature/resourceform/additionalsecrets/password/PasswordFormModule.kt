package net.svaroh.passly.feature.resourceform.additionalsecrets.password

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel

internal fun Module.passwordFormModule() {
    viewModel { params ->
        PasswordFormViewModel(
            mode = params.get(),
            passwordModel = params.get(),
            entropyCalculator = get(),
            getOrLoadGeneratorSettingsUseCase = get(),
            secretGenerator = get(),
        )
    }
}
