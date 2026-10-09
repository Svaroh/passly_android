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

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.secrets.SecretsRepository
import timber.log.Timber

class FetchSecretUseCase(
    private val secretsRepository: SecretsRepository,
) : AsyncUseCase<FetchSecretUseCase.Input, FetchSecretUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        Timber.d("Fetching secret")
        return when (val result = secretsRepository.getSecret(input.resourceId)) {
            is DomainResult.Finished -> Output.EncryptedSecret(result.value.data)
            is DomainResult.Incomplete -> {
                Timber.e("Failed to fetch secret")
                Output.Failure(result)
            }
        }
    }

    data class Input(
        val resourceId: String,
    )

    sealed class Output {
        data class EncryptedSecret(
            val encryptedSecret: String,
        ) : Output()

        data class Failure(
            val incomplete: DomainResult.Incomplete,
        ) : Output()
    }
}
