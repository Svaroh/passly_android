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

package net.svaroh.passly.feature.resourceform.additionalsecrets.pincode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.core.passwordgenerator.PinCodeGenerator
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormIntent.AdvancedGenerationResult
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormIntent.ApplyChanges
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormIntent.Generate
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormIntent.GoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormIntent.OpenAdvancedGeneration
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormIntent.PinCodeChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormIntent.RemovePinCode
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormSideEffect.ApplyAndGoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormSideEffect.NavigateBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeFormSideEffect.NavigateToAdvancedGeneration
import net.svaroh.passly.ui.LeadingContentType.PIN_CODE
import net.svaroh.passly.ui.PinCodeUiModel
import net.svaroh.passly.ui.ResourceFormMode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class PinCodeFormViewModelTest : KoinTest {
    private val pinCodeGenerator: PinCodeGenerator = mock()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        factory<PinCodeGenerator> { pinCodeGenerator }
                        factory { params ->
                            PinCodeFormViewModel(
                                mode = params.get(),
                                pinCodeUiModel = params.get(),
                                pinCodeGenerator = get(),
                            )
                        }
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `constructor should set initial state from pin code model`() =
        runTest {
            val viewModel =
                get<PinCodeFormViewModel> {
                    parametersOf(resourceFormMode, PinCodeUiModel(pinCode = "1234", length = 6))
                }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.pinCode).isEqualTo("1234")
                assertThat(state.length).isEqualTo(6)
            }
        }

    @Test
    fun `pin code change should filter non-digits and cap at max length`() =
        runTest {
            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }
            viewModel.onIntent(PinCodeChanged("12a3b4567890123456"))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.pinCode).isEqualTo("123456789012")
            }
        }

    @Test
    fun `valid pin code apply should emit ApplyAndGoBack`() =
        runTest {
            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }
            viewModel.onIntent(PinCodeChanged("1234"))

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.pinCodeUiModel?.pinCode).isEqualTo("1234")
            }
        }

    @Test
    fun `pin code too short should not emit ApplyAndGoBack and should add validation error`() =
        runTest {
            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }
            viewModel.onIntent(PinCodeChanged("12"))
            viewModel.onIntent(ApplyChanges)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.validationErrors).isNotEmpty()
                assertIs<PinCodeValidationError.TooShort>(state.validationErrors.first())
            }
        }

    @Test
    fun `pin code too long via initial model should add TooLong validation error on apply`() =
        runTest {
            val viewModel =
                get<PinCodeFormViewModel> {
                    parametersOf(resourceFormMode, PinCodeUiModel(pinCode = "1234567890123", length = 12))
                }
            viewModel.onIntent(ApplyChanges)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.validationErrors).isNotEmpty()
                assertIs<PinCodeValidationError.TooLong>(state.validationErrors.first())
            }
        }

    @Test
    fun `apply with empty pin code should add TooShort validation error`() =
        runTest {
            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }
            viewModel.onIntent(ApplyChanges)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.validationErrors).isNotEmpty()
                assertIs<PinCodeValidationError.TooShort>(state.validationErrors.first())
            }
        }

    @Test
    fun `pin code change should clear existing validation errors`() =
        runTest {
            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }
            viewModel.onIntent(PinCodeChanged("12"))
            viewModel.onIntent(ApplyChanges)
            assertThat(viewModel.viewState.value.validationErrors).isNotEmpty()

            viewModel.onIntent(PinCodeChanged("123"))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.validationErrors).isEmpty()
            }
        }

    @Test
    fun `apply with valid pin after fixing too-short input should emit ApplyAndGoBack`() =
        runTest {
            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }
            viewModel.onIntent(PinCodeChanged("12"))
            viewModel.onIntent(ApplyChanges)
            assertThat(viewModel.viewState.value.validationErrors).isNotEmpty()

            viewModel.onIntent(PinCodeChanged("1234"))

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.pinCodeUiModel?.pinCode).isEqualTo("1234")
            }
            assertThat(viewModel.viewState.value.validationErrors).isEmpty()
        }

    @Test
    fun `generate should clear validation errors`() =
        runTest {
            whenever(pinCodeGenerator.generate(4)).thenReturn("5678")

            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }
            viewModel.onIntent(PinCodeChanged("12"))
            viewModel.onIntent(ApplyChanges)
            assertThat(viewModel.viewState.value.validationErrors).isNotEmpty()

            viewModel.onIntent(Generate)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.pinCode).isEqualTo("5678")
                assertThat(state.validationErrors).isEmpty()
            }
        }

    @Test
    fun `generate intent should produce a pin from the generator`() =
        runTest {
            whenever(pinCodeGenerator.generate(4)).thenReturn("9876")

            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }
            viewModel.onIntent(Generate)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.pinCode).isEqualTo("9876")
            }
        }

    @Test
    fun `advanced generation result should regenerate pin with the new length`() =
        runTest {
            whenever(pinCodeGenerator.generate(8)).thenReturn("12345678")

            val viewModel =
                get<PinCodeFormViewModel> {
                    parametersOf(resourceFormMode, PinCodeUiModel(pinCode = "1234", length = 4))
                }
            viewModel.onIntent(AdvancedGenerationResult(PinCodeUiModel(pinCode = "1234", length = 8)))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.pinCode).isEqualTo("12345678")
                assertThat(state.length).isEqualTo(8)
            }
        }

    @Test
    fun `open advanced generation should emit NavigateToAdvancedGeneration`() =
        runTest {
            val viewModel =
                get<PinCodeFormViewModel> {
                    parametersOf(resourceFormMode, PinCodeUiModel(pinCode = "1234", length = 8))
                }

            viewModel.sideEffect.test {
                viewModel.onIntent(OpenAdvancedGeneration)
                val sideEffect = awaitItem()
                assertIs<NavigateToAdvancedGeneration>(sideEffect)
                assertThat(sideEffect.pinCodeUiModel.length).isEqualTo(8)
                assertThat(sideEffect.pinCodeUiModel.pinCode).isEqualTo("1234")
            }
        }

    @Test
    fun `remove pin code should emit ApplyAndGoBack with null`() =
        runTest {
            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }

            viewModel.sideEffect.test {
                viewModel.onIntent(RemovePinCode)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.pinCodeUiModel).isNull()
            }
        }

    @Test
    fun `go back should emit NavigateBack`() =
        runTest {
            val viewModel = get<PinCodeFormViewModel> { parametersOf(resourceFormMode, PinCodeUiModel.empty()) }

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    private companion object {
        val resourceFormMode =
            ResourceFormMode.Create(
                leadingContentType = PIN_CODE,
                parentFolderId = null,
            )
    }
}
