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

package com.passbolt.mobile.android.data.folders.datasource.remote

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.map
import com.passbolt.mobile.android.core.networking.ResponseHandler
import com.passbolt.mobile.android.core.networking.callWithHandler
import com.passbolt.mobile.android.core.networking.toDomainResult
import com.passbolt.mobile.android.data.folders.datasource.remote.api.FoldersApi
import com.passbolt.mobile.android.data.folders.mapper.toFolderModelWithAttributes
import com.passbolt.mobile.android.data.folders.mapper.toFoldersPage
import com.passbolt.mobile.android.domain.folders.datasource.FoldersRemoteDataSource
import com.passbolt.mobile.android.domain.folders.model.FolderModelWithAttributes
import com.passbolt.mobile.android.domain.folders.model.FoldersPage
import com.passbolt.mobile.android.dto.request.CreateFolderRequestDto
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.ui.PermissionModel

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
