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

import java.time.Duration

/**
 * Retry schedule for failed background runs, as specified in `docs/sync-protocol.md`.
 *
 * The ladder saturates at six hours and never gives up: a server that is unreachable for a year must not leave the
 * client in a permanently broken state, it just keeps checking rarely.
 */
object SyncBackoff {
    private const val FIRST_RETRY_SECONDS = 30L
    private const val SECOND_RETRY_MINUTES = 1L
    private const val THIRD_RETRY_MINUTES = 5L
    private const val FOURTH_RETRY_MINUTES = 15L
    private const val FIFTH_RETRY_HOURS = 1L
    private const val SATURATED_RETRY_HOURS = 6L

    private val LADDER =
        listOf(
            Duration.ofSeconds(FIRST_RETRY_SECONDS),
            Duration.ofMinutes(SECOND_RETRY_MINUTES),
            Duration.ofMinutes(THIRD_RETRY_MINUTES),
            Duration.ofMinutes(FOURTH_RETRY_MINUTES),
            Duration.ofHours(FIFTH_RETRY_HOURS),
            Duration.ofHours(SATURATED_RETRY_HOURS),
        )

    /**
     * @param consecutiveFailures how many runs failed in a row, `1` for the first failure.
     */
    fun delayAfter(consecutiveFailures: Int): Duration {
        require(consecutiveFailures >= 1) { "consecutiveFailures must be at least 1" }
        return LADDER[(consecutiveFailures - 1).coerceAtMost(LADDER.lastIndex)]
    }

    /**
     * A user initiated run bypasses the ladder - if someone presses "synchronise now", they get an attempt now.
     */
    fun delayAfter(
        consecutiveFailures: Int,
        trigger: SyncTrigger,
    ): Duration = if (trigger.isUserInitiated) Duration.ZERO else delayAfter(consecutiveFailures)
}
