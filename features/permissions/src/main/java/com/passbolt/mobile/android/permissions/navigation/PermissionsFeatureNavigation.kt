package net.svaroh.passly.permissions.navigation

import androidx.compose.runtime.Composable
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.ConfirmPermissions
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.GroupPermissionDetails
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.PermissionRecipients
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.UserPermissionDetails
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsScreen
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsViewModel
import net.svaroh.passly.permissions.grouppermissionsdetails.GroupPermissionsScreen
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsScreen
import net.svaroh.passly.permissions.permissions.PermissionsScreen
import net.svaroh.passly.permissions.permissions.PermissionsViewModel
import net.svaroh.passly.permissions.userpermissionsdetails.UserPermissionsScreen
import net.svaroh.passly.ui.ConfirmPermissionsMode
import net.svaroh.passly.ui.PermissionsItem
import net.svaroh.passly.ui.PermissionsMode
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent as ConfirmPermissionsIntent

class PermissionsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Permissions> { key ->
                PermissionsScreenEntry(
                    id = key.id,
                    mode = key.mode,
                    permissionsItem = key.permissionsItem,
                )
            }

            entry<ConfirmPermissions> { key ->
                ConfirmPermissionsScreenEntry(
                    confirmMode = key.confirmMode,
                    driftedEntityNames = key.driftedEntityNames,
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
    ) {
        val viewModel: PermissionsViewModel =
            koinViewModel(parameters = { parametersOf(id, mode, permissionsItem) })

        PassboltTheme {
            PermissionsScreen(
                viewModel = viewModel,
            )
        }
    }

    @Composable
    private fun ConfirmPermissionsScreenEntry(
        confirmMode: ConfirmPermissionsMode,
        driftedEntityNames: List<String>?,
    ) {
        val viewModel: ConfirmPermissionsViewModel =
            koinViewModel(parameters = { parametersOf(confirmMode, driftedEntityNames) })

        PermissionListEditResultEffects(
            onModifyUserPermission = { viewModel.onIntent(ConfirmPermissionsIntent.UserPermissionModified(it)) },
            onDeleteUserPermission = { viewModel.onIntent(ConfirmPermissionsIntent.UserPermissionDeleted(it)) },
            onModifyGroupPermission = { viewModel.onIntent(ConfirmPermissionsIntent.GroupPermissionModified(it)) },
            onDeleteGroupPermission = { viewModel.onIntent(ConfirmPermissionsIntent.GroupPermissionDeleted(it)) },
            onAddShareRecipients = { viewModel.onIntent(ConfirmPermissionsIntent.ShareRecipientsAdded(it)) },
        )

        PassboltTheme {
            ConfirmPermissionsScreen(
                viewModel = viewModel,
            )
        }
    }
}
