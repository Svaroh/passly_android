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

package net.svaroh.passly.data.preferences.datasource.local

import net.svaroh.passly.ui.HomeDisplayViewUiModel

internal class HomeDisplayViewSerializer {
    fun serialize(homeDisplayView: HomeDisplayViewUiModel): String =
        when (homeDisplayView) {
            HomeDisplayViewUiModel.ALL_ITEMS -> ALL_ITEMS_ID
            HomeDisplayViewUiModel.FAVOURITES -> FAVOURITES_ID
            HomeDisplayViewUiModel.RECENTLY_MODIFIED -> RECENTLY_MODIFIED_ID
            HomeDisplayViewUiModel.SHARED_WITH_ME -> SHARED_WITH_ME_ID
            HomeDisplayViewUiModel.OWNED_BY_ME -> OWNED_BY_ME_ID
            HomeDisplayViewUiModel.EXPIRY -> EXPIRY_ID
            HomeDisplayViewUiModel.FOLDERS -> FOLDERS_ID
            HomeDisplayViewUiModel.TAGS -> TAGS_ID
            HomeDisplayViewUiModel.GROUPS -> GROUPS_ID
        }

    fun deserialize(identifier: String?): HomeDisplayViewUiModel? =
        when (identifier) {
            ALL_ITEMS_ID -> HomeDisplayViewUiModel.ALL_ITEMS
            FAVOURITES_ID -> HomeDisplayViewUiModel.FAVOURITES
            RECENTLY_MODIFIED_ID -> HomeDisplayViewUiModel.RECENTLY_MODIFIED
            SHARED_WITH_ME_ID -> HomeDisplayViewUiModel.SHARED_WITH_ME
            OWNED_BY_ME_ID -> HomeDisplayViewUiModel.OWNED_BY_ME
            EXPIRY_ID -> HomeDisplayViewUiModel.EXPIRY
            FOLDERS_ID -> HomeDisplayViewUiModel.FOLDERS
            TAGS_ID -> HomeDisplayViewUiModel.TAGS
            GROUPS_ID -> HomeDisplayViewUiModel.GROUPS
            else -> null
        }

    private companion object {
        private const val ALL_ITEMS_ID = "ALL_ITEMS"
        private const val FAVOURITES_ID = "FAVOURITES"
        private const val RECENTLY_MODIFIED_ID = "RECENTLY_MODIFIED"
        private const val SHARED_WITH_ME_ID = "SHARED_WITH_ME"
        private const val OWNED_BY_ME_ID = "OWNED_BY_ME"
        private const val EXPIRY_ID = "EXPIRY"
        private const val FOLDERS_ID = "FOLDERS"
        private const val TAGS_ID = "TAGS"
        private const val GROUPS_ID = "GROUPS"
    }
}
