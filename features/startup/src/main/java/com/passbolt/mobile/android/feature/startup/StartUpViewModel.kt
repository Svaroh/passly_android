package com.passbolt.mobile.android.feature.startup

import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountsUseCase
import com.passbolt.mobile.android.feature.startup.StartUpIntent.AcknowledgeDeprecatedOsWarning
import com.passbolt.mobile.android.feature.startup.StartUpIntent.HideDeprecatedOsWarning
import com.passbolt.mobile.android.feature.startup.StartUpSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.startup.StartUpSideEffect.NavigateToSignIn
import com.passbolt.mobile.android.feature.startup.deprecatedoswarning.DeprecatedOsWarningInteractor
import com.passbolt.mobile.android.ui.AccountSetupDataModel

class StartUpViewModel(
    private val accountSetupDataModel: AccountSetupDataModel?,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val deprecatedOsWarningInteractor: DeprecatedOsWarningInteractor,
) : SideEffectViewModel<StartUpState, StartUpSideEffect>(StartUpState()) {
    private var resolvedNavigation: StartUpSideEffect? = null

    init {
        launch { resolveStartUp() }
    }

    fun onIntent(intent: StartUpIntent) {
        when (intent) {
            AcknowledgeDeprecatedOsWarning -> dismissDeprecatedOsWarning(hideForCurrentOs = false)
            HideDeprecatedOsWarning -> dismissDeprecatedOsWarning(hideForCurrentOs = true)
        }
    }

    private fun resolveStartUp() {
        val navigation = resolveAccountNavigation()
        resolvedNavigation = navigation
        if (deprecatedOsWarningInteractor.shouldShowDeprecatedOsWarning()) {
            updateViewState { copy(showDeprecatedOsWarning = true) }
        } else {
            emitSideEffect(navigation)
        }
    }

    private fun resolveAccountNavigation(): StartUpSideEffect {
        val accounts = getAccountsUseCase.execute(Unit).users
        return if (accounts.isEmpty() || accountSetupDataModel != null) {
            NavigateToSetup(accountSetupDataModel)
        } else {
            NavigateToSignIn
        }
    }

    private fun dismissDeprecatedOsWarning(hideForCurrentOs: Boolean) {
        if (!viewState.value.showDeprecatedOsWarning) {
            return
        }
        if (hideForCurrentOs) {
            deprecatedOsWarningInteractor.hideDeprecatedOsWarning()
        }
        updateViewState { copy(showDeprecatedOsWarning = false) }
        resolvedNavigation?.let { emitSideEffect(it) }
    }
}
