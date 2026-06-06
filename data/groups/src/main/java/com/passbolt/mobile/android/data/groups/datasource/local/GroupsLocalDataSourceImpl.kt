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

package net.svaroh.passly.data.groups.datasource.local

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import net.svaroh.passly.data.groups.mapper.toDomain
import net.svaroh.passly.data.groups.mapper.toEntity
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.database.QuerySanitizer
import net.svaroh.passly.domain.groups.datasource.GroupsLocalDataSource
import net.svaroh.passly.domain.groups.model.Group
import net.svaroh.passly.domain.groups.model.GroupWithItemsCount
import net.svaroh.passly.domain.groups.model.GroupWithMembers
import net.svaroh.passly.domain.groups.model.GroupWithUsers
import net.svaroh.passly.entity.group.GroupUpdateState.PENDING
import net.svaroh.passly.entity.group.UsersAndGroupCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class GroupsLocalDataSourceImpl(
    private val databaseProvider: DatabaseProvider,
    private val querySanitizer: QuerySanitizer,
) : GroupsLocalDataSource {
    override suspend fun getGroups(
        excludingIds: List<String>,
        userId: String,
    ): List<Group> =
        databaseProvider
            .get(userId)
            .groupsDao()
            .getAllExcluding(excludingIds)
            .map { it.toDomain() }

    override fun getGroupsWithItemsCountPaged(
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<GroupWithItemsCount>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = {
                databaseProvider
                    .get(userId)
                    .paginatedGroupsDao()
                    .getAllWithSharedItemsCount(querySanitizer.sanitize(searchQuery))
            },
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override suspend fun getGroupWithUsers(
        groupId: String,
        userId: String,
    ): GroupWithUsers =
        databaseProvider
            .get(userId)
            .groupsDao()
            .getGroupWithUsers(groupId)
            .toDomain()

    override suspend fun upsertGroups(
        groups: List<GroupWithMembers>,
        userId: String,
    ) {
        val database = databaseProvider.get(userId)
        val groupsDao = database.groupsDao()
        val usersAndGroupsCrossRefDao = database.usersAndGroupsCrossRefDao()

        val usersAndGroupsCrossRefs =
            groups.flatMap { group ->
                group.members.map { member ->
                    UsersAndGroupCrossRef(member.userId, group.group.id)
                }
            }

        // group the upsert + delete-stale writes into one transaction so Room invalidates observers once
        databaseProvider.inTransaction(userId) {
            groupsDao.setAllUpdateState(PENDING)
            usersAndGroupsCrossRefDao.deleteAll()
            groupsDao.upsertAll(groups.map { it.group.toEntity() })
            usersAndGroupsCrossRefDao.insertAll(usersAndGroupsCrossRefs)
            groupsDao.removeWithUpdateState(PENDING)
        }
    }
}
