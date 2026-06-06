package net.svaroh.passly.groupdetails.navigation

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.GroupDetailsNavigationKey.GroupMemberDetails
import net.svaroh.passly.core.navigation.compose.keys.GroupDetailsNavigationKey.GroupMembers
import net.svaroh.passly.groupdetails.groupmemberdetails.GroupMemberDetailsScreen
import net.svaroh.passly.groupdetails.groupmembers.GroupMembersScreen

class GroupDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<GroupMembers> { key ->
                PassboltTheme { GroupMembersScreen(groupId = key.groupId) }
            }

            entry<GroupMemberDetails> { key ->
                PassboltTheme { GroupMemberDetailsScreen(userId = key.userId) }
            }
        }
}
