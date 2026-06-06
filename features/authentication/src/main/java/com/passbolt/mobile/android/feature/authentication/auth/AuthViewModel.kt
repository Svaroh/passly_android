package net.svaroh.passly.feature.authentication.auth

import android.security.keystore.KeyPermanentlyInvalidatedException
import net.svaroh.passly.common.extension.erase
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.accountdata.GetAccountDataUseCase
import net.svaroh.passly.core.accounts.usecase.accountdata.SaveServerFingerprintUseCase
import net.svaroh.passly.core.accounts.usecase.privatekey.GetPrivateKeyUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import net.svaroh.passly.core.authenticationcore.passphrase.GetPassphraseUseCase
import net.svaroh.passly.core.authenticationcore.session.SaveSessionUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.idlingresource.SignInIdlingResource
import net.svaroh.passly.core.inappreview.InAppReviewInteractor
import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.DUO
import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.TOTP
import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.YUBIKEY
import net.svaroh.passly.core.mvp.authentication.MfaProvidersHandler
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig.ManageAccount
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig.Setup
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig.Startup
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase.Passphrase
import net.svaroh.passly.core.preferences.usecase.GetGlobalPreferencesUseCase
import net.svaroh.passly.core.security.rootdetection.RootDetector
import net.svaroh.passly.core.security.runtimeauth.RuntimeAuthenticatedFlag
import net.svaroh.passly.encryptedstorage.biometric.BiometricCipher
import net.svaroh.passly.feature.authentication.auth.AuthIntent.AcceptChangedServerFingerprint
import net.svaroh.passly.feature.authentication.auth.AuthIntent.AccessLogs
import net.svaroh.passly.feature.authentication.auth.AuthIntent.AuthenticateUsingBiometry
import net.svaroh.passly.feature.authentication.auth.AuthIntent.BiometricAuthenticationError
import net.svaroh.passly.feature.authentication.auth.AuthIntent.BiometricAuthenticationSuccess
import net.svaroh.passly.feature.authentication.auth.AuthIntent.BiometricKeyInvalidated
import net.svaroh.passly.feature.authentication.auth.AuthIntent.ChooseOtherMfaProvider
import net.svaroh.passly.feature.authentication.auth.AuthIntent.ConfirmSetupLeave
import net.svaroh.passly.feature.authentication.auth.AuthIntent.ConnectToExistingAccount
import net.svaroh.passly.feature.authentication.auth.AuthIntent.DismissConfirmSetupLeave
import net.svaroh.passly.feature.authentication.auth.AuthIntent.DismissHelpMenu
import net.svaroh.passly.feature.authentication.auth.AuthIntent.DismissNoAccountExplanation
import net.svaroh.passly.feature.authentication.auth.AuthIntent.DismissServerNotReachable
import net.svaroh.passly.feature.authentication.auth.AuthIntent.ForgotPassword
import net.svaroh.passly.feature.authentication.auth.AuthIntent.GoBack
import net.svaroh.passly.feature.authentication.auth.AuthIntent.MfaSucceeded
import net.svaroh.passly.feature.authentication.auth.AuthIntent.OpenHelpMenu
import net.svaroh.passly.feature.authentication.auth.AuthIntent.PassphraseInputChanged
import net.svaroh.passly.feature.authentication.auth.AuthIntent.RejectChangedServerFingerprint
import net.svaroh.passly.feature.authentication.auth.AuthIntent.Retry
import net.svaroh.passly.feature.authentication.auth.AuthIntent.RootedDeviceAcknowledged
import net.svaroh.passly.feature.authentication.auth.AuthIntent.SignIn
import net.svaroh.passly.feature.authentication.auth.AuthIntent.SignOut
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.AuthSuccess
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.FinishAffinity
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.HideKeyboard
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.LaunchBiometricPrompt
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.NavigateBack
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.NavigateToAccountList
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.NavigateToLogs
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.NavigateToMfa
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.AUTHENTICATION_ERROR
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.BIOMETRIC_CHANGED
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.BIOMETRIC_DECRYPT_ERROR
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.CHALLENGE_INVALID_SIGNATURE
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.CHALLENGE_TOKEN_EXPIRED
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.CHALLENGE_VERIFICATION_FAILURE
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.DECRYPTION_ERROR
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.GENERIC
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.TIME_OUT_OF_SYNC
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect.SnackbarErrorType.WRONG_PASSPHRASE
import net.svaroh.passly.feature.authentication.auth.challenge.MfaStatus
import net.svaroh.passly.feature.authentication.auth.challenge.MfaStatusProvider
import net.svaroh.passly.feature.authentication.auth.challenge.MfaStatusProvider.MfaState
import net.svaroh.passly.feature.authentication.auth.usecase.BiometryInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor.Error.Generic
import net.svaroh.passly.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor.Error.IncorrectServerFingerprint
import net.svaroh.passly.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor.Error.ServerNotReachable
import net.svaroh.passly.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor.Error.TimeIsOutOfSync
import net.svaroh.passly.feature.authentication.auth.usecase.PostSignInActionsInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.PostSignInActionsInteractor.Error.ConfigurationFetchError
import net.svaroh.passly.feature.authentication.auth.usecase.PostSignInActionsInteractor.Error.UserProfileFetchError
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor.Error.AccountDoesNotExist
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor.Error.ChallengeDecryptionError
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor.Error.ChallengeVerificationError
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor.Error.ChallengeVerificationError.Type.FAILURE
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor.Error.ChallengeVerificationError.Type.INVALID_SIGNATURE
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor.Error.ChallengeVerificationError.Type.TOKEN_EXPIRED
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor.Error.IncorrectPassphrase
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor.Error.SignInFailure
import net.svaroh.passly.feature.authentication.auth.usecase.SignOutUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyPassphraseUseCase
import net.svaroh.passly.feature.authentication.mfa.MfaDialogState.Duo
import net.svaroh.passly.feature.authentication.mfa.MfaDialogState.Totp
import net.svaroh.passly.feature.authentication.mfa.MfaDialogState.UnknownProvider
import net.svaroh.passly.feature.authentication.mfa.MfaDialogState.Yubikey
import net.svaroh.passly.mappers.AccountModelMapper
import net.svaroh.passly.ui.BiometricAuthError
import timber.log.Timber
import javax.crypto.Cipher

