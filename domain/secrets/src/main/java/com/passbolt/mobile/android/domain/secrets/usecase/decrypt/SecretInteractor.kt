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

package net.svaroh.passly.domain.secrets.usecase.decrypt

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.mvp.authentication.AuthenticatedUseCaseOutput
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.core.mvp.authentication.CompleteAuthenticatedOutput
import net.svaroh.passly.core.mvp.authentication.IncompleteAuthenticatedOutput
import net.svaroh.passly.core.mvp.authentication.UnauthenticatedReason
import net.svaroh.passly.core.secrets.usecase.db.GetLocalSecretUseCase
import net.svaroh.passly.core.secrets.usecase.db.UpsertLocalSecretsUseCase
import net.svaroh.passly.gopenpgp.exception.OpenPgpError
import timber.log.Timber

/**
 * Resolves the plain secret of a resource, local copy first.
 *
 * The local ciphertext is authoritative for reading: it is the same armored block the server holds, it never
 * expires, and reaching for it does not need a session, a network or a server that still exists. The network is
 * consulted only when this device has never stored the secret - a resource that appeared through some path that did
 * not carry secrets yet. Whatever the network returns is written to the local store, so the same resource is
 * autonomous from then on.
 */
class SecretInteractor(
    private val fetchSecretUseCase: FetchSecretUseCase,
    private val decryptSecretUseCase: DecryptSecretUseCase,
    private val getLocalSecretUseCase: GetLocalSecretUseCase,
    private val upsertLocalSecretsUseCase: UpsertLocalSecretsUseCase,
) {
    suspend fun fetchAndDecrypt(resourceId: String): Output =
        when (val local = getLocalSecretUseCase.execute(GetLocalSecretUseCase.Input(resourceId))) {
            is GetLocalSecretUseCase.Output.Cached -> decrypt(local.armoredSecret)
            is GetLocalSecretUseCase.Output.NotCached -> fetchDecryptAndCache(resourceId)
        }

    private suspend fun fetchDecryptAndCache(resourceId: String): Output =
        when (val response = fetchSecretUseCase.execute(FetchSecretUseCase.Input(resourceId))) {
            is FetchSecretUseCase.Output.EncryptedSecret -> {
                cache(resourceId, response.encryptedSecret)
                decrypt(response.encryptedSecret)
            }
            is FetchSecretUseCase.Output.Failure -> Output.FetchFailure(response.incomplete)
        }

    private suspend fun cache(
        resourceId: String,
        armoredSecret: String,
    ) {
        try {
            upsertLocalSecretsUseCase.execute(
                UpsertLocalSecretsUseCase.Input(
                    secrets =
                        listOf(
                            UpsertLocalSecretsUseCase.LocalSecret(
                                resourceId = resourceId,
                                secretId = null,
                                armoredData = armoredSecret,
                                modified = null,
                            ),
                        ),
                ),
            )
        } catch (exception: Exception) {
            // failing to cache must not fail the read the user asked for
            Timber.e(exception, "Could not store the secret of resource locally")
        }
    }

    private suspend fun decrypt(encryptedSecret: String): Output =
        when (val output = decryptSecretUseCase.execute(DecryptSecretUseCase.Input(encryptedSecret))) {
            is DecryptSecretUseCase.Output.DecryptedSecret -> Output.Success(output.decryptedSecret)
            is DecryptSecretUseCase.Output.Failure -> Output.DecryptFailure(output.exception)
            is DecryptSecretUseCase.Output.Unauthorized -> Output.Unauthorized(output.reason)
        }

    sealed class Output : AuthenticatedUseCaseOutput {
        data class FetchFailure(
            override val incomplete: DomainResult.Incomplete,
        ) : Output(),
            IncompleteAuthenticatedOutput

        data class DecryptFailure(
            val error: OpenPgpError,
        ) : Output(),
            CompleteAuthenticatedOutput

        data class Unauthorized(
            val reason: UnauthenticatedReason,
        ) : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Unauthenticated(reason)
        }

        data class Success(
            val decryptedSecret: String,
        ) : Output(),
            CompleteAuthenticatedOutput
    }
}
