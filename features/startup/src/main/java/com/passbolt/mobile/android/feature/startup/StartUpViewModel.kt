package com.passbolt.mobile.android.feature.startup

import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.feature.startup.StartUpSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.startup.StartUpSideEffect.NavigateToSignIn

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
