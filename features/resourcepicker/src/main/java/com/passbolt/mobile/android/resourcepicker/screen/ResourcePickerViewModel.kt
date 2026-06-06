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
import net.svaroh.passly.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.CLEAR
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.NONE
import net.svaroh.passly.resourcepicker.model.ConfirmationType
import net.svaroh.passly.resourcepicker.model.PickResourceAction
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.ApplyClick
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.CloseConfirmationDialog
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.ConfirmOtpLink
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.GoBack
import net.svaroh.passly.resourcepicker.screen.ResourcePickerIntent.Initialize
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
import net.svaroh.passly.ui.selectedOnly
import kotlinx.coroutines.launch
import java.util.UUID

internal class ResourcePickerViewModel(
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val resourcePickerDataProvider: ResourcePickerDataProvider,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
) : SideEffectViewModel<ResourcePickerState, ResourcePickerSideEffect>(ResourcePickerState()) {
    private var suggestionUri: String? = null

    init {
        synchronizeWithDataRefresh()
    }

    fun onIntent(intent: ResourcePickerIntent) {
        when (intent) {
            is Initialize -> initialize(intent.suggestionUri)
            is Search -> searchQueryChanged(intent.searchQuery)
            is ResourcePicked -> resourcePicked(intent.resource)
            is ConfirmOtpLink -> confirmOtpLink(intent.pickAction)
            SearchEndIconAction -> searchEndIconAction()
            ApplyClick -> applyClick()
            CloseConfirmationDialog -> updateViewState { copy(showConfirmationDialog = false) }
            GoBack -> emitSideEffect(NavigateUp)
        }
    }

    private fun initialize(suggestionUri: String?) {
        this.suggestionUri = suggestionUri
        loadResources()
    }

    private fun searchQueryChanged(query: String) {
        val searchEndIcon = if (query.isBlank()) NONE else CLEAR
        viewModelScope.launch {
            updateViewState {
                copy(
                    searchQuery = query,
                    searchInputEndIconMode = searchEndIcon,
                )
            }
            loadResources()
        }
    }

    private fun searchEndIconAction() {
        when (viewState.value.searchInputEndIconMode) {
            CLEAR -> {
                searchQueryChanged("")
                updateViewState {
                    copy(searchInputEndIconMode = NONE)
                }
            }
            else -> {
                // no-op
            }
        }
    }

    private fun resourcePicked(resource: ResourcePickerListItem) {
        if (resource.isSelectable) {
            val resourceId = resource.resourceModel.resourceId
            val updatedSuggestedResources =
                viewState.value.resourcePickerData.suggestedResources
                    .selectedOnly(resourceId)
            val updatedResources =
                viewState.value.resourcePickerData.resources
                    .selectedOnly(resourceId)

            updateViewState {
                copy(
                    pickedResource = resource,
                    isApplyButtonEnabled = true,
                    resourcePickerData =
                        resourcePickerData.copy(
                            suggestedResources = updatedSuggestedResources,
                            resources = updatedResources,
                        ),
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
            val selectableIdToSlugMapping =
                getResourceTypeIdToSlugMappingUseCase
                    .execute(Unit)
                    .idToSlugMapping
                    .filter { it.value in SELECTABLE_RESOURCE_TYPES_SLUGS }

            val pickedResourceResourceTypeId = UUID.fromString(pickedResource.resourceModel.resourceTypeId)

            require(pickedResourceResourceTypeId in selectableIdToSlugMapping.keys)

            val (pickAction, confirmationType) =
                when (val slug = selectableIdToSlugMapping[pickedResourceResourceTypeId]) {
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
            val data =
                resourcePickerDataProvider.provideData(
                    searchQuery = viewState.value.searchQuery.takeIf { it.isNotBlank() },
                    suggestionUri = suggestionUri,
                )

            // Restore selection if exists
            val pickedResourceId =
                viewState.value.pickedResource
                    ?.resourceModel
                    ?.resourceId
            val updatedData =
                data.copy(
                    suggestedResources = data.suggestedResources.selectedOnly(pickedResourceId),
                    resources = data.resources.selectedOnly(pickedResourceId),
                )

            updateViewState { copy(resourcePickerData = updatedData) }
        }
    }

    private fun synchronizeWithDataRefresh() {
        viewModelScope.launch(coroutineLaunchContext.ui) {
            dataRefreshTrackingFlow.dataRefreshStatusFlow.collect { status ->
                when (status) {
                    InProgress -> updateViewState { copy(isRefreshing = true) }
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
        internal val SELECTABLE_RESOURCE_TYPES_SLUGS =
            listOf(
                ContentType.PasswordAndDescription.slug,
                ContentType.V5Default.slug,
                ContentType.PasswordDescriptionTotp.slug,
                ContentType.V5DefaultWithTotp.slug,
            )
    }
}
