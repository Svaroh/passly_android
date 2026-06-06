package net.svaroh.passly.domain.preferences.mapper

import net.svaroh.passly.ui.DefaultFilterUiModel
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.HomeDisplayViewUiModel

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

fun HomeDisplayViewUiModel.toHomeDisplayViewModel(): HomeDisplayViewModel =
    when (this) {
        HomeDisplayViewUiModel.ALL_ITEMS -> HomeDisplayViewModel.AllItems
        HomeDisplayViewUiModel.FAVOURITES -> HomeDisplayViewModel.Favourites
        HomeDisplayViewUiModel.RECENTLY_MODIFIED -> HomeDisplayViewModel.RecentlyModified
        HomeDisplayViewUiModel.SHARED_WITH_ME -> HomeDisplayViewModel.SharedWithMe
        HomeDisplayViewUiModel.OWNED_BY_ME -> HomeDisplayViewModel.OwnedByMe
        HomeDisplayViewUiModel.FOLDERS -> HomeDisplayViewModel.folderRoot()
        HomeDisplayViewUiModel.TAGS -> HomeDisplayViewModel.tagsRoot()
        HomeDisplayViewUiModel.GROUPS -> HomeDisplayViewModel.groupsRoot()
        HomeDisplayViewUiModel.EXPIRY -> HomeDisplayViewModel.Expiry
    }

fun DefaultFilterUiModel.toHomeDisplayViewModel(lastUsedHomeView: HomeDisplayViewUiModel): HomeDisplayViewModel =
    when (this) {
        DefaultFilterUiModel.LAST_USED -> lastUsedHomeView.toHomeDisplayViewModel()
        DefaultFilterUiModel.ALL_ITEMS -> HomeDisplayViewModel.AllItems
        DefaultFilterUiModel.FAVOURITES -> HomeDisplayViewModel.Favourites
        DefaultFilterUiModel.RECENTLY_MODIFIED -> HomeDisplayViewModel.RecentlyModified
        DefaultFilterUiModel.SHARED_WITH_ME -> HomeDisplayViewModel.SharedWithMe
        DefaultFilterUiModel.OWNED_BY_ME -> HomeDisplayViewModel.OwnedByMe
        DefaultFilterUiModel.FOLDERS -> HomeDisplayViewModel.folderRoot()
        DefaultFilterUiModel.TAGS -> HomeDisplayViewModel.tagsRoot()
        DefaultFilterUiModel.GROUPS -> HomeDisplayViewModel.groupsRoot()
        DefaultFilterUiModel.EXPIRY -> HomeDisplayViewModel.Expiry
    }
