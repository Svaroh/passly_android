/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package net.svaroh.passly.permissions.confirmpermissions

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.fulldatarefresh.service.DataRefreshService
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.GroupPermissionDetails
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.PermissionRecipients
import net.svaroh.passly.core.navigation.compose.keys.PermissionsNavigationKey.UserPermissionDetails
import net.svaroh.passly.core.navigation.compose.results.NavigationResultEventBus
import net.svaroh.passly.core.navigation.compose.results.PermissionsConfirmedResult
import net.svaroh.passly.core.navigation.compose.results.ShareCompleteResult
import net.svaroh.passly.core.ui.banner.WarningBanner
import net.svaroh.passly.core.ui.button.PrimaryButton
import net.svaroh.passly.core.ui.fab.AddFloatingActionButton
import net.svaroh.passly.core.ui.progressdialog.ProgressDialog
import net.svaroh.passly.core.ui.snackbar.ColoredSnackbarVisuals
import net.svaroh.passly.core.ui.topbar.BackNavigationIcon
import net.svaroh.passly.core.ui.topbar.TitleAppBar
import net.svaroh.passly.feature.metadatakeytrust.NewMetadataKeyTrustDialog
import net.svaroh.passly.feature.metadatakeytrust.TrustedMetadataKeyDeletedDialog
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.AddPermission
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.Confirm
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.DismissMetadataKeyDeletedDialog
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.DismissMetadataKeyModifiedDialog
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.GoBack
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.SeePermission
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.TrustNewMetadataKey
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsIntent.TrustedMetadataKeyDeleted
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.CloseWithPermissionsConfirmed
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.CloseWithShareSuccess
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateBack
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToGroupPermissionDetails
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToSelectShareRecipients
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.NavigateToUserPermissionDetails
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowPermissionsDriftedSnackbar
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowSuccessSnackbar
import net.svaroh.passly.permissions.confirmpermissions.ConfirmPermissionsSideEffect.ShowToast
import net.svaroh.passly.permissions.permissions.ui.PermissionsList
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
fun ConfirmPermissionsScreen(
    viewModel: ConfirmPermissionsViewModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
) {
    val context = LocalContext.current
    val resultBus = NavigationResultEventBus.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val errorColor = colorResource(CoreUiR.color.red)
    val successColor = colorResource(CoreUiR.color.green)

    ConfirmPermissionsScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { effect ->
        when (effect) {
            NavigateBack -> navigator.navigateBack()
            is NavigateToUserPermissionDetails ->
                navigator.navigateToKey(
                    UserPermissionDetails(
                        permission = effect.permission,
                        mode = effect.mode,
                        fromSnapshot = true,
                    ),
                )
            is NavigateToGroupPermissionDetails ->
                navigator.navigateToKey(
                    GroupPermissionDetails(
                        permission = effect.permission,
                        mode = effect.mode,
                        fromSnapshot = true,
                    ),
                )
            is NavigateToSelectShareRecipients ->
                navigator.navigateToKey(
                    PermissionRecipients(
                        userPermissions = effect.users,
                        groupPermissions = effect.groups,
                    ),
                )
            is CloseWithPermissionsConfirmed -> {
                resultBus.sendResult(result = PermissionsConfirmedResult(permissions = effect.permissions))
                navigator.navigateBack()
            }
            CloseWithShareSuccess -> {
                DataRefreshService.start(context)
                resultBus.sendResult(result = ShareCompleteResult(shared = true))
                navigator.navigateBack()
            }
            is ShowToast ->
                Toast
                    .makeText(context, getToastMessage(context, effect.type), Toast.LENGTH_SHORT)
                    .show()
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getErrorMessage(context, effect.type),
                            backgroundColor = errorColor,
                        ),
                    )
                }
            is ShowSuccessSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSuccessMessage(context, effect.type),
                            backgroundColor = successColor,
                        ),
                    )
                }
            is ShowPermissionsDriftedSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getPermissionsDriftedMessage(context, effect.driftedEntityNames),
                            backgroundColor = errorColor,
                        ),
                    )
                }
        }
    }
}

@Composable
private fun ConfirmPermissionsScreen(
    state: ConfirmPermissionsState,
    onIntent: (ConfirmPermissionsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.confirm_permissions_title),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                PrimaryButton(
                    text = stringResource(LocalizationR.string.save),
                    onClick = { onIntent(Confirm) },
                    isEnabled = !state.isLoading,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        },
        floatingActionButton = {
            if (state.isEditable) {
                AddFloatingActionButton(onClick = { onIntent(AddPermission) })
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    val customVisuals = data.visuals as? ColoredSnackbarVisuals
                    if (customVisuals != null) {
                        Snackbar(
                            snackbarData = data,
                            containerColor = customVisuals.backgroundColor,
                            contentColor = customVisuals.contentColor,
                        )
                    } else {
                        Snackbar(snackbarData = data)
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            state.indirectAccessWarning?.let { warning ->
                WarningBanner(
                    text = getIndirectAccessWarningMessage(LocalContext.current, warning),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        if (state.isPreparingPermissions) {
                            Text(
                                text = stringResource(LocalizationR.string.confirm_permissions_preparing),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                        if (state.isRefreshingUsersAndGroups) {
                            Text(
                                text = stringResource(LocalizationR.string.confirm_permissions_refreshing_users_and_groups),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            } else {
                PermissionsList(
                    permissions = state.permissions,
                    onPermissionClick = { onIntent(SeePermission(it)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    ProgressDialog(isVisible = state.isApplyingShare)

    if (state.showMetadataKeyModifiedDialog && state.newMetadataKeyToTrustModel != null) {
        NewMetadataKeyTrustDialog(
            newKeyToTrustModel = state.newMetadataKeyToTrustModel,
            onTrustClick = { onIntent(TrustNewMetadataKey) },
            onDismiss = { onIntent(DismissMetadataKeyModifiedDialog) },
        )
    }

    if (state.showMetadataKeyDeletedDialog && state.trustedKeyDeletedModel != null) {
        TrustedMetadataKeyDeletedDialog(
            trustedKeyDeletedModel = state.trustedKeyDeletedModel,
            onDismiss = { onIntent(DismissMetadataKeyDeletedDialog) },
            onTrustClick = { onIntent(TrustedMetadataKeyDeleted) },
        )
    }
}
