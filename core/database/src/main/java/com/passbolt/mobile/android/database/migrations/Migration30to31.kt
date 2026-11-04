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
package net.svaroh.passly.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 30 to 31 creates the durable `SyncState` table for incremental delta sync (sync/1 protocol).
 */
@Suppress("MagicNumber")
object Migration30to31 : Migration(30, 31) {
    private const val CREATE_SYNC_STATE_TABLE =
        "CREATE TABLE IF NOT EXISTS `SyncState` (" +
            "`id` INTEGER NOT NULL, " +
            "`cursor` INTEGER, " +
            "`epoch` TEXT, " +
            "`lastSyncTimestamp` INTEGER, " +
            "`lastReconcileTimestamp` INTEGER, " +
            "`failureCount` INTEGER NOT NULL DEFAULT 0, " +
            "PRIMARY KEY(`id`))"

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_SYNC_STATE_TABLE)
    }
}
