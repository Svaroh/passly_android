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
import retrofit2.HttpException
import timber.log.Timber

sealed interface DeltaSyncResult {
    data class Success(
        val stats: SyncStats,
        val newCursor: Long,
    ) : DeltaSyncResult

    data object Gone : DeltaSyncResult

    data class Failure(
        val cause: Throwable,
    ) : DeltaSyncResult
}

/**
 * Executes incremental delta synchronization against /sync/changes.json starting from local cursor.
 * Handles multi-page deltas (has_more) and gracefully reports 410 Gone when cursor is purged.
 */
class DeltaSyncStrategy(
    private val syncApi: SyncApi,
    private val changesApplier: ChangesApplier,
) {
    suspend fun execute(
        sinceCursor: Long,
        epoch: String? = null,
        limit: Int = 500,
    ): DeltaSyncResult {
        var currentCursor = sinceCursor
        var accumulatedStats = SyncStats.EMPTY

        try {
            var hasMore = true
            while (hasMore) {
                Timber.d("Querying /sync/changes.json?since=$currentCursor&limit=$limit")
                val response = syncApi.getChanges(since = currentCursor, limit = limit)
                val body = response.body

                val pageStats = changesApplier.applyChanges(body, epoch = epoch)
                accumulatedStats = accumulatedStats + pageStats
                currentCursor = body.cursor
                hasMore = body.hasMore
            }

            return DeltaSyncResult.Success(
                stats = accumulatedStats,
                newCursor = currentCursor,
            )
        } catch (e: HttpException) {
            if (e.code() == HTTP_GONE) {
                Timber.w("Server returned 410 Gone for cursor $currentCursor; retention expired or epoch mismatch.")
                return DeltaSyncResult.Gone
            }
            Timber.e(e, "HTTP error during delta sync")
            return DeltaSyncResult.Failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during delta sync")
            return DeltaSyncResult.Failure(e)
        }
    }

    private companion object {
        const val HTTP_GONE = 410
    }
}
