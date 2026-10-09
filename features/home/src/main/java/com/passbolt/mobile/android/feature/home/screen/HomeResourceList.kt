package net.svaroh.passly.feature.home.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import net.svaroh.passly.core.compose.rememberDebouncedBoolean
import net.svaroh.passly.core.localization.R
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.keys.HomeNavigationKey
import net.svaroh.passly.core.ui.empty.EmptyResourceListState
import net.svaroh.passly.core.ui.lists.HeaderItem
import net.svaroh.passly.core.ui.loading.LoadingListState
import net.svaroh.passly.domain.folders.model.FolderWithCountAndPath
import net.svaroh.passly.domain.resources.resourceicon.ResourceIconProvider
import net.svaroh.passly.feature.home.screen.HomeIntent.OpenResourceMenu
import net.svaroh.passly.feature.home.screen.data.HeaderSectionConfiguration
import net.svaroh.passly.feature.home.screen.list.FolderItem
import net.svaroh.passly.feature.home.screen.list.FolderItemPlaceholder
import net.svaroh.passly.feature.home.screen.list.GroupItem
import net.svaroh.passly.feature.home.screen.list.RESOURCE_ITEM_PLACEHOLDER_HEIGHT
import net.svaroh.passly.feature.home.screen.list.ResourceItem
import net.svaroh.passly.feature.home.screen.list.ResourceItemPlaceholder
import net.svaroh.passly.feature.home.screen.list.TagItem
import net.svaroh.passly.ui.Folder.Child
import net.svaroh.passly.ui.GroupWithCount
import net.svaroh.passly.ui.HomeDisplayViewModel.Folders
import net.svaroh.passly.ui.HomeDisplayViewModel.Groups
import net.svaroh.passly.ui.HomeDisplayViewModel.Tags
import net.svaroh.passly.ui.ResourceUiModel
import net.svaroh.passly.ui.TagWithCount
import org.koin.compose.koinInject
import net.svaroh.passly.core.localization.R as LocalizationR

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun HomeResourceList(
    state: HomeState,
    homeListData: HomeListData,
    isListLoading: Boolean,
    navigator: AppNavigator,
    resourceHandlingStrategy: ResourceHandlingStrategy,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    resourceIconProvider: ResourceIconProvider = koinInject(),
) {
    val headerConfig = rememberHeaderConfig(state, homeListData)
    val listState = rememberLazyListState()

    // Scroll to top once when the suggested section first appears (autofill); resources and suggested
    // are emitted together, so without this the list can settle on resources before suggested shows above.
    // The guard stops it re-snapping on later visibility toggles.
    var hasScrolledToShowSuggested by remember { mutableStateOf(false) }
    LaunchedEffect(headerConfig.isSuggestedSectionVisible) {
        if (headerConfig.isSuggestedSectionVisible && !hasScrolledToShowSuggested) {
            listState.scrollToItem(0)
            hasScrolledToShowSuggested = true
        }
    }

    // Auto-scroll to top when search query changes - multiple paginated sources load at once
    // and sections added above the current scroll position (e.g. folders before resources) can push content off-screen
    LaunchedEffect(state.searchQuery) {
        if (state.searchQuery.isNotBlank()) {
            listState.scrollToItem(0)
        }
    }

    val showLoading = rememberDebouncedBoolean(headerConfig.areAllSectionsEmpty && isListLoading)

    val showEmpty =
        rememberDebouncedBoolean(
            headerConfig.areAllSectionsEmpty && !state.isRefreshing && !isListLoading,
        )

    if (showLoading) {
        LoadingListState(
            itemHeight = RESOURCE_ITEM_PLACEHOLDER_HEIGHT,
            itemContent = { ResourceItemPlaceholder() },
        )
    } else if (showEmpty) {
        EmptyResourceListState(title = stringResource(LocalizationR.string.no_passwords))
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            state = listState,
        ) {
            // suggested
            if (headerConfig.isSuggestedSectionVisible) {
                item(key = "header_suggested") { HeaderItem(stringResource(R.string.suggested)) }
                items(
                    count = homeListData.suggestedResources.itemCount,
                    key = homeListData.suggestedResources.itemKey { "suggested_${it.resourceId}" },
                ) { index ->
                    homeListData.suggestedResources[index]?.let { resource ->
                        ResourceItem(
                            resource = resource,
                            resourceIconProvider = resourceIconProvider,
                            onItemClick = { resourceHandlingStrategy.resourceItemClick(resource) },
                            onMoreClick = { onIntent(OpenResourceMenu(resource)) },
                            showMoreMenu = resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                        )
                    }
                }
            }

            // other items header
            if (headerConfig.isOtherItemsSectionVisible) {
                item(key = "header_other") { HeaderItem(stringResource(R.string.other)) }
            }

            // in current folder header
            if (headerConfig.isInCurrentFolderSectionVisible) {
                item(key = "header_in_current_folder") {
                    HeaderItem(
                        stringResource(
                            R.string.home_in_current_folder,
                            headerConfig.currentFolderName
                                ?: stringResource(R.string.folder_root),
                        ),
                    )
                }
            }

            // folders
            items(
                count = homeListData.folders.itemCount,
                key = homeListData.folders.itemKey { "folder_${it.folderId}" },
            ) { index ->
                val folder = homeListData.folders[index]
                if (folder != null) {
                    FolderItem(
                        folder = folder,
                        onFolderClick = {
                            Folders(
                                activeFolder = Child(folder.folderId),
                                activeFolderName = folder.name,
                                isActiveFolderShared = folder.isShared,
                            ).let {
                                navigator.navigateToKey(HomeNavigationKey.Home(it))
                            }
                        },
                    )
                } else {
                    FolderItemPlaceholder()
                }
            }

            // tags
            items(
                count = homeListData.tags.itemCount,
                key = homeListData.tags.itemKey { "tag_${it.id}" },
            ) { tag ->
                homeListData.tags[tag]?.let { tag ->
                    TagItem(
                        tag = tag,
                        onClick = {
                            Tags(
                                activeTagId = tag.id,
                                activeTagName = tag.slug,
                                isActiveTagShared = tag.isShared,
                            ).let {
                                navigator.navigateToKey(HomeNavigationKey.Home(it))
                            }
                        },
                    )
                }
            }

            // groups
            items(
                count = homeListData.groups.itemCount,
                key = homeListData.groups.itemKey { "group_${it.groupId}" },
            ) { group ->
                homeListData.groups[group]?.let { group ->
                    GroupItem(
                        group = group,
                        onClick = {
                            Groups(
                                activeGroupId = group.groupId,
                                activeGroupName = group.groupName,
                            ).let {
                                navigator.navigateToKey(HomeNavigationKey.Home(it))
                            }
                        },
                    )
                }
            }

            // resources
            items(
                count = homeListData.resources.itemCount,
                key = homeListData.resources.itemKey { "resource_${it.resourceId}" },
            ) { index ->
                val resource = homeListData.resources[index]
                if (resource != null) {
                    ResourceItem(
                        resource = resource,
                        resourceIconProvider = resourceIconProvider,
                        onItemClick = { resourceHandlingStrategy.resourceItemClick(resource) },
                        onMoreClick = { onIntent(OpenResourceMenu(resource)) },
                        showMoreMenu = resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                    )
                } else {
                    ResourceItemPlaceholder()
                }
            }

            // in subfolders
            if (headerConfig.isInSubFoldersSectionVisible) {
                item(key = "header_in_subfolders") { HeaderItem(stringResource(R.string.home_in_sub_folders)) }
                items(
                    count = homeListData.filteredSubfolders.itemCount,
                    key = homeListData.filteredSubfolders.itemKey { "subfolder_folder_${it.folderId}" },
                ) { folder ->
                    homeListData.filteredSubfolders[folder]?.let { folder ->
                        FolderItem(
                            folder = folder,
                            onFolderClick = {
                                Folders(
                                    activeFolder = Child(folder.folderId),
                                    activeFolderName = folder.name,
                                    isActiveFolderShared = folder.isShared,
                                ).let {
                                    navigator.navigateToKey(HomeNavigationKey.Home(it))
                                }
                            },
                        )
                    }
                }
                items(
                    count = homeListData.filteredSubfoldersResources.itemCount,
                    key = homeListData.filteredSubfoldersResources.itemKey { "subfolder_resource_${it.resourceId}" },
                ) { resource ->
                    homeListData.filteredSubfoldersResources[resource]?.let { resource ->
                        ResourceItem(
                            resource = resource,
                            resourceIconProvider = resourceIconProvider,
                            onItemClick = { resourceHandlingStrategy.resourceItemClick(resource) },
                            onMoreClick = { onIntent(OpenResourceMenu(resource)) },
                            showMoreMenu = resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                        )
                    }
                }
            }
        }
    }
}

