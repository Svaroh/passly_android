package net.svaroh.passly.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionsItem
import net.svaroh.passly.ui.PermissionsMode
import kotlinx.serialization.Serializable

sealed interface PermissionsNavigationKey : NavKey {
    @Serializable
    data class Permissions(
        val id: String,
        val mode: PermissionsMode,
        val permissionsItem: PermissionsItem,
    ) : PermissionsNavigationKey

    @Serializable
    data class GroupPermissionDetails(
        val permission: PermissionModelUi.GroupPermissionModel,
        val mode: PermissionsMode,
    ) : PermissionsNavigationKey

    @Serializable
    data class UserPermissionDetails(
        val permission: PermissionModelUi.UserPermissionModel,
        val mode: PermissionsMode,
    ) : PermissionsNavigationKey

    @Serializable
    data class PermissionRecipients(
        val userPermissions: List<PermissionModelUi.UserPermissionModel>,
        val groupPermissions: List<PermissionModelUi.GroupPermissionModel>,
    ) : PermissionsNavigationKey
}
