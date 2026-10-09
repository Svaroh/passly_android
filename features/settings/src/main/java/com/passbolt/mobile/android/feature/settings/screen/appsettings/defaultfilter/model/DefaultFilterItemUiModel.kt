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

package net.svaroh.passly.feature.settings.screen.appsettings.defaultfilter.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.svaroh.passly.ui.DefaultFilterUiModel
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

internal data class DefaultFilterItemUiModel(
    val filter: DefaultFilterUiModel,
    @param:StringRes val nameRes: Int,
    @param:DrawableRes val iconRes: Int,
)

@Suppress("LongMethod")
internal fun DefaultFilterUiModel.toUiModel(): DefaultFilterItemUiModel =
    when (this) {
        DefaultFilterUiModel.LAST_USED ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_last_used,
                iconRes = CoreUiR.drawable.ic_filter,
            )
        DefaultFilterUiModel.ALL_ITEMS ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_all_items,
                iconRes = CoreUiR.drawable.ic_list,
            )
        DefaultFilterUiModel.FAVOURITES ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_favourites,
                iconRes = CoreUiR.drawable.ic_star,
            )
        DefaultFilterUiModel.RECENTLY_MODIFIED ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_recently_modified,
                iconRes = CoreUiR.drawable.ic_clock,
            )
        DefaultFilterUiModel.SHARED_WITH_ME ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_shared_with_me,
                iconRes = CoreUiR.drawable.ic_share,
            )
        DefaultFilterUiModel.OWNED_BY_ME ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_owned_by_me,
                iconRes = CoreUiR.drawable.ic_person,
            )
        DefaultFilterUiModel.EXPIRY ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_expiry,
                iconRes = CoreUiR.drawable.ic_calendar_clock,
            )
        DefaultFilterUiModel.FOLDERS ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_folders,
                iconRes = CoreUiR.drawable.ic_folder,
            )
        DefaultFilterUiModel.TAGS ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_tags,
                iconRes = CoreUiR.drawable.ic_tag,
            )
        DefaultFilterUiModel.GROUPS ->
            DefaultFilterItemUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_groups,
                iconRes = CoreUiR.drawable.ic_group,
            )
    }
