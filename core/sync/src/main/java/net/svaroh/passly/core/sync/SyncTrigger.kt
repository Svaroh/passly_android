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
 * What asked for a synchronisation run.
 *
 * The trigger decides how loud the run is allowed to be: only [MANUAL] may interrupt the user with an MFA prompt,
 * and only [MANUAL] forces a reconciliation pass on top of the delta.
 */
enum class SyncTrigger {
    /** The user pressed "synchronise now" or pulled to refresh. */
    MANUAL,

    /** The account was just unlocked. */
    UNLOCK,

    /** A queued local write is waiting to be pushed. */
    PENDING_WRITE,

    /** Periodic background run scheduled by WorkManager. */
    BACKGROUND,
    ;

    val isUserInitiated: Boolean
        get() = this == MANUAL
}
