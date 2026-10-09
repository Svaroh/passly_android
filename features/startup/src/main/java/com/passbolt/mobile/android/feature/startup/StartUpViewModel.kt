package net.svaroh.passly.feature.startup

import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.domain.accounts.usecase.GetAccountsUseCase
import net.svaroh.passly.feature.startup.StartUpIntent.AcknowledgeDeprecatedOsWarning
import net.svaroh.passly.feature.startup.StartUpIntent.HideDeprecatedOsWarning
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSetup
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSignIn
import net.svaroh.passly.feature.startup.deprecatedoswarning.DeprecatedOsWarningInteractor
import net.svaroh.passly.ui.AccountSetupDataModel

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
