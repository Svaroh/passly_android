package net.svaroh.passly.feature.home.screen

import android.content.Context
import androidx.annotation.DrawableRes
import net.svaroh.passly.common.extension.toSingleLine
import net.svaroh.passly.ui.Folder.Child
import net.svaroh.passly.ui.Folder.Root
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.HomeDisplayViewModel.AllItems
import net.svaroh.passly.ui.HomeDisplayViewModel.Expiry
import net.svaroh.passly.ui.HomeDisplayViewModel.Favourites
import net.svaroh.passly.ui.HomeDisplayViewModel.Folders
import net.svaroh.passly.ui.HomeDisplayViewModel.Groups
import net.svaroh.passly.ui.HomeDisplayViewModel.OwnedByMe
import net.svaroh.passly.ui.HomeDisplayViewModel.RecentlyModified
import net.svaroh.passly.ui.HomeDisplayViewModel.SharedWithMe
import net.svaroh.passly.ui.HomeDisplayViewModel.Tags
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Suppress("CyclomaticComplexMethod")
internal fun getAppBarTitle(
    context: Context,
    state: HomeState,
): String =
    when (state.homeView) {
        is Folders ->
            when (state.homeView.activeFolder) {
                is Child ->
                    state.homeView.activeFolderName
                        .orEmpty()
                        .toSingleLine()
                is Root -> context.getString(LocalizationR.string.filters_menu_folders)
            }
        is Groups ->
            if (state.homeView.activeGroupId == null) {
                context.getString(LocalizationR.string.filters_menu_groups)
            } else {
                state.homeView.activeGroupName
                    .orEmpty()
                    .toSingleLine()
            }
        is Tags ->
            if (state.homeView.activeTagId == null) {
                context.getString(LocalizationR.string.filters_menu_tags)
            } else {
                state.homeView.activeTagName
                    .orEmpty()
                    .toSingleLine()
            }
        AllItems -> context.getString(LocalizationR.string.filters_menu_all_items)
        Expiry -> context.getString(LocalizationR.string.filters_menu_expiry)
        Favourites -> context.getString(LocalizationR.string.filters_menu_favourites)
        OwnedByMe -> context.getString(LocalizationR.string.filters_menu_owned_by_me)
        RecentlyModified -> context.getString(LocalizationR.string.filters_menu_recently_modified)
        SharedWithMe -> context.getString(LocalizationR.string.filters_menu_shared_with_me)
        HomeDisplayViewModel.NotLoaded -> context.getString(LocalizationR.string.filters_menu_loading)
    }

@DrawableRes
internal fun getAppBarIconResId(state: HomeState): Int =
    when (state.homeView) {
        AllItems -> CoreUiR.drawable.ic_list
        Expiry -> CoreUiR.drawable.ic_calendar_clock
        Favourites -> CoreUiR.drawable.ic_star
        is Folders -> if (state.homeView.isActiveFolderShared == true) CoreUiR.drawable.ic_shared_folder else CoreUiR.drawable.ic_folder
        is Groups -> CoreUiR.drawable.ic_group
        OwnedByMe -> CoreUiR.drawable.ic_person
        RecentlyModified -> CoreUiR.drawable.ic_clock
        SharedWithMe -> CoreUiR.drawable.ic_share
        is Tags -> if (state.homeView.isActiveTagShared == true) CoreUiR.drawable.ic_shared_tag else CoreUiR.drawable.ic_tag
        HomeDisplayViewModel.NotLoaded -> CoreUiR.drawable.ic_password_generate
    }
