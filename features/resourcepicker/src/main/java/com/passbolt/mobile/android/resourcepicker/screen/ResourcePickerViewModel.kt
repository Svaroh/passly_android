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

package net.svaroh.passly.resourcepicker.screen

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.CLEAR
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.NONE
import net.svaroh.passly.resourcepicker.model.ConfirmationType
import net.svaroh.passly.resourcepicker.model.PickResourceAction
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.ApplyClick
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.CloseConfirmationDialog
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.ConfirmOtpLink
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.GoBack
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.ResourcePicked
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.Search
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.SearchEndIconAction
import net.svaroh.passly.resourcepicker.screen.ResourcePickerSideEffect.NavigateBackWithResult
import net.svaroh.passly.resourcepicker.screen.ResourcePickerSideEffect.NavigateUp
import net.svaroh.passly.resourcepicker.screen.ResourcePickerSideEffect.ShowErrorSnackbar
import net.svaroh.passly.resourcepicker.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import net.svaroh.passly.resourcepicker.screen.SnackbarErrorType.NO_PERMISSION
import net.svaroh.passly.resourcepicker.screen.SnackbarErrorType.UNSUPPORTED_RESOURCE_TYPE
import net.svaroh.passly.resourcepicker.screen.data.ResourcePickerDataProvider
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.ui.ResourcePickerListItem
import net.svaroh.passly.ui.ResourcePickerListItem.Selection.NOT_SELECTABLE_NO_PERMISSION
import net.svaroh.passly.ui.ResourcePickerListItem.Selection.NOT_SELECTABLE_UNSUPPORTED_RESOURCE_TYPE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

internal class ResourcePickerViewModel(
    private val suggestionUri: String?,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val resourcePickerDataProvider: ResourcePickerDataProvider,
) : SideEffectViewModel<ResourcePickerState, ResourcePickerSideEffect>(ResourcePickerState()) {
    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadResources()
        synchronizeWithDataRefresh()
        observeSearchQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            searchQueryFlow
                .drop(1)
                .debounce(SEARCH_DEBOUNCE)
                .collectLatest { searchQuery ->
                    Timber.d("Applying search query (length: ${searchQuery.length})")
                    try {
                        loadResourcesData(searchQuery)
                        updateViewState { copy(isSearching = false) }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to apply the search query")
                        updateViewState { copy(isSearching = false) }
                    }
                }
        }
    }

    fun onIntent(intent: ResourcePickerIntent) {
        when (intent) {
            is Search -> searchQueryChanged(intent.searchQuery)
            is ResourcePicked -> resourcePicked(intent.resource)
            is ConfirmOtpLink -> confirmOtpLink(intent.pickAction)
            SearchEndIconAction -> searchEndIconAction()
            ApplyClick -> applyClick()
            CloseConfirmationDialog -> updateViewState { copy(showConfirmationDialog = false) }
            GoBack -> emitSideEffect(NavigateUp)
        }
    }

    private fun searchQueryChanged(query: String) {
        if (query == searchQueryFlow.value) {
            return
        }
        searchQueryFlow.value = query
        updateViewState {
            copy(
                searchQuery = query,
                searchInputEndIconMode = if (query.isBlank()) NONE else CLEAR,
                isSearching = true,
            )
        }
    }

    private fun searchEndIconAction() {
        when (viewState.value.searchInputEndIconMode) {
            CLEAR -> searchQueryChanged("")
            else -> {
                // no-op
            }
        }
    }

    private fun resourcePicked(resource: ResourcePickerListItem) {
        if (resource.isSelectable) {
            updateViewState {
                copy(
                    pickedResource = resource,
                    isApplyButtonEnabled = true,
                )
            }
        } else {
            when (resource.selection) {
                NOT_SELECTABLE_NO_PERMISSION -> emitSideEffect(ShowErrorSnackbar(NO_PERMISSION))
                NOT_SELECTABLE_UNSUPPORTED_RESOURCE_TYPE -> emitSideEffect(ShowErrorSnackbar(UNSUPPORTED_RESOURCE_TYPE))
                else -> { // no-op
                }
            }
        }
    }

    private fun applyClick() {
        val pickedResource = viewState.value.pickedResource ?: return

        viewModelScope.launch(coroutineLaunchContext.io) {
            val slug = pickedResource.resourceModel.slug

            require(slug in SELECTABLE_RESOURCE_TYPES_SLUGS)

            val (pickAction, confirmationType) =
                when (slug) {
                    ContentType.PasswordAndDescription.slug, ContentType.V5Default.slug ->
                        PickResourceAction.TOTP_LINK to ConfirmationType.LINK_TOTP
                    ContentType.PasswordDescriptionTotp.slug, ContentType.V5DefaultWithTotp.slug ->
                        PickResourceAction.TOTP_REPLACE to ConfirmationType.REPLACE_TOTP
                    else -> error("This resource type does not support linking or replacing totplink: $slug")
                }

            updateViewState {
                copy(
                    showConfirmationDialog = true,
                    confirmationType = confirmationType,
                    pickAction = pickAction,
                )
            }
        }
    }

    private fun confirmOtpLink(pickAction: PickResourceAction) {
        val pickedResource = viewState.value.pickedResource ?: return
        updateViewState { copy(showConfirmationDialog = false) }
        emitSideEffect(NavigateBackWithResult(pickAction, pickedResource.resourceModel))
    }

    private fun loadResources() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            loadResourcesData(viewState.value.searchQuery)
        }
    }

    private suspend fun loadResourcesData(searchQuery: String) {
        val data =
            resourcePickerDataProvider.provideData(
                searchQuery = searchQuery.takeIf { it.isNotBlank() },
                suggestionUri = suggestionUri,
            )

        updateViewState { copy(resourcePickerData = data) }
    }

    private fun synchronizeWithDataRefresh() {
        viewModelScope.launch(coroutineLaunchContext.ui) {
            dataRefreshTrackingFlow.dataRefreshStatusFlow.collect { status ->
                when (status) {
                    is InProgress -> updateViewState { copy(isRefreshing = true, refreshProgress = status.progress) }
                    FinishedWithFailure -> {
                        emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                        updateViewState { copy(isRefreshing = false) }
                    }
                    FinishedWithSuccess -> {
                        updateViewState { copy(isRefreshing = false) }
                        loadResources()
                    }
                    NotCompleted -> {
                        // do nothing
                    }
                }
            }
        }
    }

    internal companion object {
        val SEARCH_DEBOUNCE = 300.milliseconds

        internal val SELECTABLE_RESOURCE_TYPES_SLUGS =
            setOf(
                ContentType.PasswordAndDescription.slug,
                ContentType.V5Default.slug,
                ContentType.PasswordDescriptionTotp.slug,
                ContentType.V5DefaultWithTotp.slug,
            )
    }
}
