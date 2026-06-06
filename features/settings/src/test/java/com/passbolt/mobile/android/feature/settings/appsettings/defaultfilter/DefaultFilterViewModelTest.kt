package net.svaroh.passly.feature.settings.appsettings.defaultfilter

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.preferences.HomeDisplayViewPreferencesUpdate
import net.svaroh.passly.domain.preferences.usecase.GetAvailableDefaultFiltersUseCase
import net.svaroh.passly.domain.preferences.usecase.GetHomeDisplayViewPreferencesUseCase
import net.svaroh.passly.domain.preferences.usecase.UpdateHomeDisplayViewPreferencesUseCase
import net.svaroh.passly.feature.settings.screen.appsettings.defaultfilter.DefaultFilterIntent.SelectDefaultFilter
import net.svaroh.passly.feature.settings.screen.appsettings.defaultfilter.DefaultFilterViewModel
import net.svaroh.passly.ui.DefaultFilterUiModel
import net.svaroh.passly.ui.DefaultFilterUiModel.EXPIRY
import net.svaroh.passly.ui.DefaultFilterUiModel.FAVOURITES
import net.svaroh.passly.ui.HomeDisplayViewPreferencesUiModel
import net.svaroh.passly.ui.HomeDisplayViewUiModel.ALL_ITEMS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
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

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFilterViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetAvailableDefaultFiltersUseCase>() }
                        single { mock<GetHomeDisplayViewPreferencesUseCase>() }
                        single { mock<UpdateHomeDisplayViewPreferencesUseCase>() }
                        factoryOf(::DefaultFilterViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: DefaultFilterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should show validated filter list and selected filter initially`() {
        whenever(get<GetAvailableDefaultFiltersUseCase>().execute(Unit)) doReturn DefaultFilterUiModel.entries
        whenever(get<GetHomeDisplayViewPreferencesUseCase>().execute(Unit)) doReturn
            HomeDisplayViewPreferencesUiModel(
                lastUsedHomeView = ALL_ITEMS,
                userSetHomeView = EXPIRY,
            )

        viewModel = get()

        assertThat(viewModel.viewState.value.allFilters).containsExactlyElementsIn(DefaultFilterUiModel.entries)
        assertThat(viewModel.viewState.value.selectedFilter).isEqualTo(EXPIRY)
    }

    @Test
    fun `selected filter should be updated`() {
        whenever(get<GetAvailableDefaultFiltersUseCase>().execute(Unit)) doReturn DefaultFilterUiModel.entries
        whenever(get<GetHomeDisplayViewPreferencesUseCase>().execute(Unit)) doReturn
            HomeDisplayViewPreferencesUiModel(
                lastUsedHomeView = ALL_ITEMS,
                userSetHomeView = EXPIRY,
            )

        viewModel = get()
        viewModel.onIntent(SelectDefaultFilter(FAVOURITES))

        assertThat(viewModel.viewState.value.allFilters).containsExactlyElementsIn(DefaultFilterUiModel.entries)
        assertThat(viewModel.viewState.value.selectedFilter).isEqualTo(FAVOURITES)
        argumentCaptor<HomeDisplayViewPreferencesUpdate> {
            verify(get<UpdateHomeDisplayViewPreferencesUseCase>()).execute(capture())
            assertThat(firstValue.userSetHomeView).isEqualTo(FAVOURITES)
        }
    }
}
