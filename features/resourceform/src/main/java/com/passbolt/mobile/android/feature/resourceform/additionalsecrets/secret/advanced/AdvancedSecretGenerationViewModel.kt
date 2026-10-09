/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced

import kotlinx.coroutines.Job
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_DIGIT
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_EMOJI
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_LOWER
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_PARENTHESIS
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR1
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR2
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR3
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR4
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR5
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_UPPER
import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy
import net.svaroh.passly.core.passwordgenerator.SecretGenerator.SecretGenerationResult.Success
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.GoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PassphraseWordCaseChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PassphraseWordSeparatorChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PassphraseWordsCountChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PasswordCharacterSetToggled
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PasswordExcludeLookAlikeChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PasswordLengthChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PreviewMaskToggled
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.SavePreferences
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.TabSelected
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationSideEffect.ApplyAndGoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationSideEffect.NavigateBack
import net.svaroh.passly.ui.PassphraseGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel.PASSPHRASE
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel.PASSWORD

internal class AdvancedSecretGenerationViewModel(
    initialTab: PasswordGeneratorTypeUiModel,
    initialPasswordSettings: PasswordGeneratorSettingsUiModel,
    initialPassphraseSettings: PassphraseGeneratorSettingsUiModel,
    private val secretGenerator: SecretGenerator,
) : SideEffectViewModel<AdvancedSecretGenerationState, AdvancedSecretGenerationSideEffect>(
        initialState =
            AdvancedSecretGenerationState(
                selectedTab = initialTab,
                passwordSettings = initialPasswordSettings,
                passphraseSettings = initialPassphraseSettings,
            ),
    ) {
    private var previewJob: Job? = null

    init {
        regeneratePreview()
    }

    fun onIntent(intent: AdvancedSecretGenerationIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            SavePreferences -> savePreferences()
            PreviewMaskToggled -> updateViewState { copy(isPreviewMasked = !isPreviewMasked) }
            is TabSelected -> {
                updateViewState { copy(selectedTab = intent.type) }
                regeneratePreview()
            }
            is PasswordLengthChanged -> {
                updateViewState { copy(passwordSettings = passwordSettings.copy(length = intent.length)) }
                regeneratePreview()
            }
            is PasswordCharacterSetToggled -> {
                updateViewState { copy(passwordSettings = passwordSettings.toggleMask(intent.mask)) }
                regeneratePreview()
            }
            is PasswordExcludeLookAlikeChanged -> {
                updateViewState {
                    copy(passwordSettings = passwordSettings.copy(excludeLookAlikeChars = intent.enabled))
                }
                regeneratePreview()
            }
            is PassphraseWordsCountChanged -> {
                updateViewState { copy(passphraseSettings = passphraseSettings.copy(words = intent.count)) }
                regeneratePreview()
            }
            is PassphraseWordSeparatorChanged -> {
                updateViewState {
                    copy(passphraseSettings = passphraseSettings.copy(wordSeparator = intent.separator))
                }
                regeneratePreview()
            }
            is PassphraseWordCaseChanged -> {
                updateViewState { copy(passphraseSettings = passphraseSettings.copy(wordCase = intent.case)) }
                regeneratePreview()
            }
        }
    }

    private fun savePreferences() {
        val state = viewState.value
        if (state.preview.isEmpty()) return
        emitSideEffect(
            ApplyAndGoBack(
                passwordSettings = state.passwordSettings,
                passphraseSettings = state.passphraseSettings,
                selectedTab = state.selectedTab,
                generatedSecret = state.preview,
            ),
        )
    }

    private fun regeneratePreview() {
        previewJob?.cancel()
        previewJob =
            launch {
                val state = viewState.value
                val result =
                    when (state.selectedTab) {
                        PASSWORD -> secretGenerator.generatePassword(state.passwordSettings)
                        PASSPHRASE -> secretGenerator.generatePassphrase(state.passphraseSettings)
                    }
                when (result) {
                    is FailedToGenerateLowEntropy -> {
                        updateViewState {
                            copy(preview = "", minimumEntropyBits = result.minimumEntropyBits)
                        }
                    }
                    is Success -> {
                        val passwordStr =
                            result.password.joinToString("") {
                                String(Character.toChars(it.value))
                            }
                        updateViewState { copy(preview = passwordStr, minimumEntropyBits = null) }
                    }
                }
            }
    }

    private fun PasswordGeneratorSettingsUiModel.toggleMask(mask: String): PasswordGeneratorSettingsUiModel =
        when (mask) {
            MASK_UPPER -> copy(maskUpper = !maskUpper)
            MASK_LOWER -> copy(maskLower = !maskLower)
            MASK_DIGIT -> copy(maskDigit = !maskDigit)
            MASK_PARENTHESIS -> copy(maskParenthesis = !maskParenthesis)
            MASK_SPECIAL_CHAR1 -> copy(maskChar1 = !maskChar1)
            MASK_SPECIAL_CHAR2 -> copy(maskChar2 = !maskChar2)
            MASK_SPECIAL_CHAR3 -> copy(maskChar3 = !maskChar3)
            MASK_SPECIAL_CHAR4 -> copy(maskChar4 = !maskChar4)
            MASK_SPECIAL_CHAR5 -> copy(maskChar5 = !maskChar5)
            MASK_EMOJI -> copy(maskEmoji = !maskEmoji)
            else -> this
        }
}
