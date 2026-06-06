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

package net.svaroh.passly.domain.folders.datasource

import androidx.paging.PagingData
import net.svaroh.passly.domain.folders.model.FolderModel
import net.svaroh.passly.domain.folders.model.FolderModelWithAttributes
import net.svaroh.passly.domain.folders.model.FolderUpdateState
import net.svaroh.passly.domain.folders.model.FolderWithCountAndPath
import net.svaroh.passly.domain.folders.model.ParentPermissionItemId
import net.svaroh.passly.ui.Folder
import net.svaroh.passly.ui.PermissionModelUi
import kotlinx.coroutines.flow.Flow

interface FoldersLocalDataSource {
    suspend fun addFolder(
        folder: FolderModel,
        userId: String,
    )

    suspend fun upsertFolders(
        folders: List<FolderModel>,
        userId: String,
    )

    suspend fun setAllFoldersUpdateState(
        updateState: FolderUpdateState,
        userId: String,
    )

    suspend fun removeFoldersWithUpdateState(
        updateState: FolderUpdateState,
        userId: String,
    )

    suspend fun updateFoldersIsShared(
        currentUserServerId: String,
        userId: String,
    )

    suspend fun addFolderPermissions(
        foldersWithAttributes: List<FolderModelWithAttributes>,
        userId: String,
    )

    suspend fun clearFolderPermissions(userId: String)

    suspend fun getFolderDetails(
        folderId: String,
        userId: String,
    ): FolderModel

    suspend fun getFolderLocation(
        folderId: String,
        userId: String,
    ): List<FolderModel>

    suspend fun getFolderPermissions(
        folderId: String,
        userId: String,
    ): List<PermissionModelUi>

    suspend fun getParentFolderPermissionsToApplyToNewItem(
        parentFolderId: String,
        itemId: ParentPermissionItemId,
        currentUserServerId: String,
        userId: String,
    ): List<PermissionModelUi>

    fun getDirectChildFoldersPaged(
        folderId: String?,
        searchQuery: String?,
        pageSize: Int,
        enablePlaceholders: Boolean,
        userId: String,
    ): Flow<PagingData<FolderWithCountAndPath>>

    suspend fun getSubFoldersForFolder(
        folder: Folder,
        searchQuery: String?,
        userId: String,
    ): List<FolderWithCountAndPath>

    fun getSubFoldersForFolderPaged(
        folder: Folder,
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<FolderWithCountAndPath>>
}
