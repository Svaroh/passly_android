package com.passbolt.mobile.android.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface GroupDetailsNavigationKey : NavKey {
    @Serializable
    data class GroupMembers(
        val groupId: String,
        val fromSnapshot: Boolean = false,
    ) : GroupDetailsNavigationKey

    @Serializable
    data class GroupMemberDetails(
        val userId: String,
        val fromSnapshot: Boolean = false,
    ) : GroupDetailsNavigationKey
}
