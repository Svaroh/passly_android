package net.svaroh.passly.feature.resourceform.additionalsecrets.password

import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy
import net.svaroh.passly.core.passwordgenerator.SecretGenerator.SecretGenerationResult.Success
import net.svaroh.passly.core.passwordgenerator.entropy.EntropyCalculator
import net.svaroh.passly.core.passwordgenerator.entropy.toPasswordStrength
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.AdvancedSecretGenerationResult
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.ApplyChanges
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.DismissUnableToGeneratePassword
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.GeneratePassword
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.GoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.MainUriTextChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.OpenAdvancedSecretGeneration
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.PasswordTextChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.UsernameTextChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.ApplyAndGoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.NavigateBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.NavigateToAdvancedSecretGeneration
import net.svaroh.passly.feature.resourceform.main.GeneratorSettings
import net.svaroh.passly.feature.resourceform.main.GetOrLoadGeneratorSettingsUseCase
import net.svaroh.passly.feature.resourceform.main.GetOrLoadGeneratorSettingsUseCase.Input
import net.svaroh.passly.feature.resourceform.navigation.AdvancedSecretGenerationFormResult
import net.svaroh.passly.ui.Entropy
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel.PASSPHRASE
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel.PASSWORD
import net.svaroh.passly.ui.PasswordUiModel
import net.svaroh.passly.ui.ResourceFormMode

internal class PasswordFormViewModel(
    mode: ResourceFormMode,
    passwordModel: PasswordUiModel,
    private val entropyCalculator: EntropyCalculator,
    private val getOrLoadGeneratorSettingsUseCase: GetOrLoadGeneratorSettingsUseCase,
    private val secretGenerator: SecretGenerator,
) : SideEffectViewModel<PasswordFormState, PasswordFormSideEffect>(PasswordFormState()) {
    init {
        updateViewState {
            copy(
                resourceFormMode = mode,
                password = passwordModel.password,
                mainUri = passwordModel.mainUri,
                username = passwordModel.username,
            )
        }
        launch {
            val entropy = entropyCalculator.getSecretEntropy(passwordModel.password)
            updateViewState {
                copy(
                    entropy = entropy,
                    passwordStrength = Entropy.parse(entropy).toPasswordStrength(),
                )
            }
        }
    }

    fun onIntent(intent: PasswordFormIntent) {
        when (intent) {
            is PasswordTextChanged -> passwordTextChanged(intent.password)
            is MainUriTextChanged -> updateViewState { copy(mainUri = intent.mainUri) }
            is UsernameTextChanged -> updateViewState { copy(username = intent.username) }
            GeneratePassword -> generatePassword()
            OpenAdvancedSecretGeneration -> openAdvancedSecretGeneration()
            is AdvancedSecretGenerationResult -> advancedSecretGenerationResult(intent.result)
            ApplyChanges -> applyChanges()
            GoBack -> emitSideEffect(NavigateBack)
            DismissUnableToGeneratePassword ->
                updateViewState { copy(isUnableToGeneratePasswordDialogVisible = false) }
        }
    }

    private fun passwordTextChanged(password: String) {
        updateViewState { copy(password = password) }
        launch {
            val entropy = entropyCalculator.getSecretEntropy(password)
            updateViewState {
                copy(
                    entropy = entropy,
                    passwordStrength = Entropy.parse(entropy).toPasswordStrength(),
                )
            }
        }
    }

    private fun generatePassword() {
        launch {
            val (type, passwordSettings, passphraseSettings) = getOrLoadGeneratorSettings()
            val result =
                when (type) {
                    PASSWORD -> secretGenerator.generatePassword(passwordSettings)
                    PASSPHRASE -> secretGenerator.generatePassphrase(passphraseSettings)
                }

            when (result) {
                is FailedToGenerateLowEntropy ->
                    updateViewState {
                        copy(
                            isUnableToGeneratePasswordDialogVisible = true,
                            minimumEntropyBits = result.minimumEntropyBits,
                        )
                    }
                is Success -> {
                    val passwordString =
                        buildString {
                            result.password.forEach { append(Character.toChars(it.value)) }
                        }
                    val strength = Entropy.parse(result.entropy).toPasswordStrength()
                    updateViewState {
                        copy(
                            password = passwordString,
                            entropy = result.entropy,
                            passwordStrength = strength,
                        )
                    }
                }
            }
        }
    }

    private fun openAdvancedSecretGeneration() {
        launch {
            val (type, passwordSettings, passphraseSettings) = getOrLoadGeneratorSettings()
            emitSideEffect(
                NavigateToAdvancedSecretGeneration(
                    selectedTab = type,
                    passwordSettings = passwordSettings,
                    passphraseSettings = passphraseSettings,
                ),
            )
        }
    }

    private fun advancedSecretGenerationResult(result: AdvancedSecretGenerationFormResult) {
        updateViewState {
            copy(
                generatorType = result.selectedTab,
                passwordGeneratorSettings = result.passwordSettings,
                passphraseGeneratorSettings = result.passphraseSettings,
            )
        }
        launch {
            val entropy = entropyCalculator.getSecretEntropy(result.generatedSecret)
            updateViewState {
                copy(
                    password = result.generatedSecret,
                    entropy = entropy,
                    passwordStrength = Entropy.parse(entropy).toPasswordStrength(),
                )
            }
        }
    }

    private suspend fun getOrLoadGeneratorSettings(): GeneratorSettings {
        val state = viewState.value
        val (settings, wasLoaded) =
            getOrLoadGeneratorSettingsUseCase.execute(
                Input(
                    type = state.generatorType,
                    passwordSettings = state.passwordGeneratorSettings,
                    passphraseSettings = state.passphraseGeneratorSettings,
                ),
            )
        if (wasLoaded) {
            updateViewState {
                copy(
                    generatorType = settings.type,
                    passwordGeneratorSettings = settings.passwordSettings,
                    passphraseGeneratorSettings = settings.passphraseSettings,
                )
            }
        }
        return settings
    }

    private fun applyChanges() {
        val state = viewState.value
        emitSideEffect(
            ApplyAndGoBack(
                PasswordUiModel(
                    password = state.password,
                    mainUri = state.mainUri,
                    username = state.username,
                ),
            ),
        )
    }
}
