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
package net.svaroh.passly.feature.home.screen

import androidx.paging.compose.LazyPagingItems
import net.svaroh.passly.feature.home.screen.ShowSuggestedModel.Show
import net.svaroh.passly.feature.home.screen.data.HeaderSectionConfiguration
import net.svaroh.passly.ui.FolderWithCountAndPath
import net.svaroh.passly.ui.GroupWithCount
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.HomeDisplayViewModel.Folders
import net.svaroh.passly.ui.ResourceModel
import net.svaroh.passly.ui.TagWithCount

@Suppress("LongParameterList")
internal fun getHeaderConfig(
    resources: LazyPagingItems<ResourceModel>,
    folders: LazyPagingItems<FolderWithCountAndPath>,
    tags: LazyPagingItems<TagWithCount>,
    groups: LazyPagingItems<GroupWithCount>,
    filteredSubfolders: LazyPagingItems<FolderWithCountAndPath>,
    filteredSubfoldersResources: LazyPagingItems<ResourceModel>,
    suggestedResources: LazyPagingItems<ResourceModel>,
    searchQuery: String?,
    homeView: HomeDisplayViewModel,
    showSuggestedModel: ShowSuggestedModel,
): HeaderSectionConfiguration {
    val areAllSectionsEmpty =
        areAllSectionsEmpty(
            resources,
            folders,
            tags,
            groups,
            filteredSubfolders,
            filteredSubfoldersResources,
            suggestedResources,
        )
    val currentFolderName = (homeView as? Folders)?.activeFolderName

    return if (searchQuery.isNullOrBlank()) {
        HeaderSectionConfiguration(
            isInCurrentFolderSectionVisible = false,
            isInSubFoldersSectionVisible = false,
            isOtherItemsSectionVisible =
                !areAllSectionsEmpty && showSuggestedModel is Show && !suggestedResources.itemSnapshotList.isEmpty(),
            isSuggestedSectionVisible = !suggestedResources.itemSnapshotList.isEmpty(),
            currentFolderName = currentFolderName,
            areAllSectionsEmpty = areAllSectionsEmpty,
        )
    } else {
        HeaderSectionConfiguration(
            isInCurrentFolderSectionVisible =
                homeView is Folders &&
                    !(resources.itemSnapshotList.isEmpty() && folders.itemSnapshotList.isEmpty()),
            isInSubFoldersSectionVisible =
                homeView is Folders &&
                    !(
                        filteredSubfolders.itemSnapshotList.isEmpty() &&
                            filteredSubfoldersResources.itemSnapshotList.isEmpty()
                    ),
            isSuggestedSectionVisible = false,
            isOtherItemsSectionVisible = false,
            currentFolderName = currentFolderName,
            areAllSectionsEmpty = areAllSectionsEmpty,
        )
    }
}

private fun areAllSectionsEmpty(vararg lists: LazyPagingItems<*>): Boolean = lists.all { it.itemSnapshotList.isEmpty() }
