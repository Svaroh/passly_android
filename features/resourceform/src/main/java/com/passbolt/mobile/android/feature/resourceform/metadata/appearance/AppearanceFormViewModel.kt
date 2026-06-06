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

package net.svaroh.passly.feature.resourceform.metadata.appearance

import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.feature.resourceform.metadata.appearance.AppearanceFormIntent.ApplyChanges
import net.svaroh.passly.feature.resourceform.metadata.appearance.AppearanceFormIntent.GoBack
import net.svaroh.passly.feature.resourceform.metadata.appearance.AppearanceFormIntent.Initialize
import net.svaroh.passly.feature.resourceform.metadata.appearance.AppearanceFormIntent.SetCustomIconBackgroundColor
import net.svaroh.passly.feature.resourceform.metadata.appearance.AppearanceFormIntent.SetKeepassIcon
import net.svaroh.passly.feature.resourceform.metadata.appearance.AppearanceFormIntent.ToggleDefaultColor
import net.svaroh.passly.feature.resourceform.metadata.appearance.AppearanceFormIntent.ToggleDefaultIcon
import net.svaroh.passly.feature.resourceform.metadata.appearance.AppearanceFormSideEffect.NavigateUp
import net.svaroh.passly.mappers.ResourceFormMapper
import net.svaroh.passly.ui.ResourceAppearanceModel

internal class AppearanceFormViewModel(
    private val resourceFormMapper: ResourceFormMapper,
) : SideEffectViewModel<AppearanceFormState, AppearanceFormSideEffect>(AppearanceFormState()) {
    fun onIntent(intent: AppearanceFormIntent) {
        when (intent) {
            is SetCustomIconBackgroundColor ->
                updateViewState {
                    copy(iconBackgroundColorHex = intent.colorHexString, isDefaultColorChecked = false)
                }
            is SetKeepassIcon ->
                updateViewState {
                    copy(keepassIconValue = intent.iconValue, isDefaultIconChecked = false)
                }
            GoBack -> emitSideEffect(NavigateUp)
            ToggleDefaultColor -> toggleDefaultColor()
            ToggleDefaultIcon -> toggleDefaultIcon()
            is Initialize -> initialize(intent)
            ApplyChanges -> applyChanges()
        }
    }

    private fun applyChanges() {
        val state = viewState.value
        emitSideEffect(
            AppearanceFormSideEffect.ApplyAndGoBack(
                resourceFormMapper.toAppearanceModel(
                    state.keepassIconValue,
                    state.iconBackgroundColorHex,
                ),
            ),
        )
    }

    private fun toggleDefaultIcon() {
        val isChecked = !viewState.value.isDefaultIconChecked
        updateViewState {
            copy(
                isDefaultIconChecked = isChecked,
                keepassIconValue =
                    if (isChecked) {
                        null
                    } else {
                        viewState.value.keepassIconValue
                    },
            )
        }
    }

    private fun toggleDefaultColor() {
        val isChecked = !viewState.value.isDefaultColorChecked
        updateViewState {
            copy(
                isDefaultColorChecked = isChecked,
                iconBackgroundColorHex =
                    if (isChecked) {
                        ResourceAppearanceModel.DEFAULT_BACKGROUND_COLOR_HEX_STRING
                    } else {
                        viewState.value.iconBackgroundColorHex
                    },
            )
        }
    }

    private fun initialize(initialization: Initialize) {
        val appearanceModel = initialization.model
        updateViewState {
            copy(
                resourceFormMode = initialization.resourceFormMode,
                isDefaultColorChecked = appearanceModel.isDefaultBackgroundColorSet,
                isDefaultIconChecked = appearanceModel.isDefaultIconSet,
                keepassIconValue = appearanceModel.iconValue,
                iconBackgroundColorHex = appearanceModel.iconBackgroundHexColor,
            )
        }
    }
}
