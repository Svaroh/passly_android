package net.svaroh.passly.feature.authentication.mfa.unknown

import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.feature.authentication.auth.usecase.SignOutUseCase
import net.svaroh.passly.feature.authentication.mfa.unknown.UnknownProviderIntent.Close
import net.svaroh.passly.feature.authentication.mfa.unknown.UnknownProviderSideEffect.CloseAndNavigateToStartup

class UnknownProviderViewModel(
    private val signOutUseCase: SignOutUseCase,
) : SideEffectViewModel<UnknownProviderState, UnknownProviderSideEffect>(
        UnknownProviderState(),
    ) {
    fun onIntent(intent: UnknownProviderIntent) {
        when (intent) {
            is Close -> signOutAndClose()
        }
    }

    private fun signOutAndClose() {
        launch {
            updateViewState { copy(showProgress = true) }
            signOutUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            emitSideEffect(CloseAndNavigateToStartup)
        }
    }
}
