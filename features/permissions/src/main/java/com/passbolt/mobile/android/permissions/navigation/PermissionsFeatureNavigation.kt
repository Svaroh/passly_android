package net.svaroh.passly.permissions.navigation

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.GroupPermissionDetails
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.PermissionRecipients
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.UserPermissionDetails
import net.svaroh.passly.core.navigation.compose.results.NavigationResultEventBus
import net.svaroh.passly.core.navigation.compose.results.ResultEffect
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsScreen
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsScreen
import net.svaroh.passly.permissions.permissions.PermissionsIntent.GroupPermissionDeleted
import net.svaroh.passly.permissions.permissions.PermissionsIntent.GroupPermissionModified
import net.svaroh.passly.permissions.permissions.PermissionsIntent.ShareRecipientsAdded
import net.svaroh.passly.permissions.permissions.PermissionsIntent.UserPermissionDeleted
import net.svaroh.passly.permissions.permissions.PermissionsIntent.UserPermissionModified
import net.svaroh.passly.permissions.permissions.PermissionsScreen
import net.svaroh.passly.permissions.permissions.PermissionsViewModel
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class PermissionsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Permissions> { key ->
                val resultBus = NavigationResultEventBus.current

                val viewModel: PermissionsViewModel =
                    koinViewModel(parameters = { parametersOf(key.id, key.mode, key.permissionsItem) })

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

            entry<GroupPermissionDetails> { key ->
                PassboltTheme {
                    GroupPermissionsScreen(
                        permission = key.permission,
                        mode = key.mode,
                    )
                }
            }

            entry<UserPermissionDetails> { key ->
                PassboltTheme {
                    UserPermissionsScreen(
                        permission = key.permission,
                        mode = key.mode,
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
}
