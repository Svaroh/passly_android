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

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.idlingresource.CreateMenuModelIdlingResource
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
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
import net.svaroh.passly.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
import net.svaroh.passly.ui.ResourceMoreMenuModel
import net.svaroh.passly.ui.ResourceMoreMenuModel.DescriptionOption.HAS_METADATA_DESCRIPTION
import net.svaroh.passly.ui.ResourceMoreMenuModel.DescriptionOption.HAS_NOTE
import kotlinx.coroutines.launch
import timber.log.Timber

class ResourceMoreMenuBottomSheetViewModel(
    private val createResourceMoreMenuModelUseCase: CreateResourceMoreMenuModelUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val createMenuModelIdlingResource: CreateMenuModelIdlingResource,
) : SideEffectViewModel<ResourceMoreMenuBottomSheetState, ResourceMoreMenuBottomSheetSideEffect>(
        ResourceMoreMenuBottomSheetState(),
    ) {
    private var menuModel: ResourceMoreMenuModel? = null

    fun onIntent(intent: ResourceMoreMenuBottomSheetIntent) {
        when (intent) {
            is Initialize -> initialize(intent.resourceId)
            Close -> emitSideEffect(Dismiss)
            CopyPassword -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyPassword)
                emitSideEffect(Dismiss)
            }
            CopyMetadataDescription -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyMetadataDescription)
                emitSideEffect(Dismiss)
            }
            CopyNote -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyNote)
                emitSideEffect(Dismiss)
            }
            CopyUrl -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyUrl)
                emitSideEffect(Dismiss)
            }
            CopyUsername -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.CopyUsername)
                emitSideEffect(Dismiss)
            }
            LaunchWebsite -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.LaunchWebsite)
                emitSideEffect(Dismiss)
            }
            Delete -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.Delete)
                emitSideEffect(Dismiss)
            }
            Edit -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.Edit)
                emitSideEffect(Dismiss)
            }
            Share -> {
                emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.Share)
                emitSideEffect(Dismiss)
            }
            ToggleFavourite -> {
                menuModel?.favouriteOption?.let { option ->
                    emitSideEffect(ResourceMoreMenuBottomSheetSideEffect.ToggleFavourite(option))
                    emitSideEffect(Dismiss)
                }
            }
        }
    }

    private fun initialize(resourceId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            createMenuModelIdlingResource.setIdle(false)
            updateViewState { ResourceMoreMenuBottomSheetState(title = title) }
            dataRefreshTrackingFlow.awaitIdle()

            try {
                menuModel =
                    createResourceMoreMenuModelUseCase
                        .execute(CreateResourceMoreMenuModelUseCase.Input(resourceId))
                        .resourceMenuModel

                menuModel?.let { model ->
                    updateViewState {
                        copy(
                            title = model.title,
                            isLoading = false,
                            showCopyPassword = model.canCopy,
                            showCopyNote = model.descriptionOptions.contains(HAS_NOTE),
                            showCopyMetadataDescription = model.descriptionOptions.contains(HAS_METADATA_DESCRIPTION),
                            showSeparator = model.canDelete || model.canEdit || model.canShare,
                            showDelete = model.canDelete,
                            showEdit = model.canEdit,
                            showShare = model.canShare,
                            favouriteOption = model.favouriteOption,
                        )
                    }
                }
            } catch (exception: NullPointerException) {
                Timber.d("Resource item for the shown menu was deleted: $exception")
                emitSideEffect(Dismiss)
            } finally {
                createMenuModelIdlingResource.setIdle(true)
            }
        }
    }
}
