package net.svaroh.passly.feature.startup

import net.svaroh.passly.core.accounts.usecase.accounts.GetAccountsUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.navigation.AccountSetupDataModel
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSetup
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSignIn

class StartUpViewModel(
    private val accountSetupDataModel: AccountSetupDataModel?,
    private val getAccountsUseCase: GetAccountsUseCase,
) : SideEffectViewModel<StartUpState, StartUpSideEffect>(StartUpState) {
    init {
        launch { resolveAccountNavigation() }
    }

    private suspend fun resolveAccountNavigation() {
        val accounts = getAccountsUseCase.execute(Unit).users
        if (accounts.isEmpty() || accountSetupDataModel != null) {
            emitSideEffect(NavigateToSetup(accountSetupDataModel))
        } else {
            emitSideEffect(NavigateToSignIn)
        }
    }
}
