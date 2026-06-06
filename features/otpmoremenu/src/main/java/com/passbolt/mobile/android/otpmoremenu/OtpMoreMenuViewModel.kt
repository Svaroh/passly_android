package net.svaroh.passly.otpmoremenu

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.otpmoremenu.OtpMoreMenuIntent.Close
import net.svaroh.passly.otpmoremenu.OtpMoreMenuIntent.CopyOtp
import net.svaroh.passly.otpmoremenu.OtpMoreMenuIntent.DeleteOtp
import net.svaroh.passly.otpmoremenu.OtpMoreMenuIntent.EditOtp
import net.svaroh.passly.otpmoremenu.OtpMoreMenuIntent.Initialize
import net.svaroh.passly.otpmoremenu.OtpMoreMenuIntent.ShowOtp
import net.svaroh.passly.otpmoremenu.OtpMoreMenuSideEffect.Dismiss
import net.svaroh.passly.otpmoremenu.OtpMoreMenuSideEffect.InvokeCopyOtp
import net.svaroh.passly.otpmoremenu.OtpMoreMenuSideEffect.InvokeDeleteOtp
import net.svaroh.passly.otpmoremenu.OtpMoreMenuSideEffect.InvokeEditOtp
import net.svaroh.passly.otpmoremenu.OtpMoreMenuSideEffect.InvokeShowOtp
import net.svaroh.passly.otpmoremenu.usecase.CreateOtpMoreMenuModelUseCase
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

class OtpMoreMenuViewModel(
    private val createOtpMoreMenuModelUseCase: CreateOtpMoreMenuModelUseCase,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
) : SideEffectViewModel<OtpMoreMenuState, OtpMoreMenuSideEffect>(OtpMoreMenuState()) {
    fun onIntent(intent: OtpMoreMenuIntent) {
        when (intent) {
            Close -> emitSideEffect(Dismiss)
            CopyOtp -> emitSideEffect(InvokeCopyOtp)
            DeleteOtp -> emitSideEffect(InvokeDeleteOtp)
            EditOtp -> emitSideEffect(InvokeEditOtp)
            ShowOtp -> emitSideEffect(InvokeShowOtp)
            is Initialize -> initialize(intent)
        }
    }

    private fun initialize(initialize: Initialize) {
        updateViewState { copy(title = initialize.resourceName, showShowOtpButton = initialize.canShowTotp) }
        viewModelScope.launch {
            dataRefreshTrackingFlow.awaitIdle()
            val menuModel =
                createOtpMoreMenuModelUseCase
                    .execute(
                        CreateOtpMoreMenuModelUseCase.Input(initialize.resourceId),
                    ).otpMoreMenuModel

            updateViewState {
                copy(
                    showDeleteButton = menuModel.canDelete,
                    showEditButton = menuModel.canEdit,
                    showSeparator = menuModel.canEdit || menuModel.canDelete,
                )
            }
        }
    }
}
