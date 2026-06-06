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

package net.svaroh.passly.feature.settings.screen.appsettings.expertsettings

import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.domain.preferences.GlobalPreferencesUpdate
import net.svaroh.passly.domain.preferences.usecase.GetGlobalPreferencesUseCase
import net.svaroh.passly.domain.preferences.usecase.UpdateGlobalPreferencesUseCase
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.ExpertSettingsIntent.GoBack
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.ExpertSettingsIntent.GoToPageSize
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.ExpertSettingsIntent.ToggleAuthRequiredOnEveryEntry
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.ExpertSettingsIntent.ToggleHideRootWarning
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.ExpertSettingsScreenSideEffect.NavigateToPageSize
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.ExpertSettingsScreenSideEffect.NavigateUp

internal class ExpertSettingsViewModel(
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
) : SideEffectViewModel<ExpertSettingsState, ExpertSettingsScreenSideEffect>(ExpertSettingsState()) {
    init {
        loadInitialValues()
    }

    fun onIntent(intent: ExpertSettingsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            ToggleAuthRequiredOnEveryEntry -> toggleAuthRequiredOnEveryEntry()
            ToggleHideRootWarning -> toggleHideRootWarning()
            GoToPageSize -> emitSideEffect(NavigateToPageSize)
        }
    }

    private fun loadInitialValues() {
        val globalPreferences = getGlobalPreferencesUseCase.execute(Unit)
        updateViewState {
            copy(
                isAuthRequiredOnEveryEntryChecked = globalPreferences.isAuthRequiredOnEveryEntry,
                isHideRootWarningChecked = globalPreferences.isHideRootDialogEnabled,
            )
        }
    }

    private fun toggleAuthRequiredOnEveryEntry() {
        val isChecked = !viewState.value.isAuthRequiredOnEveryEntryChecked
        updateGlobalPreferencesUseCase.execute(
            GlobalPreferencesUpdate(isAuthRequiredOnEveryEntry = isChecked),
        )
        updateViewState {
            copy(isAuthRequiredOnEveryEntryChecked = isChecked)
        }
    }

    private fun toggleHideRootWarning() {
        val isHideRootWarningChecked = !viewState.value.isHideRootWarningChecked

        updateGlobalPreferencesUseCase.execute(
            GlobalPreferencesUpdate(isHideRootDialogEnabled = isHideRootWarningChecked),
        )
        updateViewState {
            copy(isHideRootWarningChecked = isHideRootWarningChecked)
        }
    }
}
