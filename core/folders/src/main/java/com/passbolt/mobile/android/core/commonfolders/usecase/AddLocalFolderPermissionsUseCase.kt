package net.svaroh.passly.core.commonfolders.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.core.accounts.usecase.SelectedAccountUseCase
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.entity.folder.FolderAndUsersCrossRef
import net.svaroh.passly.entity.group.FolderAndGroupsCrossRef
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.ui.FolderModelWithAttributes
import net.svaroh.passly.ui.PermissionModel

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
class AddLocalFolderPermissionsUseCase(
    private val databaseProvider: DatabaseProvider,
    private val permissionsModelMapper: PermissionsModelMapper,
) : AsyncUseCase<AddLocalFolderPermissionsUseCase.Input, Unit>,
    SelectedAccountUseCase {
    override suspend fun execute(input: Input) {
        val db = databaseProvider.get(selectedAccountId)
        val foldersAndGroupsCrossRefDao = db.folderAndGroupsCrossRefDao()
        val foldersAndUsersCrossRefDao = db.folderAndUsersCrossRefDao()

        input.foldersWithAttributes.apply {
            val folderGroupsPermissions =
                map {
                    it.folderModel.folderId to
                        it.folderPermissions
                            .filterIsInstance<PermissionModel.GroupPermissionModel>()
                }

            val folderUsersPermissions =
                map {
                    it.folderModel.folderId to
                        it.folderPermissions
                            .filterIsInstance<PermissionModel.UserPermissionModel>()
                }

            val folderAndGroupCrossRefs =
                folderGroupsPermissions
                    .flatMap { (folderId, groupPermissions) ->
                        groupPermissions.map { groupPermission ->
                            FolderAndGroupsCrossRef(
                                folderId,
                                groupPermission.group.groupId,
                                permissionsModelMapper.map(groupPermission.permission),
                                groupPermission.permissionId,
                            )
                        }
                    }

            val folderAndUsersCrossRefs =
                folderUsersPermissions
                    .flatMap { (folderId, userPermissions) ->
                        userPermissions.map { userPermission ->
                            FolderAndUsersCrossRef(
                                folderId,
                                userPermission.userId,
                                permissionsModelMapper.map(userPermission.permission),
                                userPermission.permissionId,
                            )
                        }
                    }

            foldersAndGroupsCrossRefDao.insertAll(folderAndGroupCrossRefs)
            foldersAndUsersCrossRefDao.insertAll(folderAndUsersCrossRefs)
        }
    }

    data class Input(
        val foldersWithAttributes: List<FolderModelWithAttributes>,
    )
}
