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

package net.svaroh.passly.locationdetails

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.locationdetails.LocationDetailsIntent.GoBack
import net.svaroh.passly.locationdetails.LocationDetailsIntent.ToggleExpanded
import net.svaroh.passly.locationdetails.LocationDetailsSideEffect.NavigateToHome
import net.svaroh.passly.locationdetails.LocationDetailsSideEffect.NavigateUp
import net.svaroh.passly.locationdetails.LocationDetailsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.locationdetails.LocationDetailsSideEffect.ShowToast
import net.svaroh.passly.locationdetails.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import net.svaroh.passly.locationdetails.ToastType.CONTENT_NOT_AVAILABLE
import net.svaroh.passly.locationdetails.data.ExpandableFolderTreeCreator
import net.svaroh.passly.locationdetails.data.createExpandedIds
import net.svaroh.passly.locationdetails.ui.LocationItem
import net.svaroh.passly.locationdetails.ui.LocationItem.FOLDER
import net.svaroh.passly.locationdetails.ui.LocationItem.RESOURCE
import kotlinx.coroutines.launch
import timber.log.Timber

internal class LocationDetailsViewModel(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val locationItem: LocationItem,
    private val itemId: String,
    private val getLocalFolderDetailsUseCase: GetLocalFolderDetailsUseCase,
    private val getLocalFolderLocationUseCase: GetLocalFolderLocationUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val expandableFolderTreeCreator: ExpandableFolderTreeCreator,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
) : SideEffectViewModel<LocationDetailsState, LocationDetailsSideEffect>(LocationDetailsState()) {
    init {
        viewModelScope.launch(coroutineLaunchContext.io) {
            loadItem(locationItem, itemId)
        }
        viewModelScope.launch(coroutineLaunchContext.io) {
            synchronizeWithDataRefresh(locationItem, itemId)
        }
    }

    fun onIntent(intent: LocationDetailsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            is ToggleExpanded -> {
                if (viewState.value.expandedItemIds.contains(intent.itemId)) {
                    collapseFolderItem(intent.itemId)
                } else {
                    expandFolderItem(intent.itemId)
                }
            }
        }
    }

    private suspend fun loadItem(
        locationItem: LocationItem,
        itemId: String,
    ) {
        when (locationItem) {
            RESOURCE -> loadResourceLocation(itemId)
            FOLDER -> loadFolderLocation(itemId)
        }
    }

    private suspend fun synchronizeWithDataRefresh(
        locationItem: LocationItem,
        resourceId: String,
    ) {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect {
            when (it) {
                InProgress -> updateViewState { copy(isRefreshing = true) }
                FinishedWithFailure -> {
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                    updateViewState { copy(isRefreshing = false) }
                }
                FinishedWithSuccess -> {
                    updateViewState {
                        copy(isRefreshing = false)
                    }
                    loadItem(locationItem, resourceId)
                }
                NotCompleted -> {
                    // do nothing
                }
            }
        }
    }

    private suspend fun loadResourceLocation(resourceId: String) {
        try {
            val resource =
                getLocalResourceUseCase
                    .execute(GetLocalResourceUseCase.Input(resourceId))
                    .resource

            val parentFolders =
                resource.folderId?.let {
                    getLocalFolderLocationUseCase
                        .execute(GetLocalFolderLocationUseCase.Input(it))
                        .parentFolders
                } ?: emptyList()

            val folderTree = expandableFolderTreeCreator.create(parentFolders)
            val expandedIds = createExpandedIds(folderTree)

            updateViewState {
                copy(
                    itemName = resource.metadataJsonModel.name,
                    isSharedFolder = false,
                    resource = resource,
                    parentFolders = parentFolders,
                    folderTree = folderTree,
                    expandedItemIds = expandedIds,
                )
            }
        } catch (_: NullPointerException) {
            emitSideEffect(ShowToast(CONTENT_NOT_AVAILABLE))
            emitSideEffect(NavigateToHome)
        } catch (throwable: Exception) {
            Timber.e(throwable)
        }
    }

    private suspend fun loadFolderLocation(folderId: String) {
        try {
            val folder =
                getLocalFolderDetailsUseCase
                    .execute(GetLocalFolderDetailsUseCase.Input(folderId))
                    .folder

            val parentFolders =
                getLocalFolderLocationUseCase
                    .execute(GetLocalFolderLocationUseCase.Input(folderId))
                    .parentFolders

            val folderTree = expandableFolderTreeCreator.create(parentFolders)
            val expandedIds = createExpandedIds(folderTree)

            updateViewState {
                copy(
                    itemName = folder.name,
                    isSharedFolder = folder.isShared,
                    resource = null,
                    parentFolders = parentFolders,
                    folderTree = folderTree,
                    expandedItemIds = expandedIds,
                )
            }
        } catch (_: NullPointerException) {
            emitSideEffect(ShowToast(CONTENT_NOT_AVAILABLE))
            emitSideEffect(NavigateToHome)
        } catch (throwable: Exception) {
            Timber.e(throwable)
        }
    }

    private fun expandFolderItem(itemId: String) {
        updateViewState {
            copy(expandedItemIds = expandedItemIds + itemId)
        }
    }

    private fun collapseFolderItem(itemId: String) {
        updateViewState {
            copy(expandedItemIds = expandedItemIds - itemId)
        }
    }
}
