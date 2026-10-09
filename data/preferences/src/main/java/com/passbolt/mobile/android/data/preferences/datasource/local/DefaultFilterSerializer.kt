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

import net.svaroh.passly.ui.DefaultFilterUiModel

internal class DefaultFilterSerializer {
    fun serialize(defaultFilter: DefaultFilterUiModel): String =
        when (defaultFilter) {
            DefaultFilterUiModel.LAST_USED -> LAST_USED_ID
            DefaultFilterUiModel.ALL_ITEMS -> ALL_ITEMS_ID
            DefaultFilterUiModel.FAVOURITES -> FAVOURITES_ID
            DefaultFilterUiModel.RECENTLY_MODIFIED -> RECENTLY_MODIFIED_ID
            DefaultFilterUiModel.SHARED_WITH_ME -> SHARED_WITH_ME_ID
            DefaultFilterUiModel.OWNED_BY_ME -> OWNED_BY_ME_ID
            DefaultFilterUiModel.EXPIRY -> EXPIRY_ID
            DefaultFilterUiModel.FOLDERS -> FOLDERS_ID
            DefaultFilterUiModel.TAGS -> TAGS_ID
            DefaultFilterUiModel.GROUPS -> GROUPS_ID
        }

    fun deserialize(identifier: String?): DefaultFilterUiModel? =
        when (identifier) {
            LAST_USED_ID -> DefaultFilterUiModel.LAST_USED
            ALL_ITEMS_ID -> DefaultFilterUiModel.ALL_ITEMS
            FAVOURITES_ID -> DefaultFilterUiModel.FAVOURITES
            RECENTLY_MODIFIED_ID -> DefaultFilterUiModel.RECENTLY_MODIFIED
            SHARED_WITH_ME_ID -> DefaultFilterUiModel.SHARED_WITH_ME
            OWNED_BY_ME_ID -> DefaultFilterUiModel.OWNED_BY_ME
            EXPIRY_ID -> DefaultFilterUiModel.EXPIRY
            FOLDERS_ID -> DefaultFilterUiModel.FOLDERS
            TAGS_ID -> DefaultFilterUiModel.TAGS
            GROUPS_ID -> DefaultFilterUiModel.GROUPS
            else -> null
        }

    private companion object {
        private const val LAST_USED_ID = "LAST_USED"
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
