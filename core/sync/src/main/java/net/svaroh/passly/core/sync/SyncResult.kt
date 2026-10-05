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

/**
 * Outcome of a single synchronisation run.
 *
 * None of these values ever makes local data unavailable - a failed or skipped run only means the local replica is
 * less fresh than it could be.
 */
sealed interface SyncResult {
    data class Success(
        val stats: SyncStats,
    ) : SyncResult

    /**
     * The run did not start. This is the normal outcome with no connectivity and is not an error.
     */
    data class Skipped(
        val reason: Reason,
    ) : SyncResult {
        enum class Reason {
            NO_NETWORK,
            ALREADY_RUNNING,
            NO_ACCOUNT,
        }
    }

    /**
     * The run started and could not finish. The cursor stays where the last committed page left it, so the next run
     * resumes instead of restarting.
     */
    data class Failure(
        val reason: SyncFailureReason,
    ) : SyncResult
}

enum class SyncFailureReason {
    /** Server unreachable, DNS failure, timeout. */
    UNREACHABLE,

    /** Session could not be renewed silently; local data is untouched. */
    SESSION,

    /** MFA is required and the run was not user initiated. */
    MFA_REQUIRED,

    /** Server rejected the request or answered with something this protocol version does not understand. */
    PROTOCOL,

    /** Writing the batch to the local database failed. */
    STORAGE,
}

data class SyncStats(
    val upserted: Int = 0,
    val deleted: Int = 0,
    val secretsUpdated: Int = 0,
    val pushedOperations: Int = 0,
    val reconciled: Boolean = false,
) {
    val isEmpty: Boolean
        get() = upserted == 0 && deleted == 0 && secretsUpdated == 0 && pushedOperations == 0
}
