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

package com.passbolt.mobile.android.feature.otp.screen

import android.widget.Toast
import androidx.activity.compose.LocalActivity
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.clipboard.ClipboardAccess
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.compose.rememberDebouncedBoolean
import com.passbolt.mobile.android.core.fulldatarefresh.service.DataRefreshService
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.ConfirmPermissions
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.MainResourceForm
import com.passbolt.mobile.android.core.ui.dialogs.ConfirmResourceDeleteAlertDialog
import com.passbolt.mobile.android.core.ui.empty.EmptyResourceListState
import com.passbolt.mobile.android.core.ui.fab.AddFloatingActionButton
import com.passbolt.mobile.android.core.ui.progressdialog.ProgressDialog
import com.passbolt.mobile.android.core.ui.progressindicator.SearchProgressIndicator
import com.passbolt.mobile.android.core.ui.pulltorefresh.SlidingFeedbackPullToRefreshBox
import com.passbolt.mobile.android.core.ui.scaffold.HomeScaffold
import com.passbolt.mobile.android.core.ui.search.SearchInput
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.domain.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.feature.home.screen.ResourceHandlingStrategy
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountBottomSheet
import com.passbolt.mobile.android.feature.metadatakeytrust.NewMetadataKeyTrustDialog
import com.passbolt.mobile.android.feature.metadatakeytrust.TrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseDeleteConfirmationDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseSwitchAccount
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseTrustNewKeyDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseTrustedKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ConfirmDeleteTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CopyOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.DeleteOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.Dispose
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.EditOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OpenOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.RevealOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.Search
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.SearchEndIconAction
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.TrustMetadataKeyDeletion
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.CopyToClipboard
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.InitiateDataRefresh
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToConfirmPermissions
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToEditResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowToast
import com.passbolt.mobile.android.feature.otp.screen.ui.ProgressSource
import com.passbolt.mobile.android.feature.otp.screen.ui.ProgressSource.RevealedOtp
import com.passbolt.mobile.android.feature.otp.screen.ui.ProgressSource.UniversalAutofillCountdown
import com.passbolt.mobile.android.otpmoremenu.OtpMoreMenuBottomSheet
import com.passbolt.mobile.android.testtags.composetags.Otp
import com.passbolt.mobile.android.ui.ConfirmPermissionsMode
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.ResourceFormMode
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun OtpScreen(
    navigator: AppNavigator,
    resourceHandlingStrategy: ResourceHandlingStrategy,
    modifier: Modifier = Modifier,
    viewModel: OtpViewModel = koinViewModel(),
    resourceIconProvider: ResourceIconProvider = koinInject(),
    clipboardAccess: ClipboardAccess = koinInject(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val errorColor = colorResource(CoreUiR.color.red)
    val successColor = colorResource(CoreUiR.color.green)

    OtpScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        resourceIconProvider = resourceIconProvider,
        snackbarHostState = snackbarHostState,
        resourceHandlingStrategy = resourceHandlingStrategy,
        modifier = modifier,
    )

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onIntent(Dispose)
        }
    }

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
                            backgroundColor = errorColor,
                        ),
                    )
                }
            is ShowSuccessSnackbar ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = getSuccessMessage(context, it.type, it.message),
                            backgroundColor = successColor,
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
            is NavigateToConfirmPermissions ->
                navigator.navigateToKey(
                    ConfirmPermissions(ConfirmPermissionsMode.Edit(it.resourceId), it.driftedEntityNames),
                )
            InitiateDataRefresh -> DataRefreshService.start(context)
            is ShowToast -> Toast.makeText(context, getToastMessage(context, it.type), Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod")
@Composable
fun OtpScreen(
    state: OtpState,
    onIntent: (OtpIntent) -> Unit,
    resourceIconProvider: ResourceIconProvider,
    snackbarHostState: SnackbarHostState,
    resourceHandlingStrategy: ResourceHandlingStrategy,
    modifier: Modifier = Modifier,
) {
    val isAutofillMode = resourceHandlingStrategy.appContext == AppContext.AUTOFILL
    val showMoreMenu = resourceHandlingStrategy.shouldShowResourceMoreMenu()
    val showCloseButton = resourceHandlingStrategy.shouldShowCloseButton()
    val activity = LocalActivity.current

    HomeScaffold(
        snackbarHostState = snackbarHostState,
        modifier =
            modifier
                .testTag(Otp.SCREEN),
        appBarTitle = stringResource(LocalizationR.string.main_menu_otp),
        appBarIconRes = CoreUiR.drawable.ic_time_lock,
        shouldShowCloseIcon = showCloseButton,
        onCloseClick = { activity?.finish() },
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
                initialValue = state.searchQuery,
                onEndIconClick = { onIntent(SearchEndIconAction) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
        },
        floatingActionButton = {
            if (!isAutofillMode && !state.isRefreshing) {
                AddFloatingActionButton(onClick = { onIntent(CreateTotp) })
            }
        },
        content =
            { paddingValues ->
                val context = LocalContext.current
                val showSearchProgress = rememberDebouncedBoolean(state.isSearching && !state.isRefreshing)
                SlidingFeedbackPullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    refreshProgress = state.refreshProgress,
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
                            if (state.suggestedOtps.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(LocalizationR.string.suggested),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                                items(state.suggestedOtps, key = { "suggested_${it.resource.resourceId}" }) { otpItem ->
                                    OtpItem(
                                        otpItem = otpItem,
                                        resourceIconProvider = resourceIconProvider,
                                        onItemClick = { resourceHandlingStrategy.resourceItemClick(otpItem.resource) },
                                        onMoreClick = {},
                                        showMoreMenu = false,
                                        showEyeIcon = false,
                                        progressSource =
                                            resolveProgressSource(
                                                otpItem = otpItem,
                                                isAutofillMode = true,
                                                universalCountdownSeconds = state.universalCountdownSeconds,
                                            ),
                                    )
                                }
                                item {
                                    Text(
                                        text = stringResource(LocalizationR.string.other),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                            }

                            items(state.uiOtps) { otpItem ->
                                OtpItem(
                                    otpItem = otpItem,
                                    resourceIconProvider = resourceIconProvider,
                                    onItemClick = {
                                        resourceHandlingStrategy.resourceItemClick(otpItem.resource)
                                    },
                                    onMoreClick = { onIntent(OpenOtpMoreMenu(otpItem)) },
                                    showMoreMenu = showMoreMenu,
                                    showEyeIcon = !isAutofillMode,
                                    progressSource =
                                        resolveProgressSource(
                                            otpItem = otpItem,
                                            isAutofillMode = isAutofillMode,
                                            universalCountdownSeconds = state.universalCountdownSeconds,
                                        ),
                                )
                            }
                        }
                    }
                    if (showSearchProgress) {
                        SearchProgressIndicator(modifier = Modifier.align(Alignment.TopCenter))
                    }
                }
                if (state.showOtpMoreBottomSheet) {
                    val moreMenuResource = requireNotNull(state.moreMenuResource)
                    OtpMoreMenuBottomSheet(
                        resourceId = moreMenuResource.resource.resourceId,
                        resourceName = moreMenuResource.resource.metadataJsonModel.name,
                        onDismissRequest = { onIntent(CloseOtpMoreMenu) },
                        onShowOtp = { onIntent(RevealOtp(moreMenuResource.resource)) },
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

private fun resolveProgressSource(
    otpItem: OtpItemWrapper,
    isAutofillMode: Boolean,
    universalCountdownSeconds: Long,
): ProgressSource? {
    val remainingSeconds = otpItem.remainingSecondsCounter
    val expirySeconds = otpItem.otpExpirySeconds
    return when {
        otpItem.isVisible && remainingSeconds != null && expirySeconds != null ->
            RevealedOtp(remainingSeconds = remainingSeconds, expirySeconds = expirySeconds)
        isAutofillMode ->
            UniversalAutofillCountdown(
                remainingSeconds = universalCountdownSeconds,
                expirySeconds = DEFAULT_TOTP_PERIOD,
            )
        else -> null
    }
}
