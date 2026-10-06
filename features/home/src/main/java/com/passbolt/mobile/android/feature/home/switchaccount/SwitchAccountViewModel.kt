package net.svaroh.passly.feature.home.switchaccount

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.domain.accounts.usecase.GetAllAccountsDataUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.accounts.usecase.SaveSelectedAccountUseCase
import net.svaroh.passly.domain.users.profile.UserProfileRefreshTrackingFlow
import net.svaroh.passly.feature.authentication.auth.usecase.SignOutUseCase
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountIntent.Close
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountIntent.CloseSignOutDialog
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountIntent.ManageAccounts
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountIntent.Refresh
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountIntent.SeeCurrentAccountDetails
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountIntent.SignOut
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountIntent.SignOutConfirmed
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountIntent.SwitchAccount
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountSideEffect.NavigateToSignInForAccount
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountSideEffect.NavigateToStartup
import net.svaroh.passly.mappers.SwitchAccountModelMapper
import net.svaroh.passly.ui.SwitchAccountUiModel.AccountItem

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

class SwitchAccountViewModel(
    appContext: AppContext,
    private val getAllAccountsDataUseCase: GetAllAccountsDataUseCase,
    private val switchAccountModelMapper: SwitchAccountModelMapper,
    private val signOutUseCase: SignOutUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val userProfileRefreshTrackingFlow: UserProfileRefreshTrackingFlow,
) : SideEffectViewModel<SwitchAccountState, SwitchAccountSideEffect>(
        initialState = SwitchAccountState(appContext = appContext),
    ) {
    init {
        loadAccounts()
        viewModelScope.launch {
            userProfileRefreshTrackingFlow.isRefreshing.collect { isRefreshing ->
                updateViewState { copy(isCurrentAccountProfileLoading = isRefreshing) }
            }
        }
    }

    fun onIntent(intent: SwitchAccountIntent) {
        when (intent) {
            is Refresh -> loadAccounts()
            is Close -> emitSideEffect(SwitchAccountSideEffect.Dismiss)
            is SeeCurrentAccountDetails -> emitSideEffect(SwitchAccountSideEffect.NavigateToAccountDetails)
            is SignOut -> updateViewState { copy(showSignOutDialog = true) }
            is SignOutConfirmed -> signOut()
            is SwitchAccount -> switchToAccount(intent.account)
            ManageAccounts -> emitSideEffect(SwitchAccountSideEffect.NavigateToManageAccounts)
            CloseSignOutDialog -> updateViewState { copy(showSignOutDialog = false) }
        }
    }

    private fun loadAccounts() {
        val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
        val accounts = getAllAccountsDataUseCase.execute(Unit).accounts
        val accountsList = switchAccountModelMapper.map(accounts, selectedAccount, viewState.value.appContext)

        updateViewState {
            copy(accountsList = accountsList)
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            updateViewState { copy(showSignOutDialog = false, showProgress = true) }
            dataRefreshTrackingFlow.awaitIdle()
            signOutUseCase.execute(Unit)
            updateViewState { copy(showProgress = false) }
            emitSideEffect(NavigateToStartup(viewState.value.appContext))
        }
    }

    private fun switchToAccount(account: AccountItem) {
        saveSelectedAccountUseCase.execute(UserIdInput(account.userId))
        emitSideEffect(NavigateToSignInForAccount(viewState.value.appContext))
    }
}
