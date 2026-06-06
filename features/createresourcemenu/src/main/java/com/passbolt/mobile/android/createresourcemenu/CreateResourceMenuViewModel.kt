package net.svaroh.passly.createresourcemenu

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.Close
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.CreateFolder
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.CreateNote
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.CreatePassword
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.CreateTotp
import net.svaroh.passly.createresourcemenu.CreateResourceMenuIntent.Initialize
import net.svaroh.passly.createresourcemenu.usecase.CreateCreateResourceMenuModelUseCase
import net.svaroh.passly.ui.HomeDisplayViewModel
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

class CreateResourceMenuViewModel(
    private val createCreateResourceMoreMenuModelUseCase: CreateCreateResourceMenuModelUseCase,
) : SideEffectViewModel<CreateResourceMenuState, CreateResourceMenuSideEffect>(CreateResourceMenuState()) {
    fun onIntent(intent: CreateResourceMenuIntent) {
        when (intent) {
            Close -> emitSideEffect(CreateResourceMenuSideEffect.Dismiss)
            CreatePassword -> emitSideEffect(CreateResourceMenuSideEffect.InvokeCreatePassword)
            CreateTotp -> emitSideEffect(CreateResourceMenuSideEffect.InvokeCreateTotp)
            CreateFolder -> emitSideEffect(CreateResourceMenuSideEffect.InvokeCreateFolder)
            CreateNote -> emitSideEffect(CreateResourceMenuSideEffect.InvokeCreateNote)
            is Initialize -> initialize(intent.homeDisplayViewModel)
        }
    }

    private fun initialize(homeDisplayViewModel: HomeDisplayViewModel?) {
        viewModelScope.launch {
            createCreateResourceMoreMenuModelUseCase
                .execute(
                    CreateCreateResourceMenuModelUseCase.Input(homeDisplayViewModel),
                ).model
                .apply {
                    updateViewState {
                        copy(
                            showPasswordButton = isPasswordEnabled,
                            showTotpButton = isTotpEnabled,
                            showNoteButton = isNoteEnabled,
                            showFoldersButton = isFolderEnabled,
                        )
                    }
                }
        }
    }
}
