package com.passbolt.mobile.android.permissions.navigation

import androidx.compose.runtime.Composable
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.ConfirmCreatePermissions
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.GroupPermissionDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.PermissionRecipients
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.UserPermissionDetails
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsScreen
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsScreen
import com.passbolt.mobile.android.permissions.permissions.PermissionsFlow
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.GroupPermissionDeleted
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.GroupPermissionModified
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.ShareRecipientsAdded
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.UserPermissionDeleted
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.UserPermissionModified
import com.passbolt.mobile.android.permissions.permissions.PermissionsScreen
import com.passbolt.mobile.android.permissions.permissions.PermissionsViewModel
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsScreen
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class PermissionsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Permissions> { key ->
                PermissionsScreenEntry(
                    id = key.id,
                    mode = key.mode,
                    permissionsItem = key.permissionsItem,
                    flow = PermissionsFlow.STANDARD,
                )
            }

            entry<ConfirmCreatePermissions> { key ->
                PermissionsScreenEntry(
                    id = key.folderId,
                    mode = PermissionsMode.EDIT,
                    permissionsItem = PermissionsItem.FOLDER,
                    flow = PermissionsFlow.CONFIRM_CREATE,
                )
            }

            entry<GroupPermissionDetails> { key ->
                PassboltTheme {
                    GroupPermissionsScreen(
                        permission = key.permission,
                        mode = key.mode,
                        fromSnapshot = key.fromSnapshot,
                    )
                }
            }

            entry<UserPermissionDetails> { key ->
                PassboltTheme {
                    UserPermissionsScreen(
                        permission = key.permission,
                        mode = key.mode,
                        fromSnapshot = key.fromSnapshot,
                    )
                }
            }

            entry<PermissionRecipients> { key ->
                PassboltTheme {
                    PermissionRecipientsScreen(
                        userPermissions = key.userPermissions,
                        groupPermissions = key.groupPermissions,
                    )
                }
            }
        }

    @Composable
    private fun PermissionsScreenEntry(
        id: String,
        mode: PermissionsMode,
        permissionsItem: PermissionsItem,
        flow: PermissionsFlow,
    ) {
        val resultBus = NavigationResultEventBus.current

        val viewModel: PermissionsViewModel =
            koinViewModel(parameters = { parametersOf(id, mode, permissionsItem, flow) })

        ResultEffect<GroupPermissionModifiedResult>(resultBus) { result ->
            viewModel.onIntent(GroupPermissionModified(result.permission))
        }
        ResultEffect<GroupPermissionDeletedResult>(resultBus) { result ->
            viewModel.onIntent(GroupPermissionDeleted(result.permission))
        }
        ResultEffect<UserPermissionModifiedResult>(resultBus) { result ->
            viewModel.onIntent(UserPermissionModified(result.permission))
        }
        ResultEffect<UserPermissionDeletedResult>(resultBus) { result ->
            viewModel.onIntent(UserPermissionDeleted(result.permission))
        }
        ResultEffect<ShareRecipientsAddedResult>(resultBus) { result ->
            viewModel.onIntent(ShareRecipientsAdded(result.permissions))
        }

        PassboltTheme {
            PermissionsScreen(
                viewModel = viewModel,
            )
        }
    }
}
