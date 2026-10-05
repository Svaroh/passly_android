/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.core.secrets.usecase.db

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase

/**
 * Reads the locally stored OpenPGP ciphertext of a secret.
 *
 * There is deliberately no freshness argument and no expiry check: a stored secret stays valid for as long as the
 * user keeps the account on the device.
 */
class GetLocalSecretUseCase(
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) : AsyncUseCase<GetLocalSecretUseCase.Input, GetLocalSecretUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val secret =
            databaseProvider
                .get(userId)
                .secretsDao()
                .getSecret(input.resourceId)

        return if (secret == null) Output.NotCached else Output.Cached(secret.armoredData)
    }

    data class Input(
        val resourceId: String,
    )

    sealed interface Output {
        data class Cached(
            val armoredSecret: String,
        ) : Output

        data object NotCached : Output
    }
}
