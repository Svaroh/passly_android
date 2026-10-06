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

package net.svaroh.passly.feature.settings.screen.accounts.keyinspector

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.formatter.DateFormatter
import net.svaroh.passly.core.formatter.FingerprintFormatter
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.users.usecase.FetchCurrentUserUseCase
import net.svaroh.passly.domain.users.usecase.GetLocalCurrentUserUseCase
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CloseMoreMenu
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CopyFingerprint
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CopyUid
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.GoBack
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.OpenMoreMenu
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddFingerprintToClipboard
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddUidToClipboard
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ErrorSnackbarType.FAILED_TO_FETCH_KEY
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.NavigateUp
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ShowErrorSnackbar
import net.svaroh.passly.mappers.AccountModelMapper
import net.svaroh.passly.ui.GpgKeyUiModel
import timber.log.Timber

internal class KeyInspectorViewModel(
    private val fetchCurrentUserUseCase: FetchCurrentUserUseCase,
    private val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val dateFormatter: DateFormatter,
    private val fingerprintFormatter: FingerprintFormatter,
    coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<KeyInspectorState, KeyInspectorScreenSideEffect>(KeyInspectorState()) {
    init {
        viewModelScope.launch(coroutineLaunchContext.default) {
            updateViewState { copy(showProgress = true) }
            loadInitialValues()
            updateViewState { copy(showProgress = false) }
        }
    }

    fun onIntent(intent: KeyInspectorIntent) {
        when (intent) {
            CopyFingerprint -> emitSideEffect(AddFingerprintToClipboard(viewState.value.fingerprint))
            CopyUid -> emitSideEffect(AddUidToClipboard(viewState.value.uid))
            GoBack -> emitSideEffect(NavigateUp)
            OpenMoreMenu -> updateViewState { copy(showBottomSheet = true) }
            CloseMoreMenu -> updateViewState { copy(showBottomSheet = false) }
        }
    }

    private suspend fun loadInitialValues() {
        showAccountData()
        fetchKeyData()
    }

    /**
     * The replica already holds this key.
     *
     * Every field on this screen - fingerprint, length, uid, dates, algorithm - is stored in the local users table,
     * so asking the server for them turns an instant screen into a wait, and an error message, whenever the server
     * cannot be reached. The network is used only when the account has no local copy of its own user yet.
     */
    private suspend fun fetchKeyData() {
        val localKey =
            try {
                getLocalCurrentUserUseCase.execute(Unit).user.gpgKey
            } catch (exception: Exception) {
                Timber.d(exception, "No local copy of the current user, asking the server")
                null
            }

        if (localKey != null) {
            showKeyData(localKey)
            return
        }

        when (val keyData = runAuthenticatedOperation { fetchCurrentUserUseCase.execute(Unit) }) {
            is FetchCurrentUserUseCase.Output.Failure -> emitSideEffect(ShowErrorSnackbar(FAILED_TO_FETCH_KEY, keyData.message))
            is FetchCurrentUserUseCase.Output.Success -> showKeyData(keyData.userUiModel.gpgKey)
        }
    }

    private fun showKeyData(keyData: GpgKeyUiModel) {
        updateViewState {
            copy(
                fingerprint = fingerprintFormatter.format(keyData.fingerprint, appendMiddleSpacing = false).orEmpty(),
                keyLength = keyData.bits,
                uid = keyData.uid.orEmpty(),
                created = keyData.keyCreationDate?.let { dateFormatter.format(it) }.orEmpty(),
                expires = keyData.keyExpirationDate?.let { dateFormatter.format(it) }.orEmpty(),
                algorithm = keyData.type.orEmpty(),
            )
        }
    }

    private fun showAccountData() {
        val accountData = getSelectedAccountDataUseCase.execute(Unit)
        val defaultLabel = AccountModelMapper.defaultLabel(accountData.firstName, accountData.lastName)
        updateViewState {
            copy(avatarUrl = accountData.avatarUrl, label = accountData.label ?: defaultLabel)
        }
    }
}
