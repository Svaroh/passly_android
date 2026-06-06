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

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_UPPER
import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.codepoints.Codepoint
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
import net.svaroh.passly.ui.CaseTypeUiModel
import net.svaroh.passly.ui.PassphraseGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.stub
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedSecretGenerationViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testAdvancedSecretGenerationModule)
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockSecretGenerator.stub {
            on { generatePassword(any()) }.thenReturn(
                SecretGenerator.SecretGenerationResult.Success(
                    password = "pwd1!".map { Codepoint(it.code) },
                    entropy = 100.0,
                ),
            )
            on { generatePassphrase(any()) }.thenReturn(
                SecretGenerator.SecretGenerationResult.Success(
                    password = "phrase".map { Codepoint(it.code) },
                    entropy = 100.0,
                ),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state contains seeded settings and generated preview`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.selectedTab).isEqualTo(PasswordGeneratorTypeUiModel.PASSWORD)
            assertThat(state.passwordSettings).isEqualTo(passwordSettings)
            assertThat(state.passphraseSettings).isEqualTo(passphraseSettings)
            assertThat(state.preview).isEqualTo("pwd1!")
            assertThat(state.minimumEntropyBits).isNull()
        }

    @Test
    fun `preview is unmasked by default and toggles on intent`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.viewState.test {
                assertThat(awaitItem().isPreviewMasked).isFalse()

                viewModel.onIntent(PreviewMaskToggled)
                assertThat(awaitItem().isPreviewMasked).isTrue()

                viewModel.onIntent(PreviewMaskToggled)
                assertThat(awaitItem().isPreviewMasked).isFalse()
            }
        }

    @Test
    fun `tab selected switches tab and regenerates preview from passphrase generator`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(TabSelected(PasswordGeneratorTypeUiModel.PASSPHRASE))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.selectedTab).isEqualTo(PasswordGeneratorTypeUiModel.PASSPHRASE)
            assertThat(state.preview).isEqualTo("phrase")
        }

    @Test
    fun `password length changed updates settings`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(PasswordLengthChanged(32))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.passwordSettings.length).isEqualTo(32)
        }

    @Test
    fun `password character set toggled flips the matching mask`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val before = viewModel.viewState.value.passwordSettings.maskUpper

            viewModel.onIntent(PasswordCharacterSetToggled(MASK_UPPER))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.passwordSettings.maskUpper).isEqualTo(!before)
        }

    @Test
    fun `exclude look alike changed updates settings`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(PasswordExcludeLookAlikeChanged(false))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.passwordSettings.excludeLookAlikeChars).isFalse()
        }

    @Test
    fun `passphrase words count changed updates settings`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(PassphraseWordsCountChanged(12))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.passphraseSettings.words).isEqualTo(12)
        }

    @Test
    fun `passphrase word separator changed updates settings`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(PassphraseWordSeparatorChanged("-"))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.passphraseSettings.wordSeparator).isEqualTo("-")
        }

    @Test
    fun `passphrase word case changed updates settings`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(PassphraseWordCaseChanged(CaseTypeUiModel.UPPERCASE))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.passphraseSettings.wordCase).isEqualTo(CaseTypeUiModel.UPPERCASE)
        }

    @Test
    fun `low entropy generation result clears preview and exposes minimum entropy`() =
        runTest {
            mockSecretGenerator.stub {
                on { generatePassword(any()) }.thenReturn(
                    SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy(80),
                )
            }

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.preview).isEmpty()
            assertThat(state.minimumEntropyBits).isEqualTo(80)
        }

    @Test
    fun `save preferences emits apply and go back when preview is present`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(SavePreferences)
                advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.generatedSecret).isEqualTo("pwd1!")
                assertThat(sideEffect.selectedTab).isEqualTo(PasswordGeneratorTypeUiModel.PASSWORD)
                assertThat(sideEffect.passwordSettings).isEqualTo(passwordSettings)
                assertThat(sideEffect.passphraseSettings).isEqualTo(passphraseSettings)
            }
        }

    @Test
    fun `save preferences does not emit when preview is empty`() =
        runTest {
            mockSecretGenerator.stub {
                on { generatePassword(any()) }.thenReturn(
                    SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy(80),
                )
            }

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(SavePreferences)
                advanceUntilIdle()
                expectNoEvents()
            }
        }

    @Test
    fun `go back emits navigate back side effect`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                advanceUntilIdle()
                assertIs<NavigateBack>(awaitItem())
            }
        }

    private fun createViewModel(): AdvancedSecretGenerationViewModel =
        get {
            parametersOf(
                PasswordGeneratorTypeUiModel.PASSWORD,
                passwordSettings,
                passphraseSettings,
            )
        }

    private companion object {
        val passwordSettings =
            PasswordGeneratorSettingsUiModel(
                length = 16,
                maskUpper = true,
                maskLower = true,
                maskDigit = true,
                maskParenthesis = false,
                maskEmoji = false,
                maskChar1 = true,
                maskChar2 = false,
                maskChar3 = false,
                maskChar4 = false,
                maskChar5 = false,
                excludeLookAlikeChars = true,
            )

        val passphraseSettings =
            PassphraseGeneratorSettingsUiModel(
                words = 8,
                wordSeparator = " ",
                wordCase = CaseTypeUiModel.LOWERCASE,
            )
    }
}
