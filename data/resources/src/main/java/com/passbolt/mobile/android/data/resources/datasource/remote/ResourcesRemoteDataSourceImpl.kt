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
package net.svaroh.passly.data.resources.datasource.remote

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.map
import net.svaroh.passly.core.networking.ResponseHandler
import net.svaroh.passly.core.networking.callWithHandler
import net.svaroh.passly.core.networking.toDomainResult
import net.svaroh.passly.core.secrets.usecase.db.UpsertLocalSecretsUseCase
import net.svaroh.passly.data.resources.datasource.remote.api.ResourceApi
import net.svaroh.passly.data.resources.mapper.toUiModel
import net.svaroh.passly.domain.resources.ResourcesRemoteDataSource
import net.svaroh.passly.domain.resources.mapper.toDomain
import net.svaroh.passly.domain.resources.model.Resource
import net.svaroh.passly.domain.resources.model.ResourceWithAttributes
import net.svaroh.passly.domain.resources.model.ResourcesPage
import net.svaroh.passly.domain.resourcetypes.usecase.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.dto.request.CreateResourceDto
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.mappers.ResourceModelMapper
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.ResourceUiModelWithAttributes
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

internal class ResourcesRemoteDataSourceImpl(
    private val resourceApi: ResourceApi,
    private val responseHandler: ResponseHandler,
    private val resourceModelMapper: ResourceModelMapper,
    private val permissionsModelMapper: PermissionsModelMapper,
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
) : ResourcesRemoteDataSource {
    override suspend fun getResourcesPage(
        limit: Int,
        page: Int,
    ): DomainResult<ResourcesPage> {
        val slugMapping = resourceTypeIdToSlugMappingProvider.provideMappingForSelectedAccount()
        return callWithHandler(responseHandler) {
            resourceApi.getResourcesPaginated(limit = limit, page = page)
        }.toDomainResult()
            .map { response ->
                ResourcesPage(
                    totalCount = response.header.pagination.count,
                    resources =
                        response.body.map {
                            val slug = requireNotNull(slugMapping[it.resourceTypeId])
                            ResourceUiModelWithAttributes(
                                resourceModelMapper.map(it, slug = slug),
                                it.tags?.map { tag -> tag.toUiModel() }.orEmpty(),
                                it.permissions?.map { permission -> permissionsModelMapper.map(permission) }.orEmpty(),
                                it.favorite?.id?.toString(),
                            ).toDomain()
                        },
                    secrets =
                        response.body.mapNotNull { resource ->
                            resource.secrets?.firstOrNull()?.let { secret ->
                                UpsertLocalSecretsUseCase.LocalSecret(
                                    resourceId = resource.id.toString(),
                                    secretId = secret.id?.toString(),
                                    armoredData = secret.data,
                                    modified = secret.modified?.let(::parseModified),
                                )
                            }
                        },
                )
            }
    }

    private fun parseModified(modified: String): ZonedDateTime? =
        try {
            ZonedDateTime.parse(modified)
        } catch (exception: DateTimeParseException) {
            Timber.w(exception, "Could not parse the secret modification date, storing the secret without it")
            null
        }

    override suspend fun getResourcePermissions(resourceId: String): DomainResult<List<PermissionModel>> =
        callWithHandler(responseHandler) { resourceApi.getResource(resourceId).body }
            .toDomainResult()
            .map { resource -> resource.permissions.orEmpty().map(permissionsModelMapper::map) }

    override suspend fun createResource(
        resource: CreateResourceDto,
        slug: String,
    ): DomainResult<ResourceWithAttributes> =
        callWithHandler(responseHandler) {
            resourceApi.createResource(resource)
        }.toDomainResult()
            .map { response ->
                ResourceUiModelWithAttributes(
                    resourceModelMapper.map(response.body, slug = slug),
                    // cannot add tags during creation
                    emptyList(),
                    listOf(permissionsModelMapper.mapToUserPermission(response.body.permission)),
                    response.body.favorite
                        ?.id
                        ?.toString(),
                ).toDomain()
            }

    override suspend fun updateResource(
        resourceId: String,
        resource: CreateResourceDto,
        slug: String,
    ): DomainResult<Resource> =
        callWithHandler(responseHandler) {
            resourceApi.updateResource(resourceId, resource)
        }.toDomainResult()
            .map { response -> resourceModelMapper.map(response.body, slug = slug).toDomain() }

    override suspend fun deleteResource(resourceId: String): DomainResult<Unit> =
        callWithHandler(responseHandler) {
            resourceApi.deleteResource(resourceId)
        }.toDomainResult()
            .map { }
}
