package net.svaroh.passly.feature.authentication.mfa.totp

import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.SignOutUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyTotpUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.Failure
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.NetworkFailure
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.Success
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.Unauthorized
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyTotpUseCase.Output.WrongCode
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpIntent.ChooseOtherProvider
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpIntent.Close
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpIntent.PasteFromClipboard
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpIntent.ToggleRememberMe
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpIntent.ValidateOtp
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.ClearOtp
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.CloseAndNavigateToStartup
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.NavigateToLogin
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.NotifyChooseOtherProvider
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.NotifyVerificationSucceeded
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.PasteOtp
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.GENERIC
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.NETWORK
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.SESSION_EXPIRED
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.WRONG_CODE
import kotlinx.coroutines.delay
import timber.log.Timber

class EnterTotpViewModel(
    hasOtherProvider: Boolean,
    private val authToken: String?,
    private val signOutUseCase: SignOutUseCase,
    private val verifyTotpUseCase: VerifyTotpUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
) : SideEffectViewModel<EnterTotpState, EnterTotpSideEffect>(
        EnterTotpState(hasOtherProvider = hasOtherProvider),
    ) {
    fun onIntent(intent: EnterTotpIntent) {
        when (intent) {
            is PasteFromClipboard -> emitSideEffect(PasteOtp)
            is ChooseOtherProvider -> emitSideEffect(NotifyChooseOtherProvider(authToken))
            is ToggleRememberMe -> updateViewState { copy(rememberMe = intent.checked) }
            is ValidateOtp -> validateOtp(intent.otp)
            is Close -> signOutAndClose()
        }
    }

    private fun validateOtp(otp: String) {
        Timber.d("Verifying TOTP")
        updateViewState { copy(showProgress = true) }
        launch {
            when (
                val result =
                    verifyTotpUseCase.execute(
                        VerifyTotpUseCase.Input(otp, authToken.orEmpty(), viewState.value.rememberMe),
                    )
            ) {
                is Failure<*> -> genericError()
                is NetworkFailure -> networkError()
                is Success -> otpSuccess(result.mfaHeader)
                is WrongCode -> totpError()
                is Unauthorized -> {
                    if (backgroundSessionRefreshSucceeded()) {
                        validateOtp(otp)
                        return@launch
                    } else {
                        emitSideEffect(ShowErrorSnackbar(SESSION_EXPIRED))
                        emitSideEffect(NavigateToLogin)
                    }
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun backgroundSessionRefreshSucceeded() = refreshSessionUseCase.execute(Unit) is RefreshSessionUseCase.Output.Success

    private fun otpSuccess(mfaHeader: String?) {
        mfaHeader?.let {
            emitSideEffect(NotifyVerificationSucceeded(it))
        } ?: run {
            emitSideEffect(ShowErrorSnackbar(GENERIC))
        }
    }

    private fun genericError() {
        emitSideEffect(ClearOtp)
        emitSideEffect(ShowErrorSnackbar(GENERIC))
    }

    private fun networkError() {
        emitSideEffect(ClearOtp)
        emitSideEffect(ShowErrorSnackbar(NETWORK))
    }

    private fun totpError() {
        launch {
            updateViewState { copy(otpTextColor = EnterTotpState.OtpTextColor.ERROR) }
            delay(CLEAR_INPUT_DELAY_MILLIS)
            updateViewState { copy(otpTextColor = EnterTotpState.OtpTextColor.DEFAULT) }
            emitSideEffect(ClearOtp)
        }
        emitSideEffect(ShowErrorSnackbar(WRONG_CODE))
    }

    private fun signOutAndClose() {
        launch {
            updateViewState { copy(showProgress = true) }
            signOutUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            emitSideEffect(CloseAndNavigateToStartup)
        }
    }

    private companion object {
        private const val CLEAR_INPUT_DELAY_MILLIS = 1000L
    }
}
