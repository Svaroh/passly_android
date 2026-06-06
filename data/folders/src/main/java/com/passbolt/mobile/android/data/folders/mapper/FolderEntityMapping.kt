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
import net.svaroh.passly.domain.folders.model.FolderUpdateState
import net.svaroh.passly.domain.folders.model.FolderWithCountAndPath
import net.svaroh.passly.entity.folder.Folder
import net.svaroh.passly.entity.folder.FolderWithChildItemsCountAndPath
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.entity.folder.FolderUpdateState as EntityFolderUpdateState

internal fun FolderModel.toEntity(
    updateState: EntityFolderUpdateState,
    permissionsModelMapper: PermissionsModelMapper,
): Folder =
    Folder(
        folderId = folderId,
        name = name,
        permission = permissionsModelMapper.map(permission),
        parentId = parentFolderId,
        isShared = isShared,
        modified = modified,
        updateState = updateState,
    )

internal fun Folder.toDomain(permissionsModelMapper: PermissionsModelMapper): FolderModel =
    FolderModel(
        folderId = folderId,
        name = name,
        parentFolderId = parentId,
        isShared = isShared,
        permission = permissionsModelMapper.map(permission),
        modified = modified,
    )

internal fun FolderWithChildItemsCountAndPath.toDomain(permissionsModelMapper: PermissionsModelMapper): FolderWithCountAndPath =
    FolderWithCountAndPath(
        folderId = folderId,
        name = name,
        permission = permissionsModelMapper.map(permission),
        parentId = parentId,
        isShared = isShared,
        subItemsCount = childItemsCount,
        path = path,
    )

internal fun FolderUpdateState.toEntity(): EntityFolderUpdateState =
    when (this) {
        FolderUpdateState.PENDING -> EntityFolderUpdateState.PENDING
        FolderUpdateState.UPDATED -> EntityFolderUpdateState.UPDATED
    }