internal data class HomeListData(
    val suggestedResources: LazyPagingItems<ResourceUiModel>,
    val resources: LazyPagingItems<ResourceUiModel>,
    val tags: LazyPagingItems<TagWithCount>,
    val groups: LazyPagingItems<GroupWithCount>,
    val folders: LazyPagingItems<FolderWithCountAndPath>,
    val filteredSubfolders: LazyPagingItems<FolderWithCountAndPath>,
    val filteredSubfoldersResources: LazyPagingItems<ResourceUiModel>,
) {
    val allLists: Array<LazyPagingItems<*>>
        get() =
            arrayOf(
                suggestedResources,
                resources,
                tags,
                groups,
                folders,
                filteredSubfolders,
                filteredSubfoldersResources,
            )
}

@Composable
internal fun rememberIsAnyListRefreshing(homeListData: HomeListData): Boolean {
    val isRefreshing by remember(homeListData) {
        derivedStateOf {
            homeListData.allLists.any { it.loadState.refresh is LoadState.Loading }
        }
    }
    return isRefreshing
}

@Composable
internal fun rememberHomeListData(
    state: HomeState,
    coroutineLaunchContext: CoroutineLaunchContext = koinInject(),
): HomeListData {
    val suggestedResources = state.homeData.suggestedResourceList.collectAsLazyPagingItems(coroutineLaunchContext.default)
    val resources = state.homeData.resourceList.collectAsLazyPagingItems(coroutineLaunchContext.default)
    val tags = state.homeData.tagsList.collectAsLazyPagingItems()
    val groups = state.homeData.groupsList.collectAsLazyPagingItems()
    val folders = state.homeData.foldersList.collectAsLazyPagingItems()
    val filteredSubfolders = state.homeData.filteredSubFolders.collectAsLazyPagingItems()
    val filteredSubfoldersResources = state.homeData.filteredSubFolderResources.collectAsLazyPagingItems()

    return remember(
        suggestedResources,
        resources,
        tags,
        groups,
        folders,
        filteredSubfolders,
        filteredSubfoldersResources,
    ) {
        HomeListData(
            suggestedResources = suggestedResources,
            resources = resources,
            tags = tags,
            groups = groups,
            folders = folders,
            filteredSubfolders = filteredSubfolders,
            filteredSubfoldersResources = filteredSubfoldersResources,
        )
    }
}

