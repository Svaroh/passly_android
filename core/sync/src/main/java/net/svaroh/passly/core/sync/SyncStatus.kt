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

import java.time.ZonedDateTime

/**
 * State of synchronisation as the UI sees it.
 *
 * [Offline] and [Failed] describe the freshness of the local replica, never its availability, so the UI must phrase
 * them neutrally ("data as of 3 August") rather than as application errors.
 */
sealed interface SyncStatus {
    val lastSuccessAt: ZonedDateTime?

    data class Idle(
        override val lastSuccessAt: ZonedDateTime?,
    ) : SyncStatus

    data class Running(
        override val lastSuccessAt: ZonedDateTime?,
        val phase: Phase,
        val processed: Int = 0,
        val total: Int? = null,
    ) : SyncStatus {
        enum class Phase {
            PUSHING_LOCAL_CHANGES,
            BOOTSTRAP,
            DELTA,
            RECONCILE,
        }
    }

    data class Offline(
        override val lastSuccessAt: ZonedDateTime?,
    ) : SyncStatus

    data class Failed(
        override val lastSuccessAt: ZonedDateTime?,
        val reason: SyncFailureReason,
        val retryAt: ZonedDateTime?,
    ) : SyncStatus
}
