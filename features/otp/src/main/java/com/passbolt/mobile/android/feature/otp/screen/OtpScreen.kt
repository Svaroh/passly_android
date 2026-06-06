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

package net.svaroh.passly.feature.otp.screen

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.clipboard.ClipboardAccess
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.fulldatarefresh.service.DataRefreshService
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ScanOtp
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.MainResourceForm
import net.svaroh.passly.core.resources.resourceicon.ResourceIconProvider
import net.svaroh.passly.core.ui.dialogs.ConfirmResourceDeleteAlertDialog
import net.svaroh.passly.core.ui.empty.EmptyResourceListState
import net.svaroh.passly.core.ui.fab.AddFloatingActionButton
import net.svaroh.passly.core.ui.progressdialog.ProgressDialog
import net.svaroh.passly.core.ui.scaffold.HomeScaffold
import net.svaroh.passly.core.ui.search.SearchInput
import net.svaroh.passly.core.ui.snackbar.ColoredSnackbarVisuals
import net.svaroh.passly.createresourcemenu.CreateResourceMenuBottomSheet
import net.svaroh.passly.feature.home.switchaccount.SwitchAccountBottomSheet
import net.svaroh.passly.feature.metadatakeytrust.NewMetadataKeyTrustDialog
import net.svaroh.passly.feature.metadatakeytrust.TrustedMetadataKeyDeletedDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseCreateResourceMenu
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseDeleteConfirmationDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseOtpMoreMenu
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseSwitchAccount
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseTrustNewKeyDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseTrustedKeyDeletedDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.ConfirmDeleteTotp
import net.svaroh.passly.feature.otp.screen.OtpIntent.CopyOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.CreateNote
import net.svaroh.passly.feature.otp.screen.OtpIntent.CreatePassword
import net.svaroh.passly.feature.otp.screen.OtpIntent.CreateTotp
import net.svaroh.passly.feature.otp.screen.OtpIntent.DeleteOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.EditOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.OpenCreateResourceMenu
import net.svaroh.passly.feature.otp.screen.OtpIntent.OpenOtpMoreMenu
import net.svaroh.passly.feature.otp.screen.OtpIntent.RevealOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.Search
import net.svaroh.passly.feature.otp.screen.OtpIntent.SearchEndIconAction
import net.svaroh.passly.feature.otp.screen.OtpIntent.TrustMetadataKeyDeletion
import net.svaroh.passly.feature.otp.screen.OtpIntent.TrustNewMetadataKey
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.CopyToClipboard
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.InitiateDataRefresh
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToCreateResourceForm
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToCreateTotp
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToEditResourceForm
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.ShowSuccessSnackbar
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.ShowToast
import net.svaroh.passly.otpmoremenu.OtpMoreMenuBottomSheet
import net.svaroh.passly.testtags.composetags.Otp
import net.svaroh.passly.ui.ResourceFormMode
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
internal fun OtpScreen(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: OtpViewModel = koinViewModel(),
    resourceIconProvider: ResourceIconProvider = koinInject(),
    clipboardAccess: ClipboardAccess = koinInject(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    OtpScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        resourceIconProvider = resourceIconProvider,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is CopyToClipboard ->
                clipboardAccess.setPrimaryClip(
                    context = context,
                    label = it.label,
                    value = it.value,
                    isSensitive = it.isSensitive,
                )
            is ShowErrorSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getErrorMessage(context, it.type, it.message),
                            backgroundColor = Color(context.getColor(CoreUiR.color.red)),
                        ),
                    )
                }
            is ShowSuccessSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSuccessMessage(context, it.type, it.message),
                            backgroundColor = Color(context.getColor(CoreUiR.color.green)),
                        ),
                    )
                }
            NavigateToCreateTotp ->
                navigator.navigateToKey(
                    ScanOtp(ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN),
                )
            is NavigateToCreateResourceForm ->
                navigator.navigateToKey(MainResourceForm(ResourceFormMode.Create(it.leadingContentType, null)))
            is NavigateToEditResourceForm ->
                navigator.navigateToKey(MainResourceForm(ResourceFormMode.Edit(it.resourceId, it.resourceName)))
            InitiateDataRefresh -> DataRefreshService.start(context)
            is ShowToast -> Toast.makeText(context, getToastMessage(context, it.type), Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    state: OtpState,
    onIntent: (OtpIntent) -> Unit,
    resourceIconProvider: ResourceIconProvider,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    HomeScaffold(
        snackbarHostState = snackbarHostState,
        modifier =
            modifier
                .testTag(Otp.SCREEN),
        appBarTitle = stringResource(LocalizationR.string.main_menu_otp),
        appBarIconRes = CoreUiR.drawable.ic_time_lock,
        appBarSearchInput = {
            SearchInput(
                onValueChange = { onIntent(Search(it)) },
                placeholder = stringResource(LocalizationR.string.otp_search),
                endIconMode = state.searchInputEndIconMode,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                avatarUrl = state.userAvatar,
                onEndIconClick = { onIntent(SearchEndIconAction) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
        },
        floatingActionButton = {
            if (!state.isRefreshing) {
                AddFloatingActionButton(onClick = { onIntent(OpenCreateResourceMenu) })
            }
        },
        content =
            { paddingValues ->
                val context = LocalContext.current
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { DataRefreshService.start(context) },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                ) {
                    if (state.shouldShowEmptyState) {
                        EmptyResourceListState(title = stringResource(LocalizationR.string.no_otps))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                        ) {
                            items(state.uiOtps) { otpItem ->
                                OtpItem(
                                    otpItem = otpItem,
                                    resourceIconProvider = resourceIconProvider,
                                    onItemClick = { onIntent(RevealOtp(otpItem)) },
                                    onMoreClick = { onIntent(OpenOtpMoreMenu(otpItem)) },
                                )
                            }
                        }
                    }
                }
                if (state.showCreateResourceBottomSheet) {
                    CreateResourceMenuBottomSheet(
                        onCreatePassword = { onIntent(CreatePassword) },
                        onCreateTotp = { onIntent(CreateTotp) },
                        onCreateNote = { onIntent(CreateNote) },
                        onDismissRequest = { onIntent(CloseCreateResourceMenu) },
                    )
                }

                if (state.showOtpMoreBottomSheet) {
                    val moreMenuResource = requireNotNull(state.moreMenuResource)
                    OtpMoreMenuBottomSheet(
                        resourceId = moreMenuResource.resource.resourceId,
                        resourceName = moreMenuResource.resource.metadataJsonModel.name,
                        onDismissRequest = { onIntent(CloseOtpMoreMenu) },
                        onShowOtp = { onIntent(RevealOtp(moreMenuResource)) },
                        onCopyOtp = { onIntent(CopyOtp(moreMenuResource)) },
                        onEditOtp = { onIntent(EditOtp(moreMenuResource)) },
                        onDeleteOtp = { onIntent(DeleteOtp(moreMenuResource)) },
                    )
                }

                ConfirmResourceDeleteAlertDialog(
                    isVisible = state.showDeleteTotpConfirmationDialog,
                    onConfirm = { onIntent(ConfirmDeleteTotp) },
                    onDismiss = { onIntent(CloseDeleteConfirmationDialog) },
                )

                if (state.showMetadataTrustedKeyDeletedDialog && state.metadataDeletedKeyModel != null) {
                    TrustedMetadataKeyDeletedDialog(
                        trustedKeyDeletedModel = state.metadataDeletedKeyModel,
                        onDismiss = { onIntent(CloseTrustedKeyDeletedDialog) },
                        onTrustClick = { onIntent(TrustMetadataKeyDeletion) },
                    )
                }

                if (state.showNewMetadataTrustDialog && state.newMetadataKeyTrustModel != null) {
                    NewMetadataKeyTrustDialog(
                        newKeyToTrustModel = state.newMetadataKeyTrustModel,
                        onTrustClick = { onIntent(TrustNewMetadataKey(state.newMetadataKeyTrustModel)) },
                        onDismiss = { onIntent(CloseTrustNewKeyDialog) },
                    )
                }

                if (state.showAccountSwitchBottomSheet) {
                    SwitchAccountBottomSheet(
                        onDismissRequest = { onIntent(CloseSwitchAccount) },
                        appContext = AppContext.APP,
                    )
                }

                ProgressDialog(state.showProgress)
            },
    )
}
