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

package net.svaroh.passly.resourcemoremenu

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.ui.bottomsheet.BottomSheetHeader
import net.svaroh.passly.core.ui.menu.OpenableSettingsItem
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Close
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyMetadataDescription
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyNote
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyPassword
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyUrl
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.CopyUsername
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Delete
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Edit
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Initialize
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.LaunchWebsite
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.Share
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetIntent.ToggleFavourite
import net.svaroh.passly.resourcemoremenu.ResourceMoreMenuBottomSheetSideEffect.Dismiss
import net.svaroh.passly.ui.ResourceMoreMenuModel.FavouriteOption
import net.svaroh.passly.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import net.svaroh.passly.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
import org.koin.androidx.compose.koinViewModel
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
fun ResourceMoreMenuBottomSheet(
    resourceId: String,
    resourceName: String,
    onDismissRequest: () -> Unit,
    onCopyPassword: () -> Unit,
    onCopyMetadataDescription: () -> Unit,
    onCopyNote: () -> Unit,
    onCopyUrl: () -> Unit,
    onCopyUsername: () -> Unit,
    onLaunchWebsite: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onToggleFavourite: (FavouriteOption) -> Unit,
    viewModel: ResourceMoreMenuBottomSheetViewModel = koinViewModel(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(resourceId) {
        viewModel.onIntent(Initialize(resourceId))
    }

    ResourceMoreMenuBottomSheet(
        state = state,
        resourceName = resourceName,
        onIntent = viewModel::onIntent,
        onDismissRequest = onDismissRequest,
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            Dismiss -> onDismissRequest()
            ResourceMoreMenuBottomSheetSideEffect.CopyPassword -> onCopyPassword()
            ResourceMoreMenuBottomSheetSideEffect.CopyMetadataDescription -> onCopyMetadataDescription()
            ResourceMoreMenuBottomSheetSideEffect.CopyNote -> onCopyNote()
            ResourceMoreMenuBottomSheetSideEffect.CopyUrl -> onCopyUrl()
            ResourceMoreMenuBottomSheetSideEffect.CopyUsername -> onCopyUsername()
            ResourceMoreMenuBottomSheetSideEffect.LaunchWebsite -> onLaunchWebsite()
            ResourceMoreMenuBottomSheetSideEffect.Delete -> onDelete()
            ResourceMoreMenuBottomSheetSideEffect.Edit -> onEdit()
            ResourceMoreMenuBottomSheetSideEffect.Share -> onShare()
            is ResourceMoreMenuBottomSheetSideEffect.ToggleFavourite -> onToggleFavourite(sideEffect.option)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceMoreMenuBottomSheet(
    state: ResourceMoreMenuBottomSheetState,
    resourceName: String,
    onIntent: (ResourceMoreMenuBottomSheetIntent) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    if (!state.isLoading) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            containerColor = colorResource(CoreUiR.color.elevated_background),
            sheetState = sheetState,
            modifier = modifier.statusBarsPadding(),
        ) {
            BottomSheetHeader(
                title = state.title.ifEmpty { resourceName },
                onClose = { onIntent(Close) },
            )

            LazyColumn(modifier = Modifier.weight(weight = 1f, fill = false)) {
                item {
                    OpenableSettingsItem(
                        title = stringResource(LocalizationR.string.more_launch_website),
                        iconPainter = painterResource(CoreUiR.drawable.ic_open_link),
                        onClick = { onIntent(LaunchWebsite) },
                        opensInternally = false,
                    )
                }

                item {
                    OpenableSettingsItem(
                        title = stringResource(LocalizationR.string.more_copy_uri),
                        iconPainter = painterResource(CoreUiR.drawable.ic_link),
                        onClick = { onIntent(CopyUrl) },
                        opensInternally = false,
                    )
                }

                if (state.showCopyPassword) {
                    item {
                        OpenableSettingsItem(
                            title = stringResource(LocalizationR.string.more_copy_password),
                            iconPainter = painterResource(CoreUiR.drawable.ic_key),
                            onClick = { onIntent(CopyPassword) },
                            opensInternally = false,
                        )
                    }
                }

                if (state.showCopyMetadataDescription) {
                    item {
                        OpenableSettingsItem(
                            title = stringResource(LocalizationR.string.more_copy_metadata_desc),
                            iconPainter = painterResource(CoreUiR.drawable.ic_description),
                            onClick = { onIntent(CopyMetadataDescription) },
                            opensInternally = false,
                        )
                    }
                }

                if (state.showCopyNote) {
                    item {
                        OpenableSettingsItem(
                            title = stringResource(LocalizationR.string.more_copy_note),
                            iconPainter = painterResource(CoreUiR.drawable.ic_notes),
                            onClick = { onIntent(CopyNote) },
                            opensInternally = false,
                        )
                    }
                }

                item {
                    OpenableSettingsItem(
                        title = stringResource(LocalizationR.string.more_copy_username),
                        iconPainter = painterResource(CoreUiR.drawable.ic_user),
                        onClick = { onIntent(CopyUsername) },
                        opensInternally = false,
                    )
                }

                state.favouriteOption?.let { option ->
                    item {
                        val titleRes =
                            when (option) {
                                ADD_TO_FAVOURITES -> LocalizationR.string.more_add_to_favourite
                                REMOVE_FROM_FAVOURITES -> LocalizationR.string.more_remove_from_favourite
                            }
                        val iconRes =
                            when (option) {
                                ADD_TO_FAVOURITES -> CoreUiR.drawable.ic_add_to_favourite
                                REMOVE_FROM_FAVOURITES -> CoreUiR.drawable.ic_remove_favourite
                            }
                        OpenableSettingsItem(
                            title = stringResource(titleRes),
                            iconPainter = painterResource(iconRes),
                            onClick = { onIntent(ToggleFavourite) },
                            opensInternally = false,
                        )
                    }
                }

                if (state.showSeparator) {
                    item {
                        HorizontalDivider(
                            color = colorResource(CoreUiR.color.divider),
                            thickness = 1.dp,
                        )
                    }
                }

                if (state.showShare) {
                    item {
                        OpenableSettingsItem(
                            title = stringResource(LocalizationR.string.more_share),
                            iconPainter = painterResource(CoreUiR.drawable.ic_share),
                            onClick = { onIntent(Share) },
                            opensInternally = false,
                        )
                    }
                }

                if (state.showEdit) {
                    item {
                        OpenableSettingsItem(
                            title = stringResource(LocalizationR.string.more_edit),
                            iconPainter = painterResource(CoreUiR.drawable.ic_edit),
                            onClick = { onIntent(Edit) },
                            opensInternally = false,
                        )
                    }
                }

                if (state.showDelete) {
                    item {
                        OpenableSettingsItem(
                            title = stringResource(LocalizationR.string.more_delete),
                            iconPainter = painterResource(CoreUiR.drawable.ic_trash),
                            onClick = { onIntent(Delete) },
                            opensInternally = false,
                            iconTint = colorResource(CoreUiR.color.red),
                        )
                    }
                }
            }
        }
    }
}
