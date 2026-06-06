package net.svaroh.passly.feature.authentication.auth.usecase

import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.accounts.usecase.RemoveAccountDataUseCase
import net.svaroh.passly.domain.accounts.usecase.RemoveAccountUseCase
import net.svaroh.passly.domain.accounts.usecase.RemoveSelectedAccountUseCase
import net.svaroh.passly.domain.auth.DatabasePassphraseRepository
import net.svaroh.passly.domain.auth.PassphraseRepository
import net.svaroh.passly.domain.auth.SessionRepository
import net.svaroh.passly.domain.auth.usecase.RemoveServerPublicRsaKeyUseCase
import net.svaroh.passly.domain.privatekey.PrivateKeyRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
class RemoveAllAccountDataUseCaseTest {
    private val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
    private val removeAccountDataUseCase = mock<RemoveAccountDataUseCase>()
    private val passphraseRepository = mock<PassphraseRepository>()
    private val privateKeyRepository = mock<PrivateKeyRepository>()
    private val removeSelectedAccountUseCase = mock<RemoveSelectedAccountUseCase>()
    private val sessionRepository = mock<SessionRepository>()
    private val removeAccountUseCase = mock<RemoveAccountUseCase>()
    private val removeServerPublicRsaKeyUseCase = mock<RemoveServerPublicRsaKeyUseCase>()
    private val databaseProvider = mock<DatabaseProvider>()
    private val databasePassphraseRepository = mock<DatabasePassphraseRepository>()

    private val useCase =
        RemoveAllAccountDataUseCase(
            getSelectedAccountUseCase = getSelectedAccountUseCase,
            removeAccountDataUseCase = removeAccountDataUseCase,
            passphraseRepository = passphraseRepository,
            privateKeyRepository = privateKeyRepository,
            removeSelectedAccountUseCase = removeSelectedAccountUseCase,
            sessionRepository = sessionRepository,
            removeAccountUseCase = removeAccountUseCase,
            removeServerPublicRsaKeyUseCase = removeServerPublicRsaKeyUseCase,
            databaseProvider = databaseProvider,
            databasePassphraseRepository = databasePassphraseRepository,
        )

    @Test
    fun `should remove session tokens and the mfa token when removing an account`() =
        runTest {
            whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(USER_ID)

            useCase.execute(UserIdInput(USER_ID))

            verify(sessionRepository).removeSession(USER_ID)
            verify(sessionRepository).removeMfaToken(USER_ID)
            verify(removeAccountDataUseCase).execute(UserIdInput(USER_ID))
            verify(passphraseRepository).removePassphrase(USER_ID)
            verify(privateKeyRepository).removePrivateKey(USER_ID)
            verify(removeAccountUseCase).execute(UserIdInput(USER_ID))
            verify(removeServerPublicRsaKeyUseCase).execute(UserIdInput(USER_ID))
            verify(databaseProvider).delete(USER_ID)
            verify(databasePassphraseRepository).removeDatabasePassphrase(USER_ID)
            verify(removeSelectedAccountUseCase).execute(Unit)
        }

    @Test
    fun `should keep the selected account when removing a different account`() =
        runTest {
            whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(OTHER_USER_ID)

            useCase.execute(UserIdInput(USER_ID))

            verify(sessionRepository).removeSession(USER_ID)
            verify(sessionRepository).removeMfaToken(USER_ID)
            verify(removeSelectedAccountUseCase, never()).execute(Unit)
        }

    private companion object {
        private const val USER_ID = "user-id"
        private const val OTHER_USER_ID = "other-user-id"
    }
}
