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

package net.svaroh.passly.feature.resources.details

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.core.otpcore.TotpParametersProvider
import net.svaroh.passly.core.otpcore.TotpParametersProvider.OtpParametersResult
import net.svaroh.passly.core.resources.actions.SecretPropertiesActionsInteractor
import net.svaroh.passly.core.resources.actions.SecretPropertyActionResult
import net.svaroh.passly.feature.resourcedetails.details.ErrorSnackbarType
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CopyTotp
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.Initialize
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.ToggleTotpVisibility
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.AddToClipboard
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsViewModel
import net.svaroh.passly.jsonmodel.delegates.TotpSecret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceDetailsTotpViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testModule)
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ResourceDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getKoin().setupDefaultMocks()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggle totp visibility should show totp when hidden`() =
        runTest {
            val totpSecret =
                TotpSecret(
                    algorithm = "SHA1",
                    key = "JBSWY3DPEHPK3PXP",
                    digits = 6,
                    period = 30L,
                )
            val otpValue = "123456"

            val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get()
            secretPropertiesActionsInteractor.stub {
                onBlocking { provideOtp() } doReturn
                    flowOf(
                        SecretPropertyActionResult.Success(
                            SecretPropertiesActionsInteractor.SECRET_LABEL,
                            isSecret = true,
                            totpSecret,
                        ),
                    )
            }

            val totpParametersProvider: TotpParametersProvider = get()
            totpParametersProvider.stub {
                onBlocking { provideOtpParameters(any(), any(), any(), any()) } doReturn
                    OtpParametersResult.OtpParameters(otpValue, secondsValid = 25)
            }

            viewModel = get()
            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ToggleTotpVisibility)

                val state = expectMostRecentItem()
                assertThat(state.totpData.totpModel?.isVisible).isTrue()
                assertThat(state.totpData.totpModel?.otpValue).isEqualTo(otpValue)
            }
        }

    @Test
    fun `copy totp should emit add to clipboard side effect`() =
        runTest {
            val totpSecret =
                TotpSecret(
                    algorithm = "SHA1",
                    key = "JBSWY3DPEHPK3PXP",
                    digits = 6,
                    period = 30L,
                )
            val otpValue = "123456"

            val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get()
            secretPropertiesActionsInteractor.stub {
                onBlocking { provideOtp() } doReturn
                    flowOf(
                        SecretPropertyActionResult.Success(
                            SecretPropertiesActionsInteractor.SECRET_LABEL,
                            isSecret = true,
                            totpSecret,
                        ),
                    )
            }

            val totpParametersProvider: TotpParametersProvider = get()
            totpParametersProvider.stub {
                onBlocking { provideOtpParameters(any(), any(), any(), any()) } doReturn
                    OtpParametersResult.OtpParameters(otpValue, secondsValid = 25)
            }

            viewModel = get()
            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

            viewModel.sideEffect.test {
                viewModel.onIntent(CopyTotp)

                val effect = awaitItem()
                assertIs<AddToClipboard>(effect)
                assertThat(effect.value).isEqualTo(otpValue)
                assertThat(effect.isSecret).isTrue()
            }
        }

    @Test
    fun `totp decryption failure should show error snackbar`() =
        runTest {
            val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get()
            secretPropertiesActionsInteractor.stub {
                onBlocking { provideOtp() } doReturn flowOf(SecretPropertyActionResult.DecryptionFailure())
            }

            viewModel = get()
            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

            viewModel.sideEffect.test {
                viewModel.onIntent(ToggleTotpVisibility)

                val effect = awaitItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(ErrorSnackbarType.DECRYPTION_FAILURE)
            }
        }
}
