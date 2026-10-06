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

package net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormIntent.GoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormIntent.LengthChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormIntent.SavePreferences
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormSideEffect.ApplyAndGoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormSideEffect.NavigateBack
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class PinCodeAdvancedGenerationFormViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        factory { params ->
                            PinCodeAdvancedGenerationFormViewModel(
                                mode = params.get(),
                                pinCodeUiModel = params.get(),
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
    fun `constructor should set initial state with length`() =
        runTest {
            val viewModel =
                get<PinCodeAdvancedGenerationFormViewModel> {
                    parametersOf(resourceFormMode, PinCodeUiModel(pinCode = "1234", length = 6))
                }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.length).isEqualTo(6)
            }
        }

    @Test
    fun `length below min should be coerced to MIN_LENGTH`() =
        runTest {
            val viewModel =
                get<PinCodeAdvancedGenerationFormViewModel> {
                    parametersOf(resourceFormMode, PinCodeUiModel(pinCode = "", length = 4))
                }
            viewModel.onIntent(LengthChanged(2))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.length).isEqualTo(PinCodeUiModel.MIN_LENGTH)
            }
        }

    @Test
    fun `length above max should be coerced to MAX_LENGTH`() =
        runTest {
            val viewModel =
                get<PinCodeAdvancedGenerationFormViewModel> {
                    parametersOf(resourceFormMode, PinCodeUiModel(pinCode = "", length = 4))
                }
            viewModel.onIntent(LengthChanged(99))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.length).isEqualTo(PinCodeUiModel.MAX_LENGTH)
            }
        }

    @Test
    fun `save preferences should emit ApplyAndGoBack with updated length`() =
        runTest {
            val viewModel =
                get<PinCodeAdvancedGenerationFormViewModel> {
                    parametersOf(resourceFormMode, PinCodeUiModel(pinCode = "1234", length = 4))
                }
            viewModel.onIntent(LengthChanged(8))

            viewModel.sideEffect.test {
                viewModel.onIntent(SavePreferences)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.pinCodeUiModel.length).isEqualTo(8)
                assertThat(sideEffect.pinCodeUiModel.pinCode).isEqualTo("1234")
            }
        }

    @Test
    fun `go back should emit NavigateBack`() =
        runTest {
            val viewModel =
                get<PinCodeAdvancedGenerationFormViewModel> {
                    parametersOf(resourceFormMode, PinCodeUiModel.empty())
                }

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
