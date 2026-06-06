package net.svaroh.passly.mappers

import net.svaroh.passly.entity.home.HomeDisplayView
import net.svaroh.passly.entity.resource.Permission
import net.svaroh.passly.entity.resource.ResourceDatabaseView
import net.svaroh.passly.ui.DefaultFilterModel
import net.svaroh.passly.ui.HomeDisplayViewModel

/**
 * Mapper responsible for mapping between UI related resource display view type and database related
 * ordering and filtering types.
 */
class HomeDisplayViewMapper {
    /**
     * @param homeView UI related resources display view type
     * @return Database related type for order or filter
     */
    fun map(homeView: HomeDisplayViewModel) =
        when (homeView) {
            is HomeDisplayViewModel.AllItems, HomeDisplayViewModel.NotLoaded -> ResourceDatabaseView.ByNameAscending
            is HomeDisplayViewModel.RecentlyModified -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayViewModel.Favourites -> ResourceDatabaseView.IsFavourite
            is HomeDisplayViewModel.OwnedByMe -> ResourceDatabaseView.HasPermissions(setOf(Permission.OWNER))
            is HomeDisplayViewModel.SharedWithMe ->
                ResourceDatabaseView.HasPermissions(
                    setOf(Permission.READ, Permission.WRITE),
                )
            is HomeDisplayViewModel.Folders -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayViewModel.Tags -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayViewModel.Groups -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayViewModel.Expiry -> ResourceDatabaseView.HasExpiry
        }

    fun map(homeView: HomeDisplayView): HomeDisplayViewModel =
        when (homeView) {
            HomeDisplayView.ALL_ITEMS -> HomeDisplayViewModel.AllItems
            HomeDisplayView.FAVOURITES -> HomeDisplayViewModel.Favourites
            HomeDisplayView.RECENTLY_MODIFIED -> HomeDisplayViewModel.RecentlyModified
            HomeDisplayView.SHARED_WITH_ME -> HomeDisplayViewModel.SharedWithMe
            HomeDisplayView.OWNED_BY_ME -> HomeDisplayViewModel.OwnedByMe
            HomeDisplayView.FOLDERS -> HomeDisplayViewModel.folderRoot()
            HomeDisplayView.TAGS -> HomeDisplayViewModel.tagsRoot()
            HomeDisplayView.GROUPS -> HomeDisplayViewModel.groupsRoot()
            HomeDisplayView.EXPIRY -> HomeDisplayViewModel.Expiry
        }

    fun map(
        userSetHomeView: DefaultFilterModel,
        lastUsedHomeView: HomeDisplayView,
    ): HomeDisplayViewModel =
        when (userSetHomeView) {
            DefaultFilterModel.LAST_USED -> map(lastUsedHomeView)
            DefaultFilterModel.ALL_ITEMS -> HomeDisplayViewModel.AllItems
            DefaultFilterModel.FAVOURITES -> HomeDisplayViewModel.Favourites
            DefaultFilterModel.RECENTLY_MODIFIED -> HomeDisplayViewModel.RecentlyModified
            DefaultFilterModel.SHARED_WITH_ME -> HomeDisplayViewModel.SharedWithMe
            DefaultFilterModel.OWNED_BY_ME -> HomeDisplayViewModel.OwnedByMe
            DefaultFilterModel.FOLDERS -> HomeDisplayViewModel.folderRoot()
            DefaultFilterModel.TAGS -> HomeDisplayViewModel.tagsRoot()
            DefaultFilterModel.GROUPS -> HomeDisplayViewModel.groupsRoot()
            DefaultFilterModel.EXPIRY -> HomeDisplayViewModel.Expiry
        }
}
