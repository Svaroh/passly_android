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

package net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize

import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.domain.preferences.GlobalPreferencesUpdate
import net.svaroh.passly.domain.preferences.usecase.GetAutomaticPageSizeUseCase
import net.svaroh.passly.domain.preferences.usecase.GetGlobalPreferencesUseCase
import net.svaroh.passly.domain.preferences.usecase.UpdateGlobalPreferencesUseCase
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.GoBack
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.PageSizeChanged
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.RestoreDefaultsClick
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.SaveClick
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeSideEffect.NavigateBack

internal class PageSizeViewModel(
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
    private val getAutomaticPageSizeUseCase: GetAutomaticPageSizeUseCase,
) : SideEffectViewModel<PageSizeState, PageSizeSideEffect>(PageSizeState()) {
    init {
        loadInitialValues()
    }

    fun onIntent(intent: PageSizeIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            SaveClick -> save()
            RestoreDefaultsClick -> restoreDefaults()
            is PageSizeChanged -> pageSizeChanged(intent.sliderIndex)
        }
    }

    private fun loadInitialValues() {
        val currentPageSize = getGlobalPreferencesUseCase.execute(Unit).apiFetchPageSize
        val automaticPageSize = getAutomaticPageSizeUseCase.execute(Unit)
        val currentIndex = allowedIndexOf(currentPageSize)
        updateViewState {
            copy(
                selectedIndex = currentIndex,
                savedIndex = currentIndex,
                automaticDefaultIndex = allowedIndexOf(automaticPageSize.defaultPageSize),
                recommendedLimitIndex = allowedIndexOf(automaticPageSize.recommendedLimit),
            )
        }
    }

    private fun pageSizeChanged(sliderIndex: Int) {
        updateViewState { copy(selectedIndex = sliderIndex.coerceIn(0, ALLOWED_PAGE_SIZES.lastIndex)) }
    }

    private fun save() {
        val pageSize = ALLOWED_PAGE_SIZES[viewState.value.selectedIndex]
        updateGlobalPreferencesUseCase.execute(
            GlobalPreferencesUpdate(
                apiFetchPageSize = pageSize,
                isApiFetchPageSizeManuallySet = true,
            ),
        )
        updateViewState { copy(savedIndex = selectedIndex) }
    }

    private fun restoreDefaults() {
        val automaticDefaultIndex = viewState.value.automaticDefaultIndex
        updateGlobalPreferencesUseCase.execute(
            GlobalPreferencesUpdate(
                apiFetchPageSize = ALLOWED_PAGE_SIZES[automaticDefaultIndex],
                isApiFetchPageSizeManuallySet = false,
            ),
        )
        updateViewState { copy(selectedIndex = automaticDefaultIndex, savedIndex = automaticDefaultIndex) }
    }

    private fun allowedIndexOf(pageSize: Int) = ALLOWED_PAGE_SIZES.indexOf(pageSize).coerceIn(0, ALLOWED_PAGE_SIZES.lastIndex)
}
