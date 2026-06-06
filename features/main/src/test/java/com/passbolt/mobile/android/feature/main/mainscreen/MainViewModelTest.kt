package net.svaroh.passly.feature.main.mainscreen

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.common.datarefresh.DataRefreshStatus
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.autofill.AutofillInformationProvider
import net.svaroh.passly.core.inappreview.InAppReviewInteractor
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.deeplink.BrowserFirstLoginDeepLinkStore
import net.svaroh.passly.entity.featureflags.FeatureFlagsModel
import net.svaroh.passly.feature.main.mainscreen.MainSideEffect.CheckForAppUpdates
import net.svaroh.passly.feature.main.mainscreen.MainSideEffect.PerformFullDataRefresh
import net.svaroh.passly.feature.main.mainscreen.MainSideEffect.TryLaunchReviewFlow
import net.svaroh.passly.feature.main.mainscreen.encouragements.EncouragementsInteractor
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginQrParser
import net.svaroh.passly.feature.transferaccounttoanotherdevice.usecase.CompleteBrowserFirstLoginUseCase
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

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

private val mockInAppReviewInteractor = mock<InAppReviewInteractor>()
private val mockEncouragementsInteractor = mock<EncouragementsInteractor>()
private val mockAutofillInformationProvider = mock<AutofillInformationProvider>()
private val mockAppNavigator =
    mock<AppNavigator> {
        on { tabSwitchRequest } doReturn MutableSharedFlow()
    }

private val defaultFeatureFlags =
    FeatureFlagsModel(
        privacyPolicyUrl = null,
        termsAndConditionsUrl = null,
        isPreviewPasswordAvailable = false,
        areFoldersAvailable = false,
        areTagsAvailable = false,
        isTotpAvailable = false,
        isRbacAvailable = false,
        isPasswordExpiryAvailable = false,
        arePasswordPoliciesAvailable = false,
        canUpdatePasswordPolicies = false,
        isV5MetadataAvailable = false,
    )

private val mockGetFeatureFlagsUseCase =
    mock<GetFeatureFlagsUseCase> {
        onBlocking { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(defaultFeatureFlags)
    }
private val mockBrowserFirstLoginQrParser = mock<BrowserFirstLoginQrParser>()
private val mockCompleteBrowserFirstLoginUseCase = mock<CompleteBrowserFirstLoginUseCase>()

private val testMainModule =
    module {
        factory {
            MainViewModel(
                inAppReviewInteractor = mockInAppReviewInteractor,
                dataRefreshTrackingFlow = get(),
                getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
                encouragementsInteractor = mockEncouragementsInteractor,
                autofillInformationProvider = mockAutofillInformationProvider,
                appNavigator = mockAppNavigator,
                browserFirstLoginDeepLinkStore = BrowserFirstLoginDeepLinkStore(),
                browserFirstLoginQrParser = mockBrowserFirstLoginQrParser,
                completeBrowserFirstLoginUseCase = mockCompleteBrowserFirstLoginUseCase,
            )
        }
        singleOf(::DataRefreshTrackingFlow)
    }

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testMainModule)
        }

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `full data refresh should start on init`() =
        runTest {
            val viewModel: MainViewModel = get()

            viewModel.sideEffect.filterIsInstance<PerformFullDataRefresh>().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `app update check should start on init`() =
        runTest {
            val viewModel: MainViewModel = get()

            viewModel.sideEffect.filterIsInstance<CheckForAppUpdates>().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `chrome native autofill should be shown when needed`() =
        runTest {
            whenever(mockEncouragementsInteractor.shouldShowChromeNativeAutofillEncouragement())
                .doReturn(true)

            val viewModel: MainViewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().showChromeNativeAutofillDialog).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `in app review flow should be shown when needed`() =
        runTest {
            whenever(mockInAppReviewInteractor.shouldShowInAppReviewFlow())
                .doReturn(true)

            val viewModel: MainViewModel = get()

            viewModel.sideEffect.filterIsInstance<TryLaunchReviewFlow>().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `totp should be visible based on feature flag`() =
        runTest {
            mockGetFeatureFlagsUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetFeatureFlagsUseCase.Output(
                        FeatureFlagsModel(
                            privacyPolicyUrl = null,
                            termsAndConditionsUrl = null,
                            isPreviewPasswordAvailable = false,
                            areFoldersAvailable = true,
                            areTagsAvailable = true,
                            isTotpAvailable = true,
                            isRbacAvailable = true,
                            isPasswordExpiryAvailable = true,
                            arePasswordPoliciesAvailable = true,
                            canUpdatePasswordPolicies = true,
                            isV5MetadataAvailable = false,
                        ),
                    )
            }

            val viewModel: MainViewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().bottomNavigationModel?.isOtpTabVisible).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `bottom nav should update after data refresh completes`() =
        runTest {
            mockGetFeatureFlagsUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetFeatureFlagsUseCase.Output(
                        FeatureFlagsModel(
                            privacyPolicyUrl = null,
                            termsAndConditionsUrl = null,
                            isPreviewPasswordAvailable = false,
                            areFoldersAvailable = true,
                            areTagsAvailable = true,
                            isTotpAvailable = true,
                            isRbacAvailable = true,
                            isPasswordExpiryAvailable = true,
                            arePasswordPoliciesAvailable = true,
                            canUpdatePasswordPolicies = true,
                            isV5MetadataAvailable = false,
                        ),
                    )
            }

            val viewModel: MainViewModel = get()
            get<DataRefreshTrackingFlow>().updateStatus(DataRefreshStatus.Idle.FinishedWithSuccess)

            viewModel.viewState.test {
                assertThat(awaitItem().bottomNavigationModel?.isOtpTabVisible).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
