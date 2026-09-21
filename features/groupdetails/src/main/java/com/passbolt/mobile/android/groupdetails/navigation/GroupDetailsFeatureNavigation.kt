package com.passbolt.mobile.android.groupdetails.navigation

import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.GroupDetailsNavigationKey.GroupMemberDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.GroupDetailsNavigationKey.GroupMembers
import com.passbolt.mobile.android.groupdetails.groupmemberdetails.GroupMemberDetailsScreen
import com.passbolt.mobile.android.groupdetails.groupmembers.GroupMembersScreen

class GroupDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<GroupMembers> { key ->
                PassboltTheme { GroupMembersScreen(groupId = key.groupId, fromSnapshot = key.fromSnapshot) }
            }

            entry<GroupMemberDetails> { key ->
                PassboltTheme { GroupMemberDetailsScreen(userId = key.userId, fromSnapshot = key.fromSnapshot) }
            }
        }
}
