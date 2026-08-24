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

package com.passbolt.mobile.android.permissions.permissions

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.metadata.interactor.ResourceAccessInteractor
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.permissions.common.PermissionsListMapper
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.MainButtonIntent
import com.passbolt.mobile.android.permissions.permissions.PermissionsIntent.SeePermission
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToGroupPermissionDetails
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToHome
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToShareResource
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.NavigateToUserPermissionDetails
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.permissions.permissions.PermissionsSideEffect.ShowToast
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.CANNOT_SHARE_RESOURCE
import com.passbolt.mobile.android.permissions.permissions.SnackbarErrorType.DATA_REFRESH_ERROR
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionModelUi.GroupPermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi.UserPermissionModel
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class PermissionsViewModel(
    permissionsItem: PermissionsItem,
    id: String,
    mode: PermissionsMode,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getLocalFolderUseCase: GetLocalFolderDetailsUseCase,
    private val permissionsListMapper: PermissionsListMapper,
    private val resourceAccessInteractor: ResourceAccessInteractor,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<PermissionsState, PermissionsSideEffect>(
        initialState =
            PermissionsState(
                permissionsItem = permissionsItem,
                permissionItemId = id,
                mode = mode,
            ),
    ) {
    private val missingItemHandler =
        CoroutineExceptionHandler { _, throwable ->
            if (throwable is IllegalStateException) {
                emitSideEffect(ShowToast(ToastType.CONTENT_NOT_AVAILABLE))
                emitSideEffect(NavigateToHome)
            }
        }

    init {
        loadInitialPermissions()
        processEditButtonVisibility()
        viewModelScope.launch(coroutineLaunchContext.ui) {
            synchronizeWithDataRefresh()
        }
    }

    fun onIntent(intent: PermissionsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            is SeePermission -> permissionClick(intent.permission)
            MainButtonIntent -> shareResourceClick()
        }
    }

    private suspend fun synchronizeWithDataRefresh() {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect { status ->
            when (status) {
                is DataRefreshStatus.InProgress -> { // no-op
                }
                DataRefreshStatus.Idle.FinishedWithFailure ->
                    emitSideEffect(ShowErrorSnackbar(DATA_REFRESH_ERROR))
                DataRefreshStatus.Idle.FinishedWithSuccess ->
                    viewModelScope.launch(coroutineLaunchContext.io) { reloadPermissions() }
                DataRefreshStatus.Idle.NotCompleted -> { // no-op
                }
            }
        }
    }

    private fun loadInitialPermissions() {
        viewModelScope.launch(missingItemHandler + coroutineLaunchContext.io) {
            if (viewState.value.permissions.isEmpty()) {
                val fetched = fetchPermissions()
                updatePermissions(fetched)
            }
        }
    }

    private suspend fun reloadPermissions() {
        val fetched = fetchPermissions()
        updatePermissions(fetched)
    }

    private suspend fun fetchPermissions(): List<PermissionModelUi> =
        when (viewState.value.permissionsItem) {
            PermissionsItem.RESOURCE ->
                getLocalResourcePermissionsUseCase
                    .execute(GetLocalResourcePermissionsUseCase.Input(viewState.value.permissionItemId))
                    .permissions
            PermissionsItem.FOLDER ->
                getLocalFolderPermissionsUseCase
                    .execute(GetLocalFolderPermissionsUseCase.Input(viewState.value.permissionItemId))
                    .permissions
        }

    private fun updatePermissions(permissions: List<PermissionModelUi>) {
        updateViewState {
            val sorted = permissionsListMapper.sorted(permissions)
            copy(
                permissions = sorted,
                showEmptyState = sorted.isEmpty(),
            )
        }
    }

    private fun processEditButtonVisibility() {
        viewModelScope.launch(missingItemHandler + coroutineLaunchContext.io) {
            val isOwner =
                when (viewState.value.permissionsItem) {
                    PermissionsItem.RESOURCE ->
                        getLocalResourceUseCase
                            .execute(GetLocalResourceUseCase.Input(viewState.value.permissionItemId))
                            .resource.permission == ResourcePermission.OWNER
                    PermissionsItem.FOLDER ->
                        getLocalFolderUseCase
                            .execute(GetLocalFolderDetailsUseCase.Input(viewState.value.permissionItemId))
                            .folder.permission == ResourcePermission.OWNER
                }
            if (isOwner && viewState.value.permissionsItem == PermissionsItem.RESOURCE) {
                updateViewState { copy(showEditButton = true) }
            }
        }
    }

    private fun permissionClick(permission: PermissionModelUi) {
        when (permission) {
            is GroupPermissionModel -> emitSideEffect(NavigateToGroupPermissionDetails(permission, PermissionsMode.VIEW))
            is UserPermissionModel -> emitSideEffect(NavigateToUserPermissionDetails(permission, PermissionsMode.VIEW))
        }
    }

    private fun shareResourceClick() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            if (resourceAccessInteractor.canShareResource()) {
                emitSideEffect(NavigateToShareResource(viewState.value.permissionItemId))
            } else {
                emitSideEffect(ShowErrorSnackbar(CANNOT_SHARE_RESOURCE))
            }
        }
    }
}
