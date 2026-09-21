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
package com.passbolt.mobile.android.feature.home.screen.data

import androidx.paging.PagingData
import androidx.paging.filter
import com.passbolt.mobile.android.common.urimatcher.AutofillUriMatcher
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalDirectChildFoldersPaginatedUseCase
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalSubFoldersForFolderPaginatedUseCase
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.domain.groups.usecase.GetLocalGroupsWithShareItemsCountPaginatedUseCase
import com.passbolt.mobile.android.domain.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcesPaginatedUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcesWithGroupPaginatedUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcesWithTagPaginatedUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalSubFolderResourcesFilteredPaginatedUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetResourcesInFolderPaginatedUseCase
import com.passbolt.mobile.android.domain.tags.usecase.GetLocalTagsPaginatedUseCase
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.homeSlugs
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.AllItems
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Expiry
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Favourites
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Groups
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.NotLoaded
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.OwnedByMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.RecentlyModified
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.SharedWithMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Tags
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.ResourceUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HomeDataProvider(
    private val getLocalResourcesPaginatedUseCase: GetLocalResourcesPaginatedUseCase,
    private val getLocalTagsPaginatedUseCase: GetLocalTagsPaginatedUseCase,
    private val getLocalResourcesWithLocalTagsPaginatedUseCase: GetLocalResourcesWithTagPaginatedUseCase,
    private val getLocalGroupsWithShareItemsCountPaginatedUseCase: GetLocalGroupsWithShareItemsCountPaginatedUseCase,
    private val getLocalResourcesWithGroupsPaginatedUseCase: GetLocalResourcesWithGroupPaginatedUseCase,
    private val getLocalDirectChildFoldersPaginatedUseCase: GetLocalDirectChildFoldersPaginatedUseCase,
    private val getResourcesInFolderPaginatedUseCase: GetResourcesInFolderPaginatedUseCase,
    private val getLocalSubFolderResourcesFilteredPaginatedUseCase: GetLocalSubFolderResourcesFilteredPaginatedUseCase,
    private val getLocalSubFoldersForFolderUseCase: GetLocalSubFoldersForFolderUseCase,
    private val getLocalSubFoldersForFolderPaginatedUseCase: GetLocalSubFoldersForFolderPaginatedUseCase,
    private val autofillMatcher: AutofillUriMatcher,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
) {
    suspend fun provideData(
        searchQuery: String?,
        homeView: HomeDisplayViewModel,
        showSuggestedModel: ShowSuggestedModel,
        slugs: Set<String> = homeSlugs,
    ): HomeData =
        when (homeView) {
            AllItems,
            Expiry,
            Favourites,
            OwnedByMe,
            RecentlyModified,
            SharedWithMe,
            -> getResourcesHomeData(searchQuery, homeView, showSuggestedModel, slugs)

            is Tags -> getTagsHomeData(searchQuery, homeView, showSuggestedModel, slugs)
            is Groups -> getGroupsHomeData(searchQuery, homeView, showSuggestedModel, slugs)
            is Folders -> getFoldersHomeData(searchQuery, homeView, showSuggestedModel, slugs)
            NotLoaded -> HomeData()
        }

    private suspend fun getFoldersHomeData(
        searchQuery: String?,
        foldersView: Folders,
        showSuggestedModel: ShowSuggestedModel,
        slugs: Set<String>,
    ): HomeData {
        if (getRbacRulesUseCase.execute(Unit).rbacModel.foldersUseRule != ALLOW) {
            return HomeData()
        }

        val folders =
            getLocalDirectChildFoldersPaginatedUseCase
                .execute(
                    GetLocalDirectChildFoldersPaginatedUseCase.Input(
                        foldersView.activeFolder.folderId,
                        searchQuery,
                        enablePlaceholders = true,
                    ),
                ).folders

        val resources =
            getResourcesInFolderPaginatedUseCase
                .execute(
                    GetResourcesInFolderPaginatedUseCase.Input(
                        foldersView.activeFolder.folderId,
                        slugs,
                        searchQuery,
                        enablePlaceholders = true,
                    ),
                ).resources

        return if (searchQuery.isNullOrBlank()) {
            HomeData(
                resourceList = resources,
                foldersList = folders,
            )
        } else {
            // resources need to be shown for all child folders
            val allSubFolders =
                getLocalSubFoldersForFolderUseCase
                    .execute(
                        GetLocalSubFoldersForFolderUseCase.Input(foldersView.activeFolder, searchQuery),
                    ).folders

            val allSubFoldersPaginated =
                getLocalSubFoldersForFolderPaginatedUseCase
                    .execute(
                        GetLocalSubFoldersForFolderPaginatedUseCase.Input(foldersView.activeFolder, searchQuery),
                    ).folders

            val filteredSubFolderResources =
                getLocalSubFolderResourcesFilteredPaginatedUseCase
                    .execute(
                        GetLocalSubFolderResourcesFilteredPaginatedUseCase.Input(
                            allSubFolders.map { it.folderId },
                            searchQuery,
                            slugs,
                        ),
                    ).resources

            HomeData(
                resourceList = resources,
                foldersList = folders,
                filteredSubFolderResources = filteredSubFolderResources,
                filteredSubFolders = allSubFoldersPaginated,
                suggestedResourceList = getSuggestedList(resources, searchQuery, foldersView, showSuggestedModel),
            )
        }
    }

    private suspend fun getGroupsHomeData(
        searchQuery: String?,
        groupsView: Groups,
        showSuggestedModel: ShowSuggestedModel,
        slugs: Set<String>,
    ): HomeData {
        val groups =
            getLocalGroupsWithShareItemsCountPaginatedUseCase
                .execute(
                    GetLocalGroupsWithShareItemsCountPaginatedUseCase.Input(searchQuery),
                ).pagedGroupsFlow
        return if (groupsView.activeGroupId == null) { // groups root - list of groups
            HomeData(groupsList = groups)
        } else { // resources shared with group
            val groupResources =
                getLocalResourcesWithGroupsPaginatedUseCase
                    .execute(
                        GetLocalResourcesWithGroupPaginatedUseCase.Input(
                            groupsView,
                            slugs,
                            searchQuery,
                        ),
                    ).resources

            HomeData(
                resourceList = groupResources,
                suggestedResourceList = getSuggestedList(groupResources, searchQuery, groupsView, showSuggestedModel),
            )
        }
    }

    private suspend fun getResourcesHomeData(
        searchQuery: String?,
        homeView: HomeDisplayViewModel,
        showSuggestedModel: ShowSuggestedModel,
        slugs: Set<String>,
    ): HomeData {
        val resourceList =
            getLocalResourcesPaginatedUseCase
                .execute(
                    GetLocalResourcesPaginatedUseCase.Input(
                        slugs,
                        homeView,
                        searchQuery,
                        enablePlaceholders = true,
                    ),
                ).pagedResourcesFlow

        val suggestedSource =
            getLocalResourcesPaginatedUseCase
                .execute(
                    GetLocalResourcesPaginatedUseCase.Input(
                        slugs,
                        homeView,
                        searchQuery,
                    ),
                ).pagedResourcesFlow

        return HomeData(
            resourceList = resourceList,
            suggestedResourceList = getSuggestedList(suggestedSource, searchQuery, homeView, showSuggestedModel),
        )
    }

    private fun getSuggestedList(
        resourceList: Flow<PagingData<ResourceUiModel>>,
        searchQuery: String?,
        homeView: HomeDisplayViewModel,
        showSuggestedModel: ShowSuggestedModel,
    ): Flow<PagingData<ResourceUiModel>> =
        if (showSuggestedModel is ShowSuggestedModel.Show && shouldShowSuggested(homeView, searchQuery)) {
            resourceList.map { pagingData ->
                pagingData.filter {
                    autofillMatcher.isMatching(
                        showSuggestedModel.suggestedUri,
                        it.metadataJsonModel.uris.orEmpty() + it.metadataJsonModel.uri.orEmpty(),
                    )
                }
            }
        } else {
            settledEmptyPagingData()
        }

    private suspend fun getTagsHomeData(
        searchQuery: String?,
        tagsView: Tags,
        showSuggestedModel: ShowSuggestedModel,
        slugs: Set<String>,
    ): HomeData {
        if (getRbacRulesUseCase.execute(Unit).rbacModel.tagsUseRule != ALLOW) {
            return HomeData()
        }

        val tags = getLocalTagsPaginatedUseCase.execute(GetLocalTagsPaginatedUseCase.Input(searchQuery)).pagedTagsFlow
        return if (tagsView.activeTagId == null) { // tags root - list of tags
            HomeData(tagsList = tags)
        } else { // resources with active tag
            val taggedResources =
                getLocalResourcesWithLocalTagsPaginatedUseCase
                    .execute(
                        GetLocalResourcesWithTagPaginatedUseCase.Input(
                            tagsView,
                            slugs,
                            searchQuery,
                        ),
                    ).resources
            HomeData(
                resourceList = taggedResources,
                suggestedResourceList = getSuggestedList(taggedResources, searchQuery, tagsView, showSuggestedModel),
            )
        }
    }

    private fun shouldShowSuggested(
        homeView: HomeDisplayViewModel,
        searchQuery: String?,
    ) = if (searchQuery.isNullOrBlank()) {
        when (homeView) {
            is AllItems -> true
            is Favourites -> true
            is Folders ->
                when (homeView.activeFolder) {
                    is Folder.Child -> false
                    is Folder.Root -> true
                }
            is Groups -> homeView.activeGroupId == null // groups root
            is OwnedByMe -> true
            is RecentlyModified -> true
            is SharedWithMe -> true
            is Tags -> homeView.activeTagId == null // tags root
            is Expiry -> true
            NotLoaded -> false
        }
    } else {
        false
    }
}
