/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced

import net.svaroh.passly.common.validation.StringIsAPositiveIntegerNumber
import net.svaroh.passly.common.validation.validation
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.AlgorithmChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.ApplyChanges
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.DigitChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.GoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.PeriodChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormSideEffect.ApplyAndGoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormSideEffect.NavigateBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced.TotpPeriodValidationError.MustBePositiveInteger
import net.svaroh.passly.ui.ResourceFormMode
import net.svaroh.passly.ui.TotpUiModel

internal class TotpAdvancedSettingsFormViewModel(
    mode: ResourceFormMode,
    private val totpUiModel: TotpUiModel,
) : SideEffectViewModel<TotpAdvancedSettingsFormState, TotpAdvancedSettingsFormSideEffect>(
        initialState =
            TotpAdvancedSettingsFormState(
                resourceFormMode = mode,
                expiry = totpUiModel.expiry,
                length = totpUiModel.length,
                algorithm = totpUiModel.algorithm,
            ),
    ) {
    fun onIntent(intent: TotpAdvancedSettingsFormIntent) {
        when (intent) {
            is PeriodChanged -> updateViewState { copy(expiry = intent.period) }
            is DigitChanged -> updateViewState { copy(length = intent.digits) }
            is AlgorithmChanged -> updateViewState { copy(algorithm = intent.algorithm) }
            ApplyChanges -> applyChanges()
            GoBack -> emitSideEffect(NavigateBack)
        }
    }

    private fun applyChanges() {
        updateViewState { copy(periodValidationErrors = emptyList()) }
        val state = viewState.value
        validation {
            of(state.expiry) {
                withRules(StringIsAPositiveIntegerNumber) {
                    onInvalid {
                        updateViewState {
                            copy(periodValidationErrors = periodValidationErrors + MustBePositiveInteger)
                        }
                    }
                }
            }
            onValid {
                emitSideEffect(
                    ApplyAndGoBack(
                        totpUiModel.copy(
                            expiry = state.expiry,
                            length = state.length,
                            algorithm = state.algorithm,
                        ),
                    ),
                )
            }
        }
    }
}
