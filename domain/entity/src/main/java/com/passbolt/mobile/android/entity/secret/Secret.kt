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
package net.svaroh.passly.entity.secret

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import net.svaroh.passly.entity.resource.Resource
import java.time.ZonedDateTime

/**
 * The OpenPGP ciphertext of a resource secret, stored exactly as the server sent it.
 *
 * This row is what makes the local replica autonomous: without it, showing a password requires the server. It is
 * never expired, never invalidated by the server and never removed on a time basis - it goes away only when its
 * resource goes away (cascade), or when the user erases local data.
 *
 * [armoredData] is PGP ciphertext inside an SQLCipher database; plaintext exists only in memory.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = CASCADE,
        ),
    ],
)
data class Secret(
    @PrimaryKey
    val resourceId: String,
    /** Server side secret id; null when the row was cached from an endpoint that does not report it. */
    val secretId: String?,
    val armoredData: String,
    /** Server side modification time of the secret, used by reconciliation to spot stale local copies. */
    val modified: ZonedDateTime?,
    /** When this device stored the row. Diagnostics only - it must never be used to expire the row. */
    val fetchedAt: ZonedDateTime,
)
