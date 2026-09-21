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
package com.passbolt.mobile.android.data.resources

import com.passbolt.mobile.android.domain.resources.ResourcesLocalDataSource
import com.passbolt.mobile.android.domain.resources.ResourcesRemoteDataSource
import com.passbolt.mobile.android.domain.resources.ResourcesRepository
import com.passbolt.mobile.android.domain.resources.model.Resource
import com.passbolt.mobile.android.domain.resources.model.ResourceWithAttributes
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.entity.resource.ResourceUpdateState
import com.passbolt.mobile.android.ui.HomeDisplayViewModel

@Suppress("TooManyFunctions")
internal class ResourcesRepositoryImpl(
    private val localDataSource: ResourcesLocalDataSource,
    private val remoteDataSource: ResourcesRemoteDataSource,
) : ResourcesRepository {
    override suspend fun getResourcesPage(
        limit: Int,
        page: Int,
    ) = remoteDataSource.getResourcesPage(limit, page)

    override suspend fun createResource(
        resource: CreateResourceDto,
        slug: String,
    ) = remoteDataSource.createResource(resource, slug)

    override suspend fun updateResource(
        resourceId: String,
        resource: CreateResourceDto,
        slug: String,
    ) = remoteDataSource.updateResource(resourceId, resource, slug)

    override suspend fun deleteResource(resourceId: String) = remoteDataSource.deleteResource(resourceId)

    override suspend fun fetchResourcePermissions(resourceId: String) = remoteDataSource.getResourcePermissions(resourceId)

    override suspend fun getLocalResource(
        resourceId: String,
        userId: String,
    ) = localDataSource.getLocalResource(resourceId, userId)

    override suspend fun getLocalResourcePermissions(
        resourceId: String,
        userId: String,
    ) = localDataSource.getLocalResourcePermissions(resourceId, userId)

    override suspend fun getLocalResourceTags(
        resourceId: String,
        userId: String,
    ) = localDataSource.getLocalResourceTags(resourceId, userId)

    override suspend fun getLocalResourcesFilteredByTag(
        tagSearchQuery: String,
        slugs: Set<String>,
        userId: String,
    ) = localDataSource.getLocalResourcesFilteredByTag(tagSearchQuery, slugs, userId)

    override suspend fun getLocalResources(
        slugs: Set<String>,
        homeDisplayView: HomeDisplayViewModel,
        searchQuery: String?,
        userId: String,
    ) = localDataSource.getLocalResources(slugs, homeDisplayView, searchQuery, userId)

    override suspend fun getLocalResourcesWithGroup(
        group: HomeDisplayViewModel.Groups,
        slugs: Set<String>,
        searchQuery: String?,
        userId: String,
    ) = localDataSource.getLocalResourcesWithGroup(group, slugs, searchQuery, userId)

    override suspend fun getLocalResourcesWithTag(
        tag: HomeDisplayViewModel.Tags,
        slugs: Set<String>,
        searchQuery: String?,
        userId: String,
    ) = localDataSource.getLocalResourcesWithTag(tag, slugs, searchQuery, userId)

    override fun getLocalResourcesPaginated(
        slugs: Set<String>,
        homeDisplayView: HomeDisplayViewModel,
        searchQuery: String?,
        pageSize: Int,
        enablePlaceholders: Boolean,
        userId: String,
    ) = localDataSource.getLocalResourcesPaginated(slugs, homeDisplayView, searchQuery, pageSize, enablePlaceholders, userId)

    override fun getLocalResourcesWithGroupPaginated(
        group: HomeDisplayViewModel.Groups,
        slugs: Set<String>,
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ) = localDataSource.getLocalResourcesWithGroupPaginated(group, slugs, searchQuery, pageSize, userId)

    override fun getLocalResourcesWithTagPaginated(
        tag: HomeDisplayViewModel.Tags,
        slugs: Set<String>,
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ) = localDataSource.getLocalResourcesWithTagPaginated(tag, slugs, searchQuery, pageSize, userId)

    override fun getResourcesInFolderPaged(
        folderId: String?,
        slugs: Set<String>,
        searchQuery: String?,
        pageSize: Int,
        enablePlaceholders: Boolean,
        userId: String,
    ) = localDataSource.getResourcesInFolderPaged(folderId, slugs, searchQuery, pageSize, enablePlaceholders, userId)

    override fun getSubFolderResourcesFilteredPaged(
        containingFolders: List<String>,
        containingQuery: String,
        slugs: Set<String>,
        pageSize: Int,
        userId: String,
    ) = localDataSource.getSubFolderResourcesFilteredPaged(containingFolders, containingQuery, slugs, pageSize, userId)

    override suspend fun addLocalResource(
        resource: Resource,
        userId: String,
    ) = localDataSource.addLocalResource(resource, userId)

    override suspend fun addLocalResourcePermissions(
        resources: List<ResourceWithAttributes>,
        userId: String,
    ) = localDataSource.addLocalResourcePermissions(resources, userId)

    override suspend fun updateLocalResource(
        resource: Resource,
        userId: String,
    ) = localDataSource.updateLocalResource(resource, userId)

    override suspend fun upsertLocalResources(
        resources: List<Resource>,
        userId: String,
    ) = localDataSource.upsertLocalResources(resources, userId)

    override suspend fun removeLocalResourcePermissions(userId: String) = localDataSource.removeLocalResourcePermissions(userId)

    override suspend fun removeLocalUris(userId: String) = localDataSource.removeLocalUris(userId)

    override suspend fun removeLocalResourcesWithUpdateState(
        updateState: ResourceUpdateState,
        userId: String,
    ) = localDataSource.removeLocalResourcesWithUpdateState(updateState, userId)

    override suspend fun setLocalResourcesUpdateState(
        updateState: ResourceUpdateState,
        userId: String,
    ) = localDataSource.setLocalResourcesUpdateState(updateState, userId)
}
