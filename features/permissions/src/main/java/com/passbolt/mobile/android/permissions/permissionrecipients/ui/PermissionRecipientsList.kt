package net.svaroh.passly.permissions.permissionrecipients.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.svaroh.passly.core.ui.permissions.GroupPermissionRow
import net.svaroh.passly.core.ui.permissions.UserPermissionRow
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.ToggleGroupSelection
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.ToggleUserSelection
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsState
import net.svaroh.passly.permissions.permissionrecipients.ui.list.AlreadyAddedHeader
import net.svaroh.passly.permissions.permissionrecipients.ui.list.GroupRecipientRow
import net.svaroh.passly.permissions.permissionrecipients.ui.list.UserRecipientRow
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel

@Composable
internal fun PermissionRecipientsList(
    state: PermissionRecipientsState,
    onIntent: (PermissionRecipientsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(
            items = state.filteredGroups,
            key = { "group_${it.groupId}" },
        ) { group ->
            GroupRecipientRow(
                group = group,
                isSelected = group.groupId in state.selectedGroupIds,
                onClick = { onIntent(ToggleGroupSelection(group)) },
            )
        }

        items(
            items = state.filteredUsers,
            key = { "user_${it.id}" },
        ) { user ->
            UserRecipientRow(
                user = user,
                isSelected = user.id in state.selectedUserIds,
                onClick = { onIntent(ToggleUserSelection(user)) },
            )
        }

        if (state.filteredExistingPermissions.isNotEmpty()) {
            item(key = "already_added_header") {
                AlreadyAddedHeader()
            }

            items(
                items = state.filteredExistingPermissions,
                key = { "already_added_${it.permissionId}" },
            ) { permission ->
                when (permission) {
                    is GroupPermissionModel -> GroupPermissionRow(permission = permission)
                    is UserPermissionModel -> UserPermissionRow(permission = permission)
                }
            }
        }
    }
}
