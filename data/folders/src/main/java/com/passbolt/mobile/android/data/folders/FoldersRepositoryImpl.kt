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

package com.passbolt.mobile.android.data.folders

import androidx.paging.PagingData
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.domain.folders.FoldersRepository
import com.passbolt.mobile.android.domain.folders.datasource.FoldersLocalDataSource
import com.passbolt.mobile.android.domain.folders.datasource.FoldersRemoteDataSource
import com.passbolt.mobile.android.domain.folders.model.FolderModel
import com.passbolt.mobile.android.domain.folders.model.FolderModelWithAttributes
import com.passbolt.mobile.android.domain.folders.model.FolderUpdateState
import com.passbolt.mobile.android.domain.folders.model.FolderWithCountAndPath
import com.passbolt.mobile.android.domain.folders.model.FoldersPage
import com.passbolt.mobile.android.domain.folders.model.ParentPermissionItemId
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import kotlinx.coroutines.flow.Flow

internal class FoldersRepositoryImpl(
    private val localDataSource: FoldersLocalDataSource,
    private val remoteDataSource: FoldersRemoteDataSource,
) : FoldersRepository {
    override suspend fun getFoldersPage(
        limit: Int,
        page: Int,
    ): DomainResult<FoldersPage> = remoteDataSource.getFoldersPage(limit, page)

    override suspend fun createFolder(
        name: String,
        parentFolderId: String?,
    ): DomainResult<FolderModelWithAttributes> = remoteDataSource.createFolder(name, parentFolderId)

    override suspend fun fetchFolderPermissions(folderId: String): DomainResult<List<PermissionModel>> =
        remoteDataSource.getFolderPermissions(folderId)

    override suspend fun addFolder(
        folder: FolderModel,
        userId: String,
    ) = localDataSource.addFolder(folder, userId)

    override suspend fun upsertFolders(
        folders: List<FolderModel>,
        userId: String,
    ) = localDataSource.upsertFolders(folders, userId)

    override suspend fun setAllFoldersUpdateState(
        updateState: FolderUpdateState,
        userId: String,
    ) = localDataSource.setAllFoldersUpdateState(updateState, userId)

    override suspend fun removeFoldersWithUpdateState(
        updateState: FolderUpdateState,
        userId: String,
    ) = localDataSource.removeFoldersWithUpdateState(updateState, userId)

    override suspend fun updateFoldersIsShared(
        currentUserServerId: String,
        userId: String,
    ) = localDataSource.updateFoldersIsShared(currentUserServerId, userId)

    override suspend fun addFolderPermissions(
        foldersWithAttributes: List<FolderModelWithAttributes>,
        userId: String,
    ) = localDataSource.addFolderPermissions(foldersWithAttributes, userId)

    override suspend fun clearFolderPermissions(userId: String) = localDataSource.clearFolderPermissions(userId)

    override suspend fun getFolderDetails(
        folderId: String,
        userId: String,
    ): FolderModel = localDataSource.getFolderDetails(folderId, userId)

    override suspend fun getFolderLocation(
        folderId: String,
        userId: String,
    ): List<FolderModel> = localDataSource.getFolderLocation(folderId, userId)

    override suspend fun getFolderPermissions(
        folderId: String,
        userId: String,
    ): List<PermissionModelUi> = localDataSource.getFolderPermissions(folderId, userId)

    override suspend fun getParentFolderPermissionsToApplyToNewItem(
        parentFolderId: String,
        itemId: ParentPermissionItemId,
        currentUserServerId: String,
        userId: String,
    ): List<PermissionModelUi> =
        localDataSource.getParentFolderPermissionsToApplyToNewItem(parentFolderId, itemId, currentUserServerId, userId)

    override fun getDirectChildFoldersPaged(
        folderId: String?,
        searchQuery: String?,
        pageSize: Int,
        enablePlaceholders: Boolean,
        userId: String,
    ): Flow<PagingData<FolderWithCountAndPath>> =
        localDataSource.getDirectChildFoldersPaged(folderId, searchQuery, pageSize, enablePlaceholders, userId)

    override suspend fun getSubFoldersForFolder(
        folder: Folder,
        searchQuery: String?,
        userId: String,
    ): List<FolderWithCountAndPath> = localDataSource.getSubFoldersForFolder(folder, searchQuery, userId)

    override fun getSubFoldersForFolderPaged(
        folder: Folder,
        searchQuery: String?,
        pageSize: Int,
        userId: String,
    ): Flow<PagingData<FolderWithCountAndPath>> = localDataSource.getSubFoldersForFolderPaged(folder, searchQuery, pageSize, userId)
}
