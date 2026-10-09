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

package net.svaroh.passly.data.folders.datasource.remote

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.map
import net.svaroh.passly.core.networking.ResponseHandler
import net.svaroh.passly.core.networking.callWithHandler
import net.svaroh.passly.core.networking.toDomainResult
import net.svaroh.passly.data.folders.datasource.remote.api.FoldersApi
import net.svaroh.passly.data.folders.mapper.toFolderModelWithAttributes
import net.svaroh.passly.data.folders.mapper.toFoldersPage
import net.svaroh.passly.domain.folders.datasource.FoldersRemoteDataSource
import net.svaroh.passly.domain.folders.model.FolderModelWithAttributes
import net.svaroh.passly.domain.folders.model.FoldersPage
import net.svaroh.passly.dto.request.CreateFolderRequestDto
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.ui.PermissionModel

internal class FoldersRemoteDataSourceImpl(
    private val foldersApi: FoldersApi,
    private val responseHandler: ResponseHandler,
    private val permissionsModelMapper: PermissionsModelMapper,
) : FoldersRemoteDataSource {
    override suspend fun getFoldersPage(
        limit: Int,
        page: Int,
    ): DomainResult<FoldersPage> =
        callWithHandler(responseHandler) { foldersApi.getFoldersPaginated(limit = limit, page = page) }
            .toDomainResult()
            .map { it.toFoldersPage(permissionsModelMapper) }

    override suspend fun createFolder(
        name: String,
        parentFolderId: String?,
    ): DomainResult<FolderModelWithAttributes> =
        callWithHandler(responseHandler) {
            foldersApi.createFolder(CreateFolderRequestDto(parentFolderId, name)).body
        }.toDomainResult()
            .map { it.toFolderModelWithAttributes(permissionsModelMapper) }

    override suspend fun getFolderPermissions(folderId: String): DomainResult<List<PermissionModel>> =
        callWithHandler(responseHandler) { foldersApi.getFolder(folderId).body }
            .toDomainResult()
            .map { folder -> folder.permissions.map(permissionsModelMapper::map) }
}
