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

package com.passbolt.mobile.android.data.groups.datasource.remote

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.map
import com.passbolt.mobile.android.core.networking.ResponseHandler
import com.passbolt.mobile.android.core.networking.callWithHandler
import com.passbolt.mobile.android.core.networking.toDomainResult
import com.passbolt.mobile.android.data.groups.datasource.remote.api.GroupsApi
import com.passbolt.mobile.android.data.groups.mapper.toDomain
import com.passbolt.mobile.android.domain.groups.datasource.GroupsRemoteDataSource
import com.passbolt.mobile.android.domain.groups.model.GroupWithMembers

internal class GroupsRemoteDataSourceImpl(
    private val groupsApi: GroupsApi,
    private val responseHandler: ResponseHandler,
) : GroupsRemoteDataSource {
    override suspend fun getGroups(): DomainResult<List<GroupWithMembers>> =
        callWithHandler(responseHandler) { groupsApi.getGroups().body }
            .toDomainResult()
            .map { groups -> groups.map { it.toDomain() } }

    override suspend fun getGroupsByIds(groupIds: List<String>): DomainResult<List<GroupWithMembers>> {
        val groupIdsSet = groupIds.toSet()
        return callWithHandler(responseHandler) { groupsApi.getGroups(hasIds = groupIds).body }
            .toDomainResult()
            // the has-id filter may not be supported on older backends according to specs
            // -> filter also after reception
            .map { groups -> groups.map { it.toDomain() }.filter { it.group.id in groupIdsSet } }
    }
}
