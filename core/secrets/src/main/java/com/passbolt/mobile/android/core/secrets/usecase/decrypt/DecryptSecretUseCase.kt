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

package net.svaroh.passly.core.secrets.usecase.decrypt

import net.svaroh.passly.common.extension.erase
import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.privatekey.GetPrivateKeyUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.core.mvp.authentication.UnauthenticatedReason
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.gopenpgp.exception.OpenPgpError
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult
import timber.log.Timber

class DecryptSecretUseCase(
    private val gopenPgp: OpenPgp,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
) : AsyncUseCase<DecryptSecretUseCase.Input, DecryptSecretUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        val account =
            UserIdInput(
                requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount),
            )
        val potentialPassphrase = passphraseMemoryCache.get()
        return if (potentialPassphrase is PotentialPassphrase.Passphrase) {
            val passphraseCopy = potentialPassphrase.passphrase.copyOf()
            val decrypted =
                gopenPgp.decryptMessageArmored(
                    getPrivateKeyUseCase.execute(account).privateKey,
                    passphraseCopy,
                    input.encryptedSecret,
                )
            when (decrypted) {
                is OpenPgpResult.Error -> {
                    Timber.e(decrypted.error.message)
                    Output.Failure(decrypted.error)
                }
                is OpenPgpResult.Result -> {
                    passphraseCopy.erase()
                    Output.DecryptedSecret(decrypted.result)
                }
            }
        } else {
            Output.Unauthorized(AuthenticationState.Unauthenticated.Reason.Passphrase)
        }
    }

    data class Input(
        val encryptedSecret: String,
    )

    sealed class Output {
        data class Unauthorized(
            val reason: UnauthenticatedReason,
        ) : Output()

        data class Failure(
            val exception: OpenPgpError,
        ) : Output()

        data class DecryptedSecret(
            val decryptedSecret: String,
        ) : Output()
    }
}
