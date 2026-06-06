package net.svaroh.passly.feature.home.filtersmenu

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.domain.preferences.HomeDisplayViewPreferencesUpdate
import net.svaroh.passly.domain.preferences.mapper.toHomeDisplayViewModel
import net.svaroh.passly.domain.preferences.usecase.UpdateHomeDisplayViewPreferencesUseCase
import net.svaroh.passly.domain.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.AllItemsClick
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.Close
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.ExpiryClick
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.FavouritesClick
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.FoldersClick
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.GroupsClick
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.Initialize
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.OwnedByMeClick
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.RecentlyModifiedClick
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.SharedWithMeClick
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuIntent.TagsClick
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuSideEffect.Dismiss
import net.svaroh.passly.feature.home.filtersmenu.FiltersMenuSideEffect.HomeViewChanged
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import net.svaroh.passly.ui.HomeDisplayViewUiModel
import net.svaroh.passly.ui.HomeDisplayViewUiModel.ALL_ITEMS
import net.svaroh.passly.ui.HomeDisplayViewUiModel.EXPIRY
import net.svaroh.passly.ui.HomeDisplayViewUiModel.FAVOURITES
import net.svaroh.passly.ui.HomeDisplayViewUiModel.FOLDERS
import net.svaroh.passly.ui.HomeDisplayViewUiModel.GROUPS
import net.svaroh.passly.ui.HomeDisplayViewUiModel.OWNED_BY_ME
import net.svaroh.passly.ui.HomeDisplayViewUiModel.RECENTLY_MODIFIED
import net.svaroh.passly.ui.HomeDisplayViewUiModel.SHARED_WITH_ME
import net.svaroh.passly.ui.HomeDisplayViewUiModel.TAGS
import net.svaroh.passly.ui.RbacRuleModel.ALLOW
import kotlinx.coroutines.launch

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

class FiltersMenuViewModel(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    private val updateHomeDisplayViewPreferencesUseCase: UpdateHomeDisplayViewPreferencesUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<FiltersMenuState, FiltersMenuSideEffect>(FiltersMenuState()) {
    init {
        processAdditionalItemsVisibility()
    }

    fun onIntent(intent: FiltersMenuIntent) {
        when (intent) {
            is Initialize -> updateViewState { copy(activeDisplayView = intent.menuModel.activeDisplayView) }
            is AllItemsClick -> handleHomeViewChanged(ALL_ITEMS)
            is FavouritesClick -> handleHomeViewChanged(FAVOURITES)
            is RecentlyModifiedClick -> handleHomeViewChanged(RECENTLY_MODIFIED)
            is SharedWithMeClick -> handleHomeViewChanged(SHARED_WITH_ME)
            is OwnedByMeClick -> handleHomeViewChanged(OWNED_BY_ME)
            is ExpiryClick -> handleHomeViewChanged(EXPIRY)
            is FoldersClick -> handleHomeViewChanged(FOLDERS)
            is TagsClick -> handleHomeViewChanged(TAGS)
            is GroupsClick -> handleHomeViewChanged(GROUPS)
            is Close -> emitSideEffect(Dismiss)
        }
    }

    private fun handleHomeViewChanged(homeDisplayViewUiModel: HomeDisplayViewUiModel) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateHomeDisplayViewPreferencesUseCase.execute(
                HomeDisplayViewPreferencesUpdate(lastUsedHomeView = homeDisplayViewUiModel),
            )
            emitSideEffect(HomeViewChanged(homeDisplayViewUiModel.toHomeDisplayViewModel()))
            emitSideEffect(Dismiss)
        }
    }

    private fun processAdditionalItemsVisibility() {
        launch {
            val featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            val rbac = getRbacRulesUseCase.execute(Unit).rbacModel

            updateViewState {
                copy(
                    showFoldersMenuItem = featureFlags.areFoldersAvailable && rbac.foldersUseRule == ALLOW,
                    showTagsMenuItem = featureFlags.areTagsAvailable && rbac.tagsUseRule == ALLOW,
                    showExpiryMenuItem = featureFlags.isPasswordExpiryAvailable,
                )
            }
        }
    }
}
