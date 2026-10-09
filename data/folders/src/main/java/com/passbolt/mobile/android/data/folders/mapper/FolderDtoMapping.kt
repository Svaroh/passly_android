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

package net.svaroh.passly.data.folders.mapper

import net.svaroh.passly.domain.folders.model.FolderModel
import net.svaroh.passly.domain.folders.model.FolderModelWithAttributes
import net.svaroh.passly.domain.folders.model.FoldersPage
import net.svaroh.passly.dto.response.BasePaginatedResponse
import net.svaroh.passly.dto.response.CreateFolderResponseDto
import net.svaroh.passly.dto.response.FolderResponseDto
import net.svaroh.passly.mappers.PermissionsModelMapper
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

private val EPOCH: ZonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)

internal fun BasePaginatedResponse<List<FolderResponseDto>>.toFoldersPage(permissionsModelMapper: PermissionsModelMapper): FoldersPage =
    FoldersPage(
        folders = body.map { it.toFolderModelWithAttributes(permissionsModelMapper) },
        totalCount = header.pagination.count,
    )

internal fun FolderResponseDto.toFolderModelWithAttributes(permissionsModelMapper: PermissionsModelMapper): FolderModelWithAttributes =
    FolderModelWithAttributes(
        FolderModel(
            folderId = id.toString(),
            parentFolderId = folderParentId?.toString(),
            name = name.orEmpty(),
            // updated in batch after folders and permissions are inserted
            isShared = false,
            permission = permissionsModelMapper.map(permission.type),
            modified = modified?.let(ZonedDateTime::parse) ?: EPOCH,
        ),
        permissions.map(permissionsModelMapper::map),
    )

internal fun CreateFolderResponseDto.toFolderModelWithAttributes(
    permissionsModelMapper: PermissionsModelMapper,
): FolderModelWithAttributes =
    FolderModelWithAttributes(
        FolderModel(
            folderId = id.toString(),
            parentFolderId = parentFolderId?.toString(),
            name = name,
            isShared = !personal,
            permission = permissionsModelMapper.map(permission.type),
            modified = modified?.let(ZonedDateTime::parse) ?: ZonedDateTime.now(),
        ),
        listOf(permissionsModelMapper.mapToUserPermission(permission)),
    )
