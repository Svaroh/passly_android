package net.svaroh.passly.mappers

import net.svaroh.passly.dto.response.FolderResponseDto
import net.svaroh.passly.entity.folder.Folder
import net.svaroh.passly.entity.folder.FolderUpdateState
import net.svaroh.passly.entity.folder.FolderWithChildItemsCountAndPath
import net.svaroh.passly.ui.FolderModel
import net.svaroh.passly.ui.FolderModelWithAttributes
import net.svaroh.passly.ui.FolderWithCountAndPath

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
class FolderModelMapper(
    private val permissionsModelMapper: PermissionsModelMapper,
) {
    fun map(folder: FolderResponseDto): FolderModelWithAttributes {
        val currentUserPermission = folder.permission
        val otherUsersPermissions =
            folder.permissions.filter { folderPermission ->
                folderPermission.aroForeignKey != currentUserPermission.aroForeignKey
            }
        return FolderModelWithAttributes(
            FolderModel(
                folderId = folder.id.toString(),
                parentFolderId = folder.folderParentId?.toString(),
                name = folder.name.orEmpty(),
                isShared = otherUsersPermissions.isNotEmpty(),
                permission = permissionsModelMapper.map(folder.permission.type),
            ),
            folder.permissions.map(permissionsModelMapper::map),
        )
    }

    fun map(folderModel: FolderModel): Folder = map(folderModel, FolderUpdateState.UPDATED)

    fun map(
        folderModel: FolderModel,
        folderUpdateState: FolderUpdateState,
    ): Folder =
        Folder(
            folderId = folderModel.folderId,
            name = folderModel.name,
            permission = permissionsModelMapper.map(folderModel.permission),
            parentId = folderModel.parentFolderId,
            isShared = folderModel.isShared,
            updateState = folderUpdateState,
        )

    fun map(folderEntity: Folder): FolderModel =
        FolderModel(
            folderId = folderEntity.folderId,
            name = folderEntity.name,
            parentFolderId = folderEntity.parentId,
            isShared = folderEntity.isShared,
            permission = permissionsModelMapper.map(folderEntity.permission),
        )

    fun map(folderWithChildItemsCountAndPath: FolderWithChildItemsCountAndPath) =
        FolderWithCountAndPath(
            folderId = folderWithChildItemsCountAndPath.folderId,
            name = folderWithChildItemsCountAndPath.name,
            permission = permissionsModelMapper.map(folderWithChildItemsCountAndPath.permission),
            parentId = folderWithChildItemsCountAndPath.parentId,
            isShared = folderWithChildItemsCountAndPath.isShared,
            subItemsCount = folderWithChildItemsCountAndPath.childItemsCount,
            path = folderWithChildItemsCountAndPath.path,
        )
}