@Suppress("LongParameterList", "TooManyFunctions")
class AuthViewModel(
    private val authConfig: AuthConfig,
    private val userId: String,
    private val appContext: AppContext,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val verifyPassphraseUseCase: VerifyPassphraseUseCase,
    private val biometricCipher: BiometricCipher,
    private val getPassphraseUseCase: GetPassphraseUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val rootDetector: RootDetector,
    private val biometryInteractor: BiometryInteractor,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag,
    private val saveSessionUseCase: SaveSessionUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val saveServerFingerprintUseCase: SaveServerFingerprintUseCase,
    private val mfaStatusProvider: MfaStatusProvider,
    private val getAndVerifyServerKeysInteractor: GetAndVerifyServerKeysAndTimeInteractor,
    private val signInVerifyInteractor: SignInVerifyInteractor,
    private val inAppReviewInteractor: InAppReviewInteractor,
    private val signInIdlingResource: SignInIdlingResource,
    private val postSignInActionsInteractor: PostSignInActionsInteractor,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val mfaProvidersHandler: MfaProvidersHandler,
) : SideEffectViewModel<AuthState, AuthSideEffect>(
        AuthState(
            authReason = mapAuthReason(authConfig),
            canShowLeaveConfirmation = authConfig is Setup,
        ),
    ) {
    private var loginState: LoginState? = null

    /**
     * Operational passphrase.
     * Typed bytes are mirrored into [AuthState.passphrase] as a display String;
     * biometric-decrypted bytes are kept here but NOT mirrored into state,
     * so the visibility toggle can't reveal a value the user never typed.
     */
    private var passphrase: ByteArray = ByteArray(0)

    init {
        loadAccountData()
        checkRootAndBiometry()
    }

    override fun onCleared() {
        super.onCleared()
        passphrase.erase()
    }

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is SignIn -> signIn()
            is PassphraseInputChanged -> passphraseInputChanged(intent.passphrase)
            is AuthenticateUsingBiometry -> authenticateUsingBiometry()
            is BiometricAuthenticationSuccess -> biometricAuthenticationSuccess(intent.cipher)
            is BiometricAuthenticationError -> biometricAuthenticationError(intent.error)
            is BiometricKeyInvalidated -> {
                biometryInteractor.disableBiometry()
                updateViewState { copy(showBiometricButton = false) }
                emitSideEffect(ShowErrorSnackbar(BIOMETRIC_CHANGED))
            }
            is ForgotPassword -> updateViewState { copy(showForgotPasswordDialog = true) }
            is GoBack -> goBack()
            is ConfirmSetupLeave -> {
                updateViewState { copy(showLeaveConfirmationDialog = false) }
                emitSideEffect(NavigateBack)
            }
            is OpenHelpMenu -> updateViewState { copy(showHelpMenu = true) }
            is DismissHelpMenu -> updateViewState { copy(showHelpMenu = false) }
            is AccessLogs -> {
                updateViewState { copy(showHelpMenu = false) }
                emitSideEffect(NavigateToLogs)
            }
            is ConnectToExistingAccount -> {
                updateViewState { copy(showAccountDoesNotExist = false) }
                emitSideEffect(NavigateToAccountList)
            }
            is RootedDeviceAcknowledged -> {
                updateViewState { copy(showDeviceRooted = false) }
                handleBiometry()
            }
            is AcceptChangedServerFingerprint -> acceptChangedServerFingerprint(intent.fingerprint)
            is MfaSucceeded -> mfaSucceeded(intent.mfaHeader)
            is ChooseOtherMfaProvider -> chooseOtherMfaProvider(intent.bearer, intent.currentProvider)
            is Retry -> retry()
            is SignOut -> signOut()
            is DismissNoAccountExplanation -> updateViewState { copy(showForgotPasswordDialog = false) }
            is DismissServerNotReachable -> updateViewState { copy(showServerNotReachable = false) }
            is DismissConfirmSetupLeave -> updateViewState { copy(showLeaveConfirmationDialog = false) }
            is RejectChangedServerFingerprint -> {
                updateViewState { copy(showServerFingerprintChanged = false) }
                emitSideEffect(FinishAffinity)
            }
        }
    }

    private fun loadAccountData() {
        launch {
            getAccountDataUseCase.execute(UserIdInput(userId)).let { account ->
                updateViewState {
                    copy(
                        accountData =
                            AuthState.AccountData(
                                label =
                                    account.label ?: AccountModelMapper.defaultLabel(
                                        account.firstName,
                                        account.lastName,
                                    ),
                                domain = account.url,
                                email = account.email,
                                avatarUrl = account.avatarUrl,
                            ),
                    )
                }
            }
        }
    }

    private fun checkRootAndBiometry() {
        if (!getGlobalPreferencesUseCase.execute(Unit).isHideRootDialogEnabled && rootDetector.isDeviceRooted()) {
            updateViewState { copy(showDeviceRooted = true) }
        } else {
            handleBiometry()
        }
    }

    private fun handleBiometry() {
        biometryInteractor.onBiometryReady(userId) {
            updateViewState { copy(showBiometricButton = true) }
            tryShowBiometricPrompt()
        }
    }

    private fun tryShowBiometricPrompt() {
        try {
            val cipher = biometricCipher.getBiometricDecryptCipher(userId)
            emitSideEffect(LaunchBiometricPrompt(cipher, viewState.value.authReason))
        } catch (exception: KeyPermanentlyInvalidatedException) {
            Timber.e(exception, "Biometric key has been invalidated")
            biometryInteractor.disableBiometry()
            updateViewState { copy(showBiometricButton = false) }
            emitSideEffect(ShowErrorSnackbar(BIOMETRIC_CHANGED))
        } catch (exception: Exception) {
            Timber.e(exception, "Exception during getting biometric cipher")
            emitSideEffect(ShowErrorSnackbar(GENERIC))
        }
    }

    private fun passphraseInputChanged(newPassphrase: ByteArray) {
        passphrase.erase()
        passphrase = newPassphrase
        updateViewState {
            copy(
                passphrase = String(newPassphrase, Charsets.UTF_8),
                isAuthButtonEnabled = newPassphrase.isNotEmpty(),
            )
        }
    }

    private fun signIn() {
        emitSideEffect(HideKeyboard)
        validatePassphrase(passphrase.copyOf())
    }

    private fun validatePassphrase(passphrase: ByteArray) {
        launch {
            val privateKey =
                requireNotNull(
                    getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey,
                )
            val isPassphraseCorrect =
                verifyPassphraseUseCase.execute(VerifyPassphraseUseCase.Input(privateKey, passphrase)).isCorrect
            if (isPassphraseCorrect) {
                passphraseMemoryCache.set(passphrase)
                onPassphraseVerified(passphrase)
            } else {
                passphrase.erase()
                emitSideEffect(ShowErrorSnackbar(WRONG_PASSPHRASE))
            }
        }
    }

    private fun onPassphraseVerified(passphrase: ByteArray) {
        when (authConfig) {
            is Startup,
            is Setup,
            is ManageAccount,
            is AuthConfig.SignIn,
            -> performSignIn(passphrase)
            is AuthConfig.RefreshPassphrase,
            is AuthConfig.Mfa,
            -> {
                runtimeAuthenticatedFlag.isAuthenticated = true
                passphrase.erase()
                emitSideEffect(AuthSuccess(authConfig, appContext))
            }
            is AuthConfig.RefreshSession -> performRefreshSession(passphrase)
        }
    }

    private fun performRefreshSession(passphrase: ByteArray) {
        updateViewState { copy(showProgress = true) }
        signInIdlingResource.setIdle(false)
        launch {
            val refreshSessionResult = refreshSessionUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            signInIdlingResource.setIdle(true)
            when (refreshSessionResult) {
                is RefreshSessionUseCase.Output.Success -> {
                    passphrase.erase()
                    runtimeAuthenticatedFlag.isAuthenticated = true
                    emitSideEffect(AuthSuccess(authConfig, appContext))
                }
                is RefreshSessionUseCase.Output.Failure -> {
                    performFullSignIn(passphrase)
                }
            }
        }
    }

    private fun performSignIn(passphrase: ByteArray) {
        if (authConfig is AuthConfig.RefreshSession) {
            performRefreshSession(passphrase)
        } else {
            performFullSignIn(passphrase)
        }
    }

    @Suppress("LongMethod")
    private fun performFullSignIn(passphrase: ByteArray) {
        signInIdlingResource.setIdle(false)
        updateViewState { copy(showProgress = true) }
        launch {
            getAndVerifyServerKeysInteractor.getAndVerifyServerKeys(
                userId,
                onError = {
                    updateViewState { copy(showProgress = false) }
                    when (it) {
                        is Generic -> {
                            emitSideEffect(ShowErrorSnackbar(GENERIC))
                        }
                        is IncorrectServerFingerprint -> {
                            updateViewState {
                                copy(
                                    showServerFingerprintChanged = true,
                                    serverFingerprintChangedFingerprint = it.fingerprint,
                                )
                            }
                        }
                        is ServerNotReachable -> {
                            updateViewState {
                                copy(showServerNotReachable = true, serverNotReachableDomain = it.serverUrl)
                            }
                        }
                        is TimeIsOutOfSync -> {
                            emitSideEffect(ShowErrorSnackbar(TIME_OUT_OF_SYNC))
                        }
                    }
                },
            ) {
                signIn(passphrase.copyOf(), it.pgpKey, it.rsaKey, it.pgpKeyFingerprint)
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun signIn(
        passphrase: ByteArray,
        serverPublicKey: String,
        rsaKey: String,
        fingerprint: String,
    ) {
        signInVerifyInteractor.signInVerify(
            serverPublicKey,
            passphrase,
            userId,
            rsaKey,
            onError = {
                passphrase.erase()
                updateViewState { copy(showProgress = false) }
                signInIdlingResource.setIdle(true)
                when (it) {
                    is AccountDoesNotExist ->
                        updateViewState {
                            copy(
                                showAccountDoesNotExist = true,
                                accountDoesNotExistLabel = it.label,
                                accountDoesNotExistEmail = it.email,
                                accountDoesNotExistUrl = it.serverUrl,
                            )
                        }
                    is ChallengeDecryptionError -> emitSideEffect(ShowErrorSnackbar(DECRYPTION_ERROR, it.message))
                    is ChallengeVerificationError -> {
                        when (it.type) {
                            TOKEN_EXPIRED -> emitSideEffect(ShowErrorSnackbar(CHALLENGE_TOKEN_EXPIRED))
                            INVALID_SIGNATURE -> emitSideEffect(ShowErrorSnackbar(CHALLENGE_INVALID_SIGNATURE))
                            FAILURE -> emitSideEffect(ShowErrorSnackbar(CHALLENGE_VERIFICATION_FAILURE))
                        }
                    }
                    is IncorrectPassphrase -> emitSideEffect(ShowErrorSnackbar(WRONG_PASSPHRASE))
                    is SignInFailure -> emitSideEffect(ShowErrorSnackbar(AUTHENTICATION_ERROR, it.message))
                }
            },
        ) {
            passphrase.erase()
            loginState =
                LoginState(
                    accessToken = it.accessToken,
                    refreshToken = it.refreshToken,
                    fingerprint = fingerprint,
                    mfaToken = it.mfaToken,
                )
            Timber.d("Checking MFA status")
            mfaStatusProvider.setState(
                MfaState(
                    challengeResponseDto = it.challengeResponseDto,
                    newMfaToken = it.mfaToken,
                    currentMfaToken = it.currentMfaToken,
                ),
            )
            when (mfaStatusProvider.provideMfaStatus()) {
                MfaStatus.NOT_REQUIRED -> {
                    Timber.d("MFA not required")
                    signInSuccess()
                }
                MfaStatus.REQUIRED -> {
                    Timber.d("MFA required")
                    mfaRequired(it.accessToken, it.challengeResponseDto.mfaProviders)
                }
            }
        }
    }

    private fun signInSuccess(updateSession: Boolean = true) {
        Timber.d("Authentication success")
        runtimeAuthenticatedFlag.isAuthenticated = true
        passphraseMemoryCache.set(passphrase.copyOf())
        val currentLoginState = requireNotNull(loginState)
        if (updateSession) {
            saveSessionUseCase.execute(
                SaveSessionUseCase.Input(
                    userId = userId,
                    accessToken = currentLoginState.accessToken,
                    refreshToken = currentLoginState.refreshToken,
                    mfaToken = loginState?.mfaToken,
                ),
            )
        }
        saveServerFingerprintUseCase.execute(
            SaveServerFingerprintUseCase.Input(userId, currentLoginState.fingerprint),
        )
        saveSelectedAccountUseCase.execute(UserIdInput(userId))
        Timber.d("Increasing sign in count")
        inAppReviewInteractor.processSuccessfulSignIn()
        loginState = null
        updateViewState { copy(showProgress = true) }
        launchPostSignInActions()
    }

    private fun launchPostSignInActions() {
        launch {
            postSignInActionsInteractor.launchPostSignInActions(
                onError = {
                    updateViewState { copy(showProgress = false) }
                    signInIdlingResource.setIdle(true)
                    when (it) {
                        ConfigurationFetchError -> updateViewState { copy(showFetchFeatureFlagsError = true) }
                        UserProfileFetchError ->
                            emitSideEffect(
                                ShowErrorSnackbar(AuthSideEffect.SnackbarErrorType.PROFILE_FETCH_FAILURE),
                            )
                    }
                },
            ) {
                updateViewState { copy(showProgress = false) }
                emitSideEffect(AuthSuccess(authConfig, appContext))
                signInIdlingResource.setIdle(true)
            }
        }
    }

    private fun authenticateUsingBiometry() {
        tryShowBiometricPrompt()
    }

    private fun biometricAuthenticationSuccess(authenticatedCipher: Cipher?) {
        authenticatedCipher?.let {
            val potentialPassphrase =
                decryptPassphraseWithBiometricCipher(authenticatedCipher)
                    ?: run {
                        emitSideEffect(ShowErrorSnackbar(BIOMETRIC_DECRYPT_ERROR))
                        return
                    }
            if (potentialPassphrase is Passphrase) {
                passphraseMemoryCache.set(potentialPassphrase.passphrase)
                passphrase.erase()
                passphrase = potentialPassphrase.passphrase.copyOf()
                updateViewState { copy(passphrase = "", isAuthButtonEnabled = false) }
                when (authConfig) {
                    is AuthConfig.RefreshPassphrase,
                    is AuthConfig.Mfa,
                    -> {
                        runtimeAuthenticatedFlag.isAuthenticated = true
                        emitSideEffect(AuthSuccess(authConfig, appContext))
                    }
                    else -> performSignIn(passphrase.copyOf())
                }
            } else {
                emitSideEffect(ShowErrorSnackbar(GENERIC))
            }
        }
    }

    private fun decryptPassphraseWithBiometricCipher(authenticatedCipher: Cipher): PotentialPassphrase? =
        try {
            getPassphraseUseCase
                .execute(GetPassphraseUseCase.Input(userId, authenticatedCipher))
                .potentialPassphrase
        } catch (e: Exception) {
            Timber.e(e, "Error decrypting passphrase with biometric cipher")
            null
        }

    private fun biometricAuthenticationError(error: BiometricAuthError) {
        val errorType =
            when (error) {
                BiometricAuthError.NO_CRYPTO_CIPHER -> AuthSideEffect.SnackbarErrorType.BIOMETRIC_NO_CRYPTO_CIPHER
                else -> AUTHENTICATION_ERROR
            }
        emitSideEffect(ShowErrorSnackbar(errorType))
    }

    private fun goBack() {
        if (viewState.value.canShowLeaveConfirmation) {
            updateViewState { copy(showLeaveConfirmationDialog = true) }
        } else {
            emitSideEffect(NavigateBack)
        }
    }

    private fun acceptChangedServerFingerprint(fingerprint: String) {
        updateViewState { copy(showServerFingerprintChanged = false) }
        when (authConfig) {
            is Startup,
            is Setup,
            is ManageAccount,
            is AuthConfig.SignIn,
            is AuthConfig.RefreshSession,
            ->
                saveServerFingerprintUseCase.execute(
                    SaveServerFingerprintUseCase.Input(userId, fingerprint),
                )
            else -> { // not used for passphrase-only modes
            }
        }
    }

    private fun mfaSucceeded(mfaHeader: String?) {
        Timber.d("MFA succeeded")
        when (authConfig) {
            is AuthConfig.RefreshPassphrase,
            is AuthConfig.Mfa,
            -> {
                runtimeAuthenticatedFlag.isAuthenticated = true
                emitSideEffect(AuthSuccess(authConfig, appContext))
            }
            else -> {
                mfaHeader?.let {
                    loginState?.mfaToken = it
                }
                signInSuccess(mfaHeader != null)
            }
        }
    }

    private fun mfaRequired(
        jwtToken: String,
        mfaProviders: List<String>?,
    ) {
        mfaProvidersHandler.setProviders(
            mfaProviders.orEmpty().map {
                MfaProvider.parse(it)
            },
        )
        val hasMultiple = mfaProvidersHandler.hasMultipleProviders()
        val mfaState =
            when (val provider = mfaProvidersHandler.firstMfaProvider()) {
                YUBIKEY -> Yubikey(jwtToken, hasMultiple)
                TOTP -> Totp(jwtToken, hasMultiple)
                DUO -> Duo(jwtToken, hasMultiple)
                else -> {
                    Timber.e("Unknown provider: $provider")
                    UnknownProvider
                }
            }
        updateViewState { copy(showProgress = false) }
        emitSideEffect(NavigateToMfa(mfaState))
    }

    private fun chooseOtherMfaProvider(
        bearer: String?,
        currentProvider: MfaProvider,
    ) {
        val hasMultiple = mfaProvidersHandler.hasMultipleProviders()
        val mfaState =
            when (mfaProvidersHandler.nextMfaProvider(currentProvider)) {
                YUBIKEY -> Yubikey(bearer, hasMultiple)
                TOTP -> Totp(bearer, hasMultiple)
                DUO -> Duo(bearer, hasMultiple)
                null -> UnknownProvider
            }
        emitSideEffect(NavigateToMfa(mfaState))
    }

    private fun retry() {
        updateViewState { copy(showFetchFeatureFlagsError = false) }
        passphraseMemoryCache.get().let {
            if (it is Passphrase) {
                performSignIn(it.passphrase)
            }
        }
    }

    private fun signOut() {
        updateViewState { copy(showFetchFeatureFlagsError = false, showProgress = true) }
        launch {
            signOutUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            emitSideEffect(NavigateBack)
        }
    }

    private class LoginState(
        val accessToken: String,
        val refreshToken: String,
        val fingerprint: String,
        var mfaToken: String? = null,
    )

    companion object {
        fun mapAuthReason(authConfig: AuthConfig): AuthState.RefreshAuthReason? =
            when (authConfig) {
                is AuthConfig.RefreshPassphrase -> AuthState.RefreshAuthReason.PASSPHRASE
                is AuthConfig.SignIn -> AuthState.RefreshAuthReason.SESSION
                else -> null
            }
    }
}
