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
package net.svaroh.passly.database.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.database.DatabaseProvider
import timber.log.Timber

/**
 * Answers whether an account already carries a usable local replica on this device.
 *
 * It is what lets the app decide that a missing server is a freshness problem rather than a blocker: an account that
 * has synchronised once can always be unlocked and used offline. An account that never synchronised has nothing to
 * show, so it still needs the server for its first run.
 *
 * The caller must have made the account the selected one first - the database key is stored per selected account.
 */
class HasLocalReplicaUseCase(
    private val databaseProvider: DatabaseProvider,
) : AsyncUseCase<UserIdInput, HasLocalReplicaUseCase.Output> {
    override suspend fun execute(input: UserIdInput): Output =
        try {
            val resourceCount =
                databaseProvider
                    .get(input.userId)
                    .resourcesDao()
                    .countAll()
            Timber.d("Local replica holds $resourceCount resources")
            Output(hasLocalReplica = resourceCount > 0)
        } catch (exception: Exception) {
            // an unreadable or not yet created database simply means there is no replica to fall back on
            Timber.w(exception, "Could not inspect the local replica")
            Output(hasLocalReplica = false)
        }

    data class Output(
        val hasLocalReplica: Boolean,
    )
}
