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

package com.passbolt.mobile.android.domain.secrets.usecase.decrypt

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.usePassphraseCopy
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpError
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import timber.log.Timber

class DecryptSecretUseCase(
    private val gopenPgp: OpenPgp,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
) : AsyncUseCase<DecryptSecretUseCase.Input, DecryptSecretUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        val userId =
            requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        return passphraseMemoryCache.usePassphraseCopy(
            onPassphraseNotPresent = { Output.Unauthorized(AuthenticationState.Unauthenticated.Reason.Passphrase) },
        ) { passphrase ->
            val decrypted =
                gopenPgp.decryptMessageArmored(
                    requireNotNull(privateKeyRepository.getPrivateKey(userId)) { "Unable to restore private key." }.armoredKey,
                    passphrase,
                    input.encryptedSecret,
                )
            when (decrypted) {
                is OpenPgpResult.Error -> {
                    Timber.e(decrypted.error.message)
                    Output.Failure(decrypted.error.pgpError)
                }
                is OpenPgpResult.Result -> Output.DecryptedSecret(decrypted.result)
            }
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
