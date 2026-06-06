package net.svaroh.passly.feature.startup

import net.svaroh.passly.feature.startup.deprecatedoswarning.deprecatedOsWarningModule
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val startUpModule =
    module {
        viewModel { params ->
            StartUpViewModel(
                accountSetupDataModel = params.getOrNull(),
                getAccountsUseCase = get(),
                deprecatedOsWarningInteractor = get(),
            )
        }
        factoryOf(::AccountSetupModelCreator)
        deprecatedOsWarningModule()
    }
