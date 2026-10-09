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

package net.svaroh.passly.domain.resources.usecase

import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.folders.usecase.FetchFolderPermissionsUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.ResourcePermission
import timber.log.Timber

class CreatePermissionsConfirmationInteractor(
    private val fetchFolderPermissionsUseCase: FetchFolderPermissionsUseCase,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
) {
    suspend fun shouldConfirmPermissions(parentFolderId: String?): Boolean {
        val folderId = parentFolderId ?: return false
        val fetchOutput =
            runAuthenticatedOperation {
                fetchFolderPermissionsUseCase.execute(FetchFolderPermissionsUseCase.Input(folderId))
            }
        return when (fetchOutput) {
            is FetchFolderPermissionsUseCase.Output.Success -> isShared(fetchOutput.permissions)
            is FetchFolderPermissionsUseCase.Output.Failure -> {
                Timber.e(
                    "Failed to fetch folder permissions to decide on confirmation: ${fetchOutput.message} - " +
                        "falling back to local permissions",
                )
                isFolderSharedLocally(folderId)
            }
        }
    }

    private fun isShared(permissions: List<PermissionModel>): Boolean {
        val currentUserServerId = getSelectedAccountDataUseCase.execute(Unit).serverId
        val isOperatorDirectOwnershipOnly =
            permissions.singleOrNull()?.let {
                it is PermissionModel.UserPermissionModel &&
                    it.userId == currentUserServerId &&
                    it.permission == ResourcePermission.OWNER
            } == true
        return permissions.isNotEmpty() && !isOperatorDirectOwnershipOnly
    }

    private suspend fun isFolderSharedLocally(folderId: String): Boolean {
        val folderPermissions =
            getLocalFolderPermissionsUseCase
                .execute(GetLocalFolderPermissionsUseCase.Input(folderId))
                .permissions
        val currentUserServerId = getSelectedAccountDataUseCase.execute(Unit).serverId
        val isOperatorDirectOwnershipOnly =
            folderPermissions.singleOrNull()?.let {
                it is PermissionModelUi.UserPermissionModel &&
                    it.user.userId == currentUserServerId &&
                    it.permission == ResourcePermission.OWNER
            } == true
        return folderPermissions.isNotEmpty() && !isOperatorDirectOwnershipOnly
    }
}
