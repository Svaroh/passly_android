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
package net.svaroh.passly.domain.resources

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.resources.model.Resource
import net.svaroh.passly.domain.resources.model.ResourceWithAttributes
import net.svaroh.passly.domain.resources.model.ResourcesPage
import net.svaroh.passly.dto.request.CreateResourceDto
import net.svaroh.passly.entity.resource.ResourceUpdateState
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.TagModel

@Suppress("TooManyFunctions")
interface ResourcesRepository {
    suspend fun getResourcesPage(
        limit: Int,
        page: Int,
    ): DomainResult<ResourcesPage>

    suspend fun createResource(
        resource: CreateResourceDto,
        slug: String,
    ): DomainResult<ResourceWithAttributes>

    suspend fun updateResource(
        resourceId: String,
        resource: CreateResourceDto,
        slug: String,
    ): DomainResult<Resource>

    suspend fun deleteResource(resourceId: String): DomainResult<Unit>

    suspend fun fetchResourcePermissions(resourceId: String): DomainResult<List<PermissionModel>>

    suspend fun getLocalResource(
        resourceId: String,
        userId: String,
    ): Resource

    suspend fun getLocalResourcePermissions(
        resourceId: String,
        userId: String,
    ): List<PermissionModelUi>

    suspend fun getLocalResourceTags(
        resourceId: String,
        userId: String,
    ): List<TagModel>

    suspend fun getLocalResourcesFilteredByTag(
        tagSearchQuery: String,
        slugs: Set<String>,
        userId: String,
    ): List<Resource>

    suspend fun getLocalResources(
        slugs: Set<String>,
        homeDisplayView: HomeDisplayViewModel,
        searchQuery: String?,
        userId: String,
    ): List<Resource>

    suspend fun getLocalResourcesWithGroup(
        group: HomeDisplayViewModel.Groups,
        slugs: Set<String>,
        searchQuery: String?,
        userId: String,
    ): List<Resource>

    suspend fun getLocalResourcesWithTag(
        tag: HomeDisplayViewModel.Tags,
        slugs: Set<String>,
        searchQuery: String?,
        userId: String,
    ): List<Resource>

    fun getLocalResourcesPaginated(
        slugs: Set<String>,
        homeDisplayView: HomeDisplayViewModel,
        searchQuery: String?,
        pageSize: Int,
        enablePlaceholders: Boolean,
        userId: String,
    ): Flow<PagingData<Resource>>

    fun getLocalResourcesWithGroupPaginated(
        group: HomeDisplayViewModel.Groups,
        slugs: Set<String>,
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<Resource>>

    fun getLocalResourcesWithTagPaginated(
        tag: HomeDisplayViewModel.Tags,
        slugs: Set<String>,
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<Resource>>

    fun getResourcesInFolderPaged(
        folderId: String?,
        slugs: Set<String>,
        searchQuery: String?,
        pageSize: Int,
        enablePlaceholders: Boolean,
        userId: String,
    ): Flow<PagingData<Resource>>

    fun getSubFolderResourcesFilteredPaged(
        containingFolders: List<String>,
        containingQuery: String,
        slugs: Set<String>,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<Resource>>

    suspend fun addLocalResource(
        resource: Resource,
        userId: String,
    )

    suspend fun addLocalResourcePermissions(
        resources: List<ResourceWithAttributes>,
        userId: String,
    )

    suspend fun updateLocalResource(
        resource: Resource,
        userId: String,
    )

    suspend fun upsertLocalResources(
        resources: List<Resource>,
        userId: String,
    )

    suspend fun removeLocalResourcePermissions(userId: String)

    suspend fun removeLocalUris(userId: String)

    suspend fun removeLocalResourcesWithUpdateState(
        updateState: ResourceUpdateState,
        userId: String,
    )

    suspend fun setLocalResourcesUpdateState(
        updateState: ResourceUpdateState,
        userId: String,
    )
}
