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
 * Drops the local copy of one secret.
 *
 * This exists for the single case where the stored ciphertext is known to be wrong - the resource was re-encrypted
 * and this device did not get the new block - so keeping it would serve a stale password. It is never used to expire
 * a secret that is still correct.
 */
class RemoveLocalSecretUseCase(
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) : AsyncUseCase<RemoveLocalSecretUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        databaseProvider
            .get(userId)
            .secretsDao()
            .delete(input.resourceId)
    }

    data class Input(
        val resourceId: String,
    )
}
