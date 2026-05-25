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

package com.passbolt.mobile.android.feature.accessibilitypolicies

import app.cash.turbine.test
import com.passbolt.mobile.android.core.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.feature.accessibilitypolicies.AccessibilityPoliciesIntent.Accept
import com.passbolt.mobile.android.feature.accessibilitypolicies.AccessibilityPoliciesIntent.Decline
import com.passbolt.mobile.android.feature.accessibilitypolicies.AccessibilityPoliciesSideEffect.NavigateToAcceptedScreen
import com.passbolt.mobile.android.feature.accessibilitypolicies.AccessibilityPoliciesSideEffect.NavigateToDeclinedScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AccessibilityPoliciesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val updateGlobalPreferencesUseCase = mock<UpdateGlobalPreferencesUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `accept emits Accepted`() =
        runTest {
            val viewModel = AccessibilityPoliciesViewModel(updateGlobalPreferencesUseCase)

            viewModel.sideEffect.test {
                viewModel.onIntent(Accept)
                assertIs<NavigateToAcceptedScreen>(awaitItem())
            }
        }

    @Test
    fun `accept writes consent flag`() =
        runTest {
            val viewModel = AccessibilityPoliciesViewModel(updateGlobalPreferencesUseCase)

            viewModel.onIntent(Accept)

            verify(updateGlobalPreferencesUseCase).execute(
                UpdateGlobalPreferencesUseCase.Input(accessibilityPoliciesConsentGiven = true),
            )
        }

    @Test
    fun `decline emits Declined`() =
        runTest {
            val viewModel = AccessibilityPoliciesViewModel(updateGlobalPreferencesUseCase)

            viewModel.sideEffect.test {
                viewModel.onIntent(Decline)
                assertIs<NavigateToDeclinedScreen>(awaitItem())
            }
        }

    @Test
    fun `decline does not write consent flag`() =
        runTest {
            val viewModel = AccessibilityPoliciesViewModel(updateGlobalPreferencesUseCase)

            viewModel.onIntent(Decline)

            verify(updateGlobalPreferencesUseCase, never()).execute(any())
        }
}
