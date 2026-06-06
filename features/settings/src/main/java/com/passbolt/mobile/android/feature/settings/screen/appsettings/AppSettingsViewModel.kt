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

package net.svaroh.passly.feature.settings.screen.appsettings

import android.security.keystore.KeyPermanentlyInvalidatedException
import net.svaroh.passly.common.BiometricInformationProvider
import net.svaroh.passly.common.autofill.DetectAutofillConflict
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.biometrickey.SaveBiometricKeyIvUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.authenticationcore.passphrase.CheckIfPassphraseFileExistsUseCase
import net.svaroh.passly.core.authenticationcore.passphrase.RemovePassphraseUseCase
import net.svaroh.passly.core.authenticationcore.passphrase.SavePassphraseUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.encryptedstorage.biometric.BiometricCipher
import net.svaroh.passly.feature.authentication.auth.usecase.BiometryInteractor
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.CancelConfigureBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.CancelConfirmKeyChange
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.CancelDisableBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.CanceledBiometricAuth
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ConfigureBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ConfirmDisableBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ConfirmKeyChangeClick
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ErroredBiometricAuth
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.FinalizedBiometricAuth
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.GoBack
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.GoToAutofill
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.GoToDefaultFilter
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.GoToExpertSettings
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.Initialize
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.InvalidateBiometricKeyPermanently
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.RefreshedPassphrase
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ShowBiometryError
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ToggleBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.LaunchBiometricPrompt
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToAutofill
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToDefaultFilter
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToExpertSettings
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToGetPassphrase
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToSystemSettings
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateUp
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.SnackbarKind.AUTHENTICAION_ERROR
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.SnackbarKind.BIOMETRY_ERROR
import net.svaroh.passly.ui.BiometricAuthError
import timber.log.Timber
import javax.crypto.Cipher

internal class AppSettingsViewModel(
    private val checkIfPassphraseExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val biometricInformationProvider: BiometricInformationProvider,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val removePassphraseUseCase: RemovePassphraseUseCase,
    private val biometricCipher: BiometricCipher,
    private val biometryInteractor: BiometryInteractor,
    private val savePassphraseUseCase: SavePassphraseUseCase,
    private val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase,
    private val detectAutofillConflict: DetectAutofillConflict,
) : SideEffectViewModel<AppSettingsState, AppSettingsSideEffect>(AppSettingsState()) {
    init {
        loadInitialValues()
    }

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: AppSettingsIntent) {
        when (intent) {
            Initialize -> initialize()
            GoBack -> emitSideEffect(NavigateUp)
            GoToAutofill -> emitSideEffect(NavigateToAutofill)
            GoToDefaultFilter -> emitSideEffect(NavigateToDefaultFilter)
            GoToExpertSettings -> emitSideEffect(NavigateToExpertSettings)
            ToggleBiometric -> toggleBiometric()
            CancelDisableBiometric -> updateViewState { copy(isDisableBiometricDialogVisible = false) }
            ConfirmDisableBiometric -> disableBiometric()
            CancelConfigureBiometric -> updateViewState { copy(isConfigureBiometricDialogVisible = false) }
            ConfigureBiometric -> {
                updateViewState { copy(isConfigureBiometricDialogVisible = false) }
                emitSideEffect(NavigateToSystemSettings)
            }
            RefreshedPassphrase -> authenticateUsingBiometryPrompt()
            CanceledBiometricAuth -> {}
            is ErroredBiometricAuth -> biometricAuthError(intent.error)
            is FinalizedBiometricAuth -> finalizedBiometricAuth(intent.cipher)
            is InvalidateBiometricKeyPermanently -> invalidateBiometricKey(intent.exception)
            is ShowBiometryError -> biometryShowError(intent.exception)
            CancelConfirmKeyChange -> updateViewState { copy(isKeyChangesDialogDetectedVisible = false) }
            ConfirmKeyChangeClick -> {
                updateViewState { copy(isKeyChangesDialogDetectedVisible = false) }
                emitSideEffect(NavigateToGetPassphrase)
            }
        }
    }

    private fun initialize() {
        val isAutofillConflictDetected = detectAutofillConflict()

        updateViewState {
            copy(
                isAutofillConflictDetected = isAutofillConflictDetected,
            )
        }
    }

    private fun biometryShowError(exception: Exception) {
        Timber.e(exception)
        emitSideEffect(
            AppSettingsSideEffect.ShowErrorSnackbar(
                snackbarKind = BIOMETRY_ERROR,
                exception.message,
            ),
        )
    }

    private fun invalidateBiometricKey(exception: KeyPermanentlyInvalidatedException) {
        Timber.e(exception)
        biometryInteractor.disableBiometry()
        updateViewState { copy(isKeyChangesDialogDetectedVisible = true) }
    }

    private fun biometricAuthError(error: BiometricAuthError) {
        Timber.e("Biometric authentication error: ${error.name}")
        emitSideEffect(
            AppSettingsSideEffect.ShowErrorSnackbar(
                snackbarKind = AUTHENTICAION_ERROR,
                error.name,
            ),
        )
    }

    private fun finalizedBiometricAuth(authenticatedCipher: Cipher?) {
        val passphrase = passphraseMemoryCache.get()
        if (passphrase is PotentialPassphrase.Passphrase && authenticatedCipher != null) {
            savePassphraseUseCase.execute(
                SavePassphraseUseCase.Input(
                    passphrase.passphrase,
                    authenticatedCipher,
                ),
            )
            saveBiometricKeyIvUseCase.execute(
                SaveBiometricKeyIvUseCase.Input(
                    authenticatedCipher.iv,
                ),
            )
            updateViewState { copy(isBiometricEnabled = true) }
        } else {
            Timber.e("Error during turing biometrics on. Passphrase not in cache after auth.")
        }
    }

    private fun loadInitialValues() {
        val passphraseFileExists =
            checkIfPassphraseExistsUseCase
                .execute(
                    UserIdInput(requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)),
                ).passphraseFileExists

        updateViewState {
            copy(
                isBiometricEnabled = passphraseFileExists,
            )
        }
    }

    private fun toggleBiometric() {
        val isEnabled = viewState.value.isBiometricEnabled
        if (isEnabled) {
            updateViewState { copy(isDisableBiometricDialogVisible = true) }
        } else {
            if (biometricInformationProvider.hasBiometricSetUp()) {
                if (passphraseMemoryCache.hasPassphrase()) {
                    authenticateUsingBiometryPrompt()
                } else {
                    emitSideEffect(NavigateToGetPassphrase)
                }
            } else {
                updateViewState { copy(isBiometricEnabled = false, isConfigureBiometricDialogVisible = true) }
            }
        }
    }

    fun authenticateUsingBiometryPrompt() {
        emitSideEffect(LaunchBiometricPrompt(biometricCipher.getBiometricEncryptCipher()))
    }

    fun disableBiometric() {
        val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
        removePassphraseUseCase.execute(UserIdInput(requireNotNull(selectedAccount)))
        updateViewState { copy(isDisableBiometricDialogVisible = false, isBiometricEnabled = false) }
    }
}
