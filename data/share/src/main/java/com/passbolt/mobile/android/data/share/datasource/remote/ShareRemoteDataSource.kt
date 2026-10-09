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

package net.svaroh.passly.data.share.datasource.remote

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.map
import net.svaroh.passly.core.networking.ResponseHandler
import net.svaroh.passly.core.networking.callWithHandler
import net.svaroh.passly.core.networking.toDomainResult
import net.svaroh.passly.data.share.datasource.remote.api.ShareApi
import net.svaroh.passly.data.share.mapper.toDomain
import net.svaroh.passly.data.share.mapper.toDto
import net.svaroh.passly.domain.share.ShareDataSource
import net.svaroh.passly.domain.share.model.EncryptedSecret
import net.svaroh.passly.domain.share.model.ShareChanges
import net.svaroh.passly.domain.share.model.SharePermission
import net.svaroh.passly.dto.request.FolderShareRequest
import net.svaroh.passly.dto.request.ResourceShareRequest
import net.svaroh.passly.dto.request.SimulateShareRequest

internal class ShareRemoteDataSource(
    private val shareApi: ShareApi,
    private val responseHandler: ResponseHandler,
) : ShareDataSource {
    override suspend fun simulateShareResource(
        resourceId: String,
        permissions: List<SharePermission>,
    ): DomainResult<ShareChanges> =
        callWithHandler(responseHandler) {
            shareApi.simulateShareResource(resourceId, SimulateShareRequest(permissions.map { it.toDto() })).body
        }.toDomainResult().map { it.toDomain() }

    override suspend fun shareResource(
        resourceId: String,
        permissions: List<SharePermission>,
        secrets: List<EncryptedSecret>,
    ): DomainResult<Unit> =
        callWithHandler(responseHandler) {
            shareApi
                .shareResource(
                    resourceId,
                    ResourceShareRequest(permissions.map { it.toDto() }, secrets.map { it.toDto() }),
                ).body
        }.toDomainResult()

    override suspend fun shareFolder(
        folderId: String,
        permissions: List<SharePermission>,
    ): DomainResult<Unit> =
        callWithHandler(responseHandler) {
            shareApi.shareFolder(folderId, FolderShareRequest(permissions.map { it.toDto() })).body
        }.toDomainResult()
}
