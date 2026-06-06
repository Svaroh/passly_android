package net.svaroh.passly.ui

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

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

@Serializable
sealed class HomeDisplayViewModel {
    @Serializable
    object AllItems : HomeDisplayViewModel()

    @Serializable
    object Favourites : HomeDisplayViewModel()

    @Serializable
    object RecentlyModified : HomeDisplayViewModel()

    @Serializable
    object SharedWithMe : HomeDisplayViewModel()

    @Serializable
    object OwnedByMe : HomeDisplayViewModel()

    @Serializable
    object Expiry : HomeDisplayViewModel()

    @Serializable
    object NotLoaded : HomeDisplayViewModel()

    @SuppressLint("UnsafeOptInUsageError") // false positive in K2
    @Serializable
    data class Folders(
        val activeFolder: Folder,
        val activeFolderName: String? = null,
        val isActiveFolderShared: Boolean? = null,
    ) : HomeDisplayViewModel()

    @SuppressLint("UnsafeOptInUsageError") // false positive in K2
    @Serializable
    data class Tags(
        val activeTagId: String? = null,
        val activeTagName: String? = null,
        val isActiveTagShared: Boolean? = null,
    ) : HomeDisplayViewModel()

    @SuppressLint("UnsafeOptInUsageError") // false positive in K2
    @Serializable
    data class Groups(
        val activeGroupId: String? = null,
        val activeGroupName: String? = null,
    ) : HomeDisplayViewModel()

    companion object {
        fun folderRoot() = Folders(Folder.Root)

        fun tagsRoot() = Tags()

        fun groupsRoot() = Groups()
    }
}
