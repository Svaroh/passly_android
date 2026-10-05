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
package net.svaroh.passly.database.impl.secrets

import androidx.room.Dao
import androidx.room.Query
import net.svaroh.passly.database.impl.base.BaseDao
import net.svaroh.passly.entity.secret.Secret

@Dao
interface SecretsDao : BaseDao<Secret> {
    @Query("SELECT * FROM Secret WHERE resourceId = :resourceId")
    suspend fun getSecret(resourceId: String): Secret?

    @Query("SELECT resourceId FROM Secret")
    suspend fun getCachedResourceIds(): List<String>

    @Query("SELECT count(*) FROM Secret")
    suspend fun count(): Int

    @Query("DELETE FROM Secret WHERE resourceId = :resourceId")
    suspend fun delete(resourceId: String)

    @Query("DELETE FROM Secret")
    suspend fun deleteAll()
}
