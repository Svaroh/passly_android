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
package net.svaroh.passly.entity.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Durable state of the sync engine for a given account.
 *
 * Stored in the local encrypted Room database alongside the replica data so that updates to the
 * cursor and data occur in the exact same transaction.
 */
@Entity(tableName = "SyncState")
data class SyncState(
    @PrimaryKey
    val id: Int = SINGLE_ROW_ID,
    val cursor: Long?,
    val epoch: String? = null,
    val lastSyncTimestamp: Long? = null,
    val lastReconcileTimestamp: Long? = null,
    val failureCount: Int = 0,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
