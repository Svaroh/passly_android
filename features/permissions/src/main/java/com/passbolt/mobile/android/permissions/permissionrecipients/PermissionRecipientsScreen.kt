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

package net.svaroh.passly.permissions.permissionrecipients

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.results.NavigationResultEventBus
import net.svaroh.passly.core.ui.button.PrimaryButton
import net.svaroh.passly.core.ui.scaffold.HomeScaffold
import net.svaroh.passly.core.ui.search.SearchInput
import net.svaroh.passly.core.ui.sharedwith.SharedWithSection
import net.svaroh.passly.permissions.navigation.ShareRecipientsAddedResult
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.GoBack
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.Save
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsIntent.Search
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsSideEffect.NavigateBack
import net.svaroh.passly.permissions.permissionrecipients.PermissionRecipientsSideEffect.NavigateBackWithResult
import net.svaroh.passly.permissions.permissionrecipients.ui.EmptyState
import net.svaroh.passly.permissions.permissionrecipients.ui.PermissionRecipientsList
import net.svaroh.passly.ui.PermissionModelUi
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
fun PermissionRecipientsScreen(
    userPermissions: List<PermissionModelUi.UserPermissionModel>,
    groupPermissions: List<PermissionModelUi.GroupPermissionModel>,
    modifier: Modifier = Modifier,
    viewModel: PermissionRecipientsViewModel =
        koinViewModel(
            parameters = { parametersOf(groupPermissions.toTypedArray(), userPermissions.toTypedArray()) },
        ),
    navigator: AppNavigator = koinInject(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resultBus = NavigationResultEventBus.current

    PermissionRecipientsScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { effect ->
        when (effect) {
            NavigateBack -> navigator.navigateBack()
            is NavigateBackWithResult -> {
                resultBus.sendResult(result = ShareRecipientsAddedResult(effect.permissions))
                navigator.navigateBack()
            }
        }
    }
}

@Composable
private fun PermissionRecipientsScreen(
    state: PermissionRecipientsState,
    onIntent: (PermissionRecipientsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    HomeScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        appBarTitle = stringResource(LocalizationR.string.permission_recipients_select_users),
        shouldShowBackIcon = true,
        onBackClick = { onIntent(GoBack) },
        appBarSearchInput = {
            Column {
                SearchInput(
                    onValueChange = { onIntent(Search(it)) },
                    placeholder = stringResource(LocalizationR.string.user_search_hint),
                    endIconMode = state.searchInputEndIconMode,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_filter),
                            contentDescription = null,
                        )
                    },
                )
                if (state.currentPermissions.isNotEmpty()) {
                    SharedWithSection(
                        permissions = state.currentPermissions,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.background,
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                PrimaryButton(
                    text = stringResource(LocalizationR.string.save),
                    onClick = { onIntent(Save) },
                )
            }
        },
        content = { paddingValues ->
            if (state.showEmptyState) {
                EmptyState(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                )
            } else {
                PermissionRecipientsList(
                    state = state,
                    onIntent = onIntent,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                )
            }
        },
    )
}
