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
import net.svaroh.passly.core.accounts.usecase.SelectedAccountUseCase
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.entity.secret.Secret
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Stores OpenPGP ciphertext of secrets exactly as it arrived from the server.
 *
 * A secret whose resource is not (yet) in the local database is skipped rather than failing the whole batch - the
 * foreign key would reject it, and the next run picks it up once the resource row exists.
 */
class UpsertLocalSecretsUseCase(
    private val databaseProvider: DatabaseProvider,
) : AsyncUseCase<UpsertLocalSecretsUseCase.Input, Unit>,
    SelectedAccountUseCase {
    override suspend fun execute(input: Input) {
        if (input.secrets.isEmpty()) return

        val database = databaseProvider.get(selectedAccountId)
        val knownResourceIds =
            input.secrets
                .map { it.resourceId }
                // chunked so the `IN` clause stays well below any SQLite bind variable limit
                .chunked(EXISTENCE_CHECK_CHUNK)
                .flatMap { database.resourcesDao().getExistingResourceIds(it) }
                .toSet()
        val fetchedAt = ZonedDateTime.now(ZoneOffset.UTC)

        val rows =
            input.secrets
                .filter { it.resourceId in knownResourceIds }
                .map {
                    Secret(
                        resourceId = it.resourceId,
                        secretId = it.secretId,
                        armoredData = it.armoredData,
                        modified = it.modified,
                        fetchedAt = fetchedAt,
                    )
                }

        database.secretsDao().upsertAll(rows)
    }

    data class Input(
        val secrets: List<LocalSecret>,
    )

    data class LocalSecret(
        val resourceId: String,
        val secretId: String?,
        val armoredData: String,
        val modified: ZonedDateTime?,
    )

    private companion object {
        private const val EXISTENCE_CHECK_CHUNK = 500
    }
}
