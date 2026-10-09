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

package net.svaroh.passly.data.users.datasource.remote

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.map
import net.svaroh.passly.core.networking.ResponseHandler
import net.svaroh.passly.core.networking.callWithHandler
import net.svaroh.passly.core.networking.toDomainResult
import net.svaroh.passly.data.users.datasource.remote.api.UsersApi
import net.svaroh.passly.data.users.mapper.toDomain
import net.svaroh.passly.domain.users.UsersDataSource
import net.svaroh.passly.domain.users.model.UserProfile

internal class UsersRemoteDataSource(
    private val usersApi: UsersApi,
    private val responseHandler: ResponseHandler,
) : UsersDataSource {
    override suspend fun getMyProfile(): DomainResult<UserProfile> =
        callWithHandler(responseHandler) { usersApi.getMyProfile().body }
            .toDomainResult()
            .map { it.toDomain() }

    override suspend fun getUsers(hasAccessTo: List<String>?): DomainResult<List<UserProfile>> =
        callWithHandler(responseHandler) { usersApi.getUsers(hasAccessTo).body }
            .toDomainResult()
            .map { it.toDomain() }

    override suspend fun getUsersByIds(userIds: List<String>): DomainResult<List<UserProfile>> {
        val userIdsSet = userIds.toSet()
        return callWithHandler(responseHandler) { usersApi.getUsers(hasIds = userIds).body }
            .toDomainResult()
            // the has-id filter may not be supported on older backends according to specs
            // -> filter also after reception
            .map { users -> users.toDomain().filter { it.id in userIdsSet } }
    }
}
