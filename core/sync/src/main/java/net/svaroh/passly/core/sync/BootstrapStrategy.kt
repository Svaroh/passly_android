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
package net.svaroh.passly.core.sync

import net.svaroh.passly.core.sync.api.SyncApi
import timber.log.Timber

sealed interface BootstrapResult {
    data class Success(
        val stats: SyncStats,
        val pinnedCursor: Long,
        val epoch: String,
    ) : BootstrapResult

    data class Failure(
        val cause: Throwable,
    ) : BootstrapResult
}

/**
 * Executes initial or recovery bootstrap synchronization against /sync/bootstrap.json.
 * Pins cursor on page 1 and downloads paginated chunks directly into the local replica.
 */
class BootstrapStrategy(
    private val syncApi: SyncApi,
    private val changesApplier: ChangesApplier,
) {
    suspend fun execute(limit: Int = 500): BootstrapResult {
        var page = 1
        var pinnedCursor: Long? = null
        var pinnedEpoch: String? = null
        var totalStats = SyncStats.EMPTY

        try {
            while (true) {
                Timber.d("Querying /sync/bootstrap.json?page=$page&limit=$limit&cursor=$pinnedCursor")
                val response = syncApi.getBootstrap(page = page, limit = limit, cursor = pinnedCursor)
                val body = response.body
                val pagination = response.header.pagination

                if (pinnedCursor == null) {
                    pinnedCursor = body.cursor
                    pinnedEpoch = body.epoch
                }

                val pageStats = changesApplier.applyBootstrapPage(body, page)
                totalStats = totalStats + pageStats

                val totalCount = pagination.count
                val currentLimit = pagination.limit ?: limit
                val isLastPage = (page * currentLimit) >= totalCount || totalCount == 0

                if (isLastPage) {
                    break
                }
                page++
            }

            val finalCursor = pinnedCursor ?: 0L
            val finalEpoch = pinnedEpoch.orEmpty()

            return BootstrapResult.Success(
                stats = totalStats,
                pinnedCursor = finalCursor,
                epoch = finalEpoch,
            )
        } catch (e: Exception) {
            Timber.e(e, "Error during bootstrap sync")
            return BootstrapResult.Failure(e)
        }
    }
}
