package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.RemoveAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.RemoveAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.RemoveSelectedAccountUseCase
import com.passbolt.mobile.android.domain.auth.DatabasePassphraseRepository
import com.passbolt.mobile.android.domain.auth.PassphraseRepository
import com.passbolt.mobile.android.domain.auth.SessionRepository
import com.passbolt.mobile.android.domain.auth.usecase.RemoveServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import timber.log.Timber

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
class RemoveAllAccountDataUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeAccountDataUseCase: RemoveAccountDataUseCase,
    private val passphraseRepository: PassphraseRepository,
    private val privateKeyRepository: PrivateKeyRepository,
    private val removeSelectedAccountUseCase: RemoveSelectedAccountUseCase,
    private val sessionRepository: SessionRepository,
    private val removeAccountUseCase: RemoveAccountUseCase,
    private val removeServerPublicRsaKeyUseCase: RemoveServerPublicRsaKeyUseCase,
    private val databaseProvider: DatabaseProvider,
    private val databasePassphraseRepository: DatabasePassphraseRepository,
) : AsyncUseCase<UserIdInput, Unit> {
    override suspend fun execute(input: UserIdInput) {
        Timber.d("Removing all account data")
        val accountToRemoveId = UserIdInput(input.userId)
        removeAccountData(accountToRemoveId)

        val selectedAccountId = getSelectedAccountUseCase.execute(Unit).selectedAccount
        if (accountToRemoveId.userId == selectedAccountId) {
            removeSelectedAccountUseCase.execute(Unit)
        }
    }

    private suspend fun removeAccountData(userIdInput: UserIdInput) {
        removeAccountDataUseCase.execute(userIdInput)
        passphraseRepository.removePassphrase(userIdInput.userId)
        privateKeyRepository.removePrivateKey(userIdInput.userId)
        sessionRepository.removeSession(userIdInput.userId)
        sessionRepository.removeMfaToken(userIdInput.userId)
        removeAccountUseCase.execute(userIdInput)
        removeServerPublicRsaKeyUseCase.execute(userIdInput)
        databaseProvider.delete(userIdInput.userId)
        databasePassphraseRepository.removeDatabasePassphrase(userIdInput.userId)
    }
}
