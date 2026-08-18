package com.passbolt.mobile.android.database.indexbenchmark

import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.folder.FolderUpdateState
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource
import java.time.ZonedDateTime

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

object IndexBenchmarkDataFactory {
    private const val ROOT_EVERY_NTH = 10

    fun createFolders(count: Int): List<Folder> {
        val now = ZonedDateTime.now()
        val folders = ArrayList<Folder>(count)
        for (index in 0 until count) {
            val parentId =
                if (index % ROOT_EVERY_NTH == 0) {
                    null
                } else {
                    folders[index / ROOT_EVERY_NTH].folderId
                }
            folders +=
                Folder(
                    folderId = "folder-$index",
                    name = "Folder $index",
                    permission = Permission.READ,
                    parentId = parentId,
                    isShared = false,
                    modified = now.minusMinutes(index.toLong()),
                    updateState = FolderUpdateState.UPDATED,
                )
        }
        return folders
    }

    fun assignToFolders(
        resources: List<Resource>,
        folders: List<Folder>,
    ): List<Resource> =
        resources.mapIndexed { index, resource ->
            resource.copy(folderId = folders[index % folders.size].folderId)
        }
}