@Composable
private fun rememberHeaderConfig(
    state: HomeState,
    homeListData: HomeListData,
): HeaderSectionConfiguration {
    val headerConfig by remember(
        homeListData.resources.itemSnapshotList.isEmpty(),
        homeListData.folders.itemSnapshotList.isEmpty(),
        homeListData.tags.itemSnapshotList.isEmpty(),
        homeListData.groups.itemSnapshotList.isEmpty(),
        homeListData.filteredSubfolders.itemSnapshotList.isEmpty(),
        homeListData.filteredSubfoldersResources.itemSnapshotList.isEmpty(),
        homeListData.suggestedResources.itemSnapshotList.isEmpty(),
        state.searchQuery,
        state.homeView,
        state.showSuggestedModel,
    ) {
        derivedStateOf {
            getHeaderConfig(
                resources = homeListData.resources,
                folders = homeListData.folders,
                tags = homeListData.tags,
                groups = homeListData.groups,
                filteredSubfolders = homeListData.filteredSubfolders,
                filteredSubfoldersResources = homeListData.filteredSubfoldersResources,
                suggestedResources = homeListData.suggestedResources,
                searchQuery = state.searchQuery,
                homeView = state.homeView,
                showSuggestedModel = state.showSuggestedModel,
            )
        }
    }
    return headerConfig
}
