package net.svaroh.passly.feature.authentication.mfa.yubikey

import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.SignOutUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyYubikeyUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyYubikeyUseCase.Output.Failure
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyYubikeyUseCase.Output.NetworkFailure
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyYubikeyUseCase.Output.Success
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyYubikeyUseCase.Output.Unauthorized
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyYubikeyUseCase.Output.YubikeyNotFromCurrentUser
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.CancelYubikeyScan
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ChooseOtherProvider
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.Close
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.DismissNotFromCurrentUserDialog
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.DismissScanCancelledDialog
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ScanYubikey
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ToggleRememberMe
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ValidateYubikeyOtp
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.CloseAndNavigateToStartup
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.LaunchYubikeyScan
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.NavigateToLogin
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.NotifyChooseOtherProvider
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.NotifyVerificationSucceeded
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.SnackbarErrorType.EMPTY_OTP
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.SnackbarErrorType.GENERIC
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.SnackbarErrorType.SESSION_EXPIRED
import timber.log.Timber

class ScanYubikeyViewModel(
    hasOtherProvider: Boolean,
    private val authToken: String?,
    private val signOutUseCase: SignOutUseCase,
    private val verifyYubikeyUseCase: VerifyYubikeyUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
) : SideEffectViewModel<ScanYubikeyState, ScanYubikeySideEffect>(
        ScanYubikeyState(hasOtherProvider = hasOtherProvider),
    ) {
    fun onIntent(intent: ScanYubikeyIntent) {
        when (intent) {
            is ScanYubikey -> emitSideEffect(LaunchYubikeyScan)
            is CancelYubikeyScan -> updateViewState { copy(showScanCancelledDialog = true) }
            is ChooseOtherProvider -> emitSideEffect(NotifyChooseOtherProvider(authToken))
            is ValidateYubikeyOtp -> validateYubikeyOtp(intent.otp)
            is Close -> signOutAndClose()
            is ToggleRememberMe -> updateViewState { copy(rememberMe = intent.checked) }
            is DismissScanCancelledDialog -> updateViewState { copy(showScanCancelledDialog = false) }
            is DismissNotFromCurrentUserDialog -> updateViewState { copy(showNotFromCurrentUserDialog = false) }
        }
    }

    private fun validateYubikeyOtp(otp: String?) {
        if (!otp.isNullOrBlank()) {
            verifyYubikey(otp)
        } else {
            emitSideEffect(ShowErrorSnackbar(EMPTY_OTP))
        }
    }

    private fun verifyYubikey(otp: String) {
        Timber.d("Verifying Yubikey")
        updateViewState { copy(showProgress = true) }
        launch {
            when (
                val result =
                    verifyYubikeyUseCase.execute(
                        VerifyYubikeyUseCase.Input(otp, authToken, viewState.value.rememberMe),
                    )
            ) {
                is Failure<*> -> emitSideEffect(ShowErrorSnackbar(GENERIC))
                is NetworkFailure -> emitSideEffect(ShowErrorSnackbar(GENERIC))
                is Success -> yubikeySuccess(result.mfaHeader)
                is Unauthorized -> {
                    if (backgroundSessionRefreshSucceeded()) {
                        verifyYubikey(otp)
                        return@launch
                    } else {
                        emitSideEffect(ShowErrorSnackbar(SESSION_EXPIRED))
                        emitSideEffect(NavigateToLogin)
                    }
                }
                is YubikeyNotFromCurrentUser ->
                    updateViewState { copy(showNotFromCurrentUserDialog = true) }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun backgroundSessionRefreshSucceeded() = refreshSessionUseCase.execute(Unit) is RefreshSessionUseCase.Output.Success

    private fun yubikeySuccess(mfaHeader: String?) {
        mfaHeader?.let {
            emitSideEffect(NotifyVerificationSucceeded(it))
        } ?: run {
            emitSideEffect(ShowErrorSnackbar(GENERIC))
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
