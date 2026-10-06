package net.svaroh.passly.feature.resourceform.main.resourcemodelhandler.v5.leadingpincode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.domain.resources.usecase.GetDefaultCreateContentTypeUseCase
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GeneratePinCode
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToPinCodeAdvancedGeneration
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.NameTextChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.NoteChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PinCodeAdvancedGenerationResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PinCodeChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PinCodeResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToPinCodeAdvancedGeneration
import net.svaroh.passly.feature.resourceform.main.ResourceFormViewModel
import net.svaroh.passly.feature.resourceform.main.ResourceModelHandler
import net.svaroh.passly.feature.resourceform.main.mockGetDefaultCreateContentTypeUseCase
import net.svaroh.passly.feature.resourceform.main.mockPinCodeGenerator
import net.svaroh.passly.feature.resourceform.main.testResourceFormModule
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.ui.LeadingContentType
import net.svaroh.passly.ui.MetadataTypeModel
import net.svaroh.passly.ui.PinCodeUiModel
import net.svaroh.passly.ui.ResourceFormMode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertIs

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

@OptIn(ExperimentalCoroutinesApi::class)
class V5PinCodeResourceFormViewModelTest : KoinTest {
    private lateinit var viewModel: ResourceFormViewModel
    private val resourceModelHandler: ResourceModelHandler by inject()

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testResourceFormModule)
        }

    @Before
    fun setUp() =
        runTest {
            Dispatchers.setMain(testDispatcher)
            mockGetDefaultCreateContentTypeUseCase.stub {
                on { execute(any()) }.thenReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        metadataType = MetadataTypeModel.V5,
                        contentType = ContentType.V5PinCodeStandalone,
                    ),
                )
            }

            viewModel = get { parametersOf(mode) }
            testScheduler.advanceUntilIdle()
        }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `leading content type pin code should initialize empty pin code model`() =
        runTest {
            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5PinCodeStandalone)
            assertThat(resourceModelHandler.metadataType).isEqualTo(MetadataTypeModel.V5)

            JSONAssert.assertEquals(
                """
                {
                    "name": ""
                }
                """.trimIndent(),
                resourceModelHandler.resourceMetadata.json,
                STRICT_MODE_ENABLED,
            )
            JSONAssert.assertEquals(
                """
                {
                    "pin_code": ""
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )

            val state = viewModel.viewState.value
            assertThat(state.leadingContentType).isEqualTo(LeadingContentType.PIN_CODE)
            assertThat(state.pinCodeData.pinCode).isEqualTo("")
        }

    @Test
    fun `pin code change should update state and secret`() =
        runTest {
            viewModel.onIntent(PinCodeChanged("4242"))
            advanceUntilIdle()

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5PinCodeStandalone)
            assertThat(viewModel.viewState.value.pinCodeData.pinCode).isEqualTo("4242")
            JSONAssert.assertEquals(
                """
                {
                    "pin_code": "4242",
                    "description": ""
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `pin code change to blank should transition to v5-note`() =
        runTest {
            viewModel.onIntent(PinCodeChanged("1234"))
            advanceUntilIdle()
            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5PinCodeStandalone)

            viewModel.onIntent(PinCodeChanged(""))
            advanceUntilIdle()

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5Note)
            assertThat(resourceModelHandler.resourceSecret.pinCode).isNull()
            assertThat(resourceModelHandler.resourceSecret.description).isEqualTo("")
        }

    @Test
    fun `generate pin code should update state and secret with generator output`() =
        runTest {
            whenever(mockPinCodeGenerator.generate(PinCodeUiModel.DEFAULT_LENGTH)).thenReturn("4242")

            viewModel.onIntent(GeneratePinCode)
            advanceUntilIdle()

            assertThat(viewModel.viewState.value.pinCodeData.pinCode).isEqualTo("4242")
            JSONAssert.assertEquals(
                """
                {
                    "pin_code": "4242",
                    "description": ""
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `pin code advanced generation result should regenerate with new length`() =
        runTest {
            whenever(mockPinCodeGenerator.generate(8)).thenReturn("12345678")

            viewModel.onIntent(PinCodeAdvancedGenerationResult(PinCodeUiModel(pinCode = "0000", length = 8)))
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertThat(state.pinCodeData.pinCode).isEqualTo("12345678")
            assertThat(state.pinCodeData.length).isEqualTo(8)
        }

    @Test
    fun `pin code result with valid model should apply pin to secret`() =
        runTest {
            viewModel.onIntent(PinCodeResult(PinCodeUiModel(pinCode = "9876", length = 4)))
            advanceUntilIdle()

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5PinCodeStandalone)
            assertThat(viewModel.viewState.value.pinCodeData.pinCode).isEqualTo("9876")
            JSONAssert.assertEquals(
                """
                {
                    "pin_code": "9876",
                    "description": ""
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `pin code result with null should transition to v5-note`() =
        runTest {
            viewModel.onIntent(PinCodeChanged("1234"))
            advanceUntilIdle()
            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5PinCodeStandalone)

            viewModel.onIntent(PinCodeResult(null))
            advanceUntilIdle()

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5Note)
            assertThat(viewModel.viewState.value.pinCodeData.pinCode).isEqualTo("")
            assertThat(resourceModelHandler.resourceSecret.pinCode).isNull()
        }

    @Test
    fun `go to pin code advanced generation should emit navigate side effect`() =
        runTest {
            viewModel.onIntent(PinCodeChanged("1234"))
            advanceUntilIdle()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToPinCodeAdvancedGeneration)
                advanceUntilIdle()
                val sideEffect = awaitItem()
                assertIs<NavigateToPinCodeAdvancedGeneration>(sideEffect)
                assertThat(sideEffect.pinCodeUiModel.pinCode).isEqualTo("1234")
            }
        }

    @Test
    fun `add note should not change content type and apply changes`() =
        runTest {
            val mockNote = "secret note"
            viewModel.onIntent(PinCodeChanged("1234"))
            viewModel.onIntent(NoteChanged(mockNote))
            advanceUntilIdle()

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5PinCodeStandalone)
            JSONAssert.assertEquals(
                """
                {
                    "pin_code": "1234",
                    "description": "$mockNote"
                }
                """.trimIndent(),
                resourceModelHandler.resourceSecret.json,
                STRICT_MODE_ENABLED,
            )
        }

    @Test
    fun `edit metadata name should not change content type`() =
        runTest {
            val mockName = "Wifi PIN"
            viewModel.onIntent(NameTextChanged(mockName))
            advanceUntilIdle()

            assertThat(resourceModelHandler.contentType).isEqualTo(ContentType.V5PinCodeStandalone)
            JSONAssert.assertEquals(
                """
                {
                    "name": "$mockName"
                }
                """.trimIndent(),
                resourceModelHandler.resourceMetadata.json,
                STRICT_MODE_ENABLED,
            )
        }

    private companion object {
        private const val STRICT_MODE_ENABLED = true

        private val mode =
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PIN_CODE,
                parentFolderId = null,
            )
    }
}
