package com.passbolt.mobile.android.permissions.navigation

import androidx.compose.runtime.Composable
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.ConfirmPermissions
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.GroupPermissionDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.PermissionRecipients
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.UserPermissionDetails
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsScreen
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsViewModel
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsScreen
import com.passbolt.mobile.android.permissions.permissionrecipients.PermissionRecipientsScreen
import com.passbolt.mobile.android.permissions.permissions.PermissionsScreen
import com.passbolt.mobile.android.permissions.permissions.PermissionsViewModel
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsScreen
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.permissions.confirmpermissions.ConfirmPermissionsIntent as ConfirmPermissionsIntent

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
