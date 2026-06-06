package net.svaroh.passly.data.tags

import androidx.paging.PagingData
import net.svaroh.passly.domain.tags.TagsRepository
import net.svaroh.passly.domain.tags.datasource.TagsLocalDataSource
import net.svaroh.passly.ui.ResourceUiModelWithAttributes
import net.svaroh.passly.ui.TagWithCount
import kotlinx.coroutines.flow.Flow

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
internal class TagsRepositoryImpl(
    private val localDataSource: TagsLocalDataSource,
) : TagsRepository {
    override suspend fun addTags(
        resourcesWithTags: List<ResourceUiModelWithAttributes>,
        userId: String,
    ) = localDataSource.addTags(resourcesWithTags, userId)

    override suspend fun getTagsWithCount(
        searchQuery: String?,
        userId: String,
    ): List<TagWithCount> = localDataSource.getTagsWithCount(searchQuery, userId)

    override fun getTagsWithCountPaginated(
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<TagWithCount>> = localDataSource.getTagsWithCountPaginated(searchQuery, pageSize, userId)

    override suspend fun removeTags(userId: String) = localDataSource.removeTags(userId)
}
