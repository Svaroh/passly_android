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

package net.svaroh.passly.permissions.permissions

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.datarefresh.DataRefreshStatus
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderDetailsUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import net.svaroh.passly.domain.metadata.interactor.ResourceAccessInteractor
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.permissions.common.PermissionsListMapper
import net.svaroh.passly.permissions.permissions.PermissionsIntent.GoBack
import net.svaroh.passly.permissions.permissions.PermissionsIntent.MainButtonIntent
import net.svaroh.passly.permissions.permissions.PermissionsIntent.SeePermission
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateBack
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateToGroupPermissionDetails
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateToHome
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateToShareResource
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateToUserPermissionDetails
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.ShowToast
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.CANNOT_SHARE_RESOURCE
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.DATA_REFRESH_ERROR
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel
import net.svaroh.passly.ui.PermissionsItem
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.ResourcePermission
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
