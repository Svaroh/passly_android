/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package net.svaroh.passly.data.users.datasource.local

import net.svaroh.passly.data.users.mapper.toDomain
import net.svaroh.passly.data.users.mapper.toEntity
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.domain.users.UsersLocalDataSource
import net.svaroh.passly.domain.users.model.UserProfile
import net.svaroh.passly.entity.user.UserUpdateState.PENDING

internal class UsersLocalDataSourceImpl(
    private val databaseProvider: DatabaseProvider,
) : UsersLocalDataSource {
    override suspend fun getUser(
        selectedAccountId: String,
        userId: String,
    ): UserProfile =
        databaseProvider
            .get(selectedAccountId)
            .usersDao()
            .get(userId)
            .toDomain()

    override suspend fun getCurrentUser(
        selectedAccountId: String,
        serverId: String,
    ): UserProfile =
        databaseProvider
            .get(selectedAccountId)
            .usersDao()
            .get(serverId)
            .toDomain()

    override suspend fun getUsers(
        selectedAccountId: String,
        excludingIds: List<String>,
    ): List<UserProfile> =
        databaseProvider
            .get(selectedAccountId)
            .usersDao()
            .getAllExcluding(excludingIds)
            .map { it.toDomain() }

    override suspend fun upsertUsers(
        selectedAccountId: String,
        users: List<UserProfile>,
    ) {
        val usersDao =
            databaseProvider
                .get(selectedAccountId)
                .usersDao()
        usersDao.setAllUpdateState(PENDING)
        usersDao.upsertAll(users.map { it.toEntity() })
        usersDao.removeWithUpdateState(PENDING)
    }
}
