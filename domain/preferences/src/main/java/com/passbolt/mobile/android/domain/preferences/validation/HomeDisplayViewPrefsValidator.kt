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

package com.passbolt.mobile.android.domain.preferences.validation

import com.passbolt.mobile.android.domain.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.DefaultFilterUiModel
import com.passbolt.mobile.android.ui.HomeDisplayViewPreferencesUiModel
import com.passbolt.mobile.android.ui.HomeDisplayViewUiModel
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import kotlinx.coroutines.runBlocking

class HomeDisplayViewPrefsValidator(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
) {
    fun validated(preferences: HomeDisplayViewPreferencesUiModel): HomeDisplayViewPreferencesUiModel {
        val featureFlags = runBlocking { getFeatureFlagsUseCase.execute(Unit).featureFlags }
        val rbac = runBlocking { getRbacRulesUseCase.execute(Unit).rbacModel }
        val validatedLastUsedView =
            preferences.lastUsedHomeView.mutateIf(
                { isNotAvailable(preferences.lastUsedHomeView, featureFlags, rbac) },
                HomeDisplayViewUiModel.ALL_ITEMS,
            )
        val validatedUserSetView =
            preferences.userSetHomeView.mutateIf(
                { isNotAvailable(preferences.userSetHomeView, featureFlags, rbac) },
                DefaultFilterUiModel.ALL_ITEMS,
            )
        return preferences.copy(
            lastUsedHomeView = validatedLastUsedView,
            userSetHomeView = validatedUserSetView,
        )
    }

    fun validatedDefaultFiltersList(): List<DefaultFilterUiModel> {
        val featureFlags = runBlocking { getFeatureFlagsUseCase.execute(Unit).featureFlags }
        val rbac = runBlocking { getRbacRulesUseCase.execute(Unit).rbacModel }
        return DefaultFilterUiModel
            .entries
            .toMutableList()
            .apply {
                if (!featureFlags.areFoldersAvailable || rbac.foldersUseRule != ALLOW) {
                    remove(DefaultFilterUiModel.FOLDERS)
                }
                if (!featureFlags.areTagsAvailable || rbac.tagsUseRule != ALLOW) {
                    remove(DefaultFilterUiModel.TAGS)
                }
            }
    }

    private fun <T> T.mutateIf(
        condition: () -> Boolean,
        replacement: T,
    ) = if (condition()) replacement else this

    private fun isNotAvailable(
        view: DefaultFilterUiModel,
        featureFlags: FeatureFlagsModel,
        rbac: RbacModel,
    ) = (view == DefaultFilterUiModel.FOLDERS && (!featureFlags.areFoldersAvailable || rbac.foldersUseRule != ALLOW)) ||
        (view == DefaultFilterUiModel.TAGS && (!featureFlags.areTagsAvailable || rbac.tagsUseRule != ALLOW))

    private fun isNotAvailable(
        view: HomeDisplayViewUiModel,
        featureFlags: FeatureFlagsModel,
        rbac: RbacModel,
    ) = (view == HomeDisplayViewUiModel.FOLDERS && (!featureFlags.areFoldersAvailable || rbac.foldersUseRule != ALLOW)) ||
        (view == HomeDisplayViewUiModel.TAGS && (!featureFlags.areTagsAvailable || rbac.tagsUseRule != ALLOW))
}
