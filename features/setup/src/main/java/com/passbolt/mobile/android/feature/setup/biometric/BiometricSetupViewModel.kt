package net.svaroh.passly.feature.setup.biometric

import net.svaroh.passly.common.BiometricInformationProvider
import net.svaroh.passly.core.accounts.usecase.biometrickey.SaveBiometricKeyIvUseCase
import net.svaroh.passly.core.authenticationcore.passphrase.SavePassphraseUseCase
import net.svaroh.passly.core.autofill.AutofillInformationProvider
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.encryptedstorage.biometric.BiometricCipher
import net.svaroh.passly.feature.authentication.auth.usecase.BiometryInteractor
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.AuthenticationSuccess
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.BiometricAuthenticationCancel
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.BiometricAuthenticationError
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.BiometricAuthenticationSuccess
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.ConfirmKeyPermanentlyInvalidated
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.DismissKeyPermanentlyInvalidated
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.GoToApp
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.KeyPermanentlyInvalidated
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.MaybeLater
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.ResumeView
import net.svaroh.passly.feature.setup.biometric.BiometricSetupIntent.UseBiometric
import net.svaroh.passly.feature.setup.biometric.BiometricSetupSideEffect.NavigateToAccessibilityPolicies
import net.svaroh.passly.feature.setup.biometric.BiometricSetupSideEffect.NavigateToAppSystemSettings
import net.svaroh.passly.feature.setup.biometric.BiometricSetupSideEffect.NavigateToEncourageAutofill
import net.svaroh.passly.feature.setup.biometric.BiometricSetupSideEffect.ShowBiometricPrompt
import net.svaroh.passly.feature.setup.biometric.BiometricSetupSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.setup.biometric.BiometricSetupSideEffect.StartAuthActivity
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.AUTHENTICATION_GENERIC
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.AUTHENTICATION_LOCKOUT
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.AUTHENTICATION_LOCKOUT_PERMANENT
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.BIOMETRIC_ENCRYPT_ERROR
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.BIOMETRIC_NO_CRYPTO_CIPHER
import net.svaroh.passly.ui.BiometricAuthError
import net.svaroh.passly.ui.BiometricAuthError.ERROR_LOCKOUT
import net.svaroh.passly.ui.BiometricAuthError.ERROR_LOCKOUT_PERMANENT
import net.svaroh.passly.ui.BiometricAuthError.GENERIC
import net.svaroh.passly.ui.BiometricAuthError.NO_CRYPTO_CIPHER
import timber.log.Timber
import javax.crypto.Cipher

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

class BiometricSetupViewModel(
    private val biometricInformationProvider: BiometricInformationProvider,
    private val autofillInformationProvider: AutofillInformationProvider,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    private val biometricCipher: BiometricCipher,
    private val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase,
    private val biometryInteractor: BiometryInteractor,
) : SideEffectViewModel<BiometricSetupState, BiometricSetupSideEffect>(BiometricSetupState()) {
    fun onIntent(intent: BiometricSetupIntent) {
        when (intent) {
            ResumeView ->
                updateViewState {
                    copy(hasBiometricSetup = biometricInformationProvider.hasBiometricSetUp())
                }
            UseBiometric -> useBiometric()
            MaybeLater -> saveAccountData()
            is KeyPermanentlyInvalidated -> {
                Timber.e(intent.exception)
                biometryInteractor.disableBiometry()
                updateViewState { copy(showKeyChangesDetected = true) }
            }
            DismissKeyPermanentlyInvalidated -> updateViewState { copy(showKeyChangesDetected = false) }
            ConfirmKeyPermanentlyInvalidated -> emitSideEffect(StartAuthActivity)
            GoToApp -> emitSideEffect(NavigateToAccessibilityPolicies)
            AuthenticationSuccess -> showBiometricPrompt()
            is BiometricAuthenticationSuccess -> saveAccountData(intent.cipher)
            BiometricAuthenticationCancel -> {}
            is BiometricAuthenticationError -> biometricAuthenticationError(intent.error)
        }
    }

    private fun useBiometric() {
        if (biometricInformationProvider.hasBiometricSetUp()) {
            showBiometricPrompt()
        } else {
            emitSideEffect(NavigateToAppSystemSettings)
        }
    }

    private fun showBiometricPrompt() {
        val cipher = biometricCipher.getBiometricEncryptCipher()
        emitSideEffect(ShowBiometricPrompt(cipher))
    }

    private fun saveAccountData(authenticatedCipher: Cipher? = null) {
        when (val cachedPassphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                authenticatedCipher?.let {
                    if (!encryptPassphraseWithBiometricCipher(cachedPassphrase.passphrase, it)) {
                        emitSideEffect(ShowErrorSnackbar(BIOMETRIC_ENCRYPT_ERROR))
                        return
                    }
                }
                if (autofillInformationProvider.isAutofillServiceSupported() &&
                    !autofillInformationProvider.isPassboltAutofillServiceSet()
                ) {
                    emitSideEffect(NavigateToEncourageAutofill)
                } else {
                    emitSideEffect(NavigateToAccessibilityPolicies)
                }
            }
            is PotentialPassphrase.PassphraseNotPresent -> {
                emitSideEffect(StartAuthActivity)
            }
        }
    }

    private fun encryptPassphraseWithBiometricCipher(
        passphrase: ByteArray,
        cipher: Cipher,
    ): Boolean =
        try {
            savePassphraseUseCase.execute(
                SavePassphraseUseCase.Input(passphrase, cipher),
            )
            saveBiometricKeyIvUseCase.execute(
                SaveBiometricKeyIvUseCase.Input(cipher.iv),
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "Error encrypting passphrase with biometric cipher")
            false
        }

    private fun biometricAuthenticationError(error: BiometricAuthError) {
        val errorType =
            when (error) {
                ERROR_LOCKOUT -> AUTHENTICATION_LOCKOUT
                ERROR_LOCKOUT_PERMANENT -> AUTHENTICATION_LOCKOUT_PERMANENT
                NO_CRYPTO_CIPHER -> BIOMETRIC_NO_CRYPTO_CIPHER
                GENERIC -> AUTHENTICATION_GENERIC
            }
        emitSideEffect(ShowErrorSnackbar(errorType))
    }
}
