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

package net.svaroh.passly.folderdetails

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.folderdetails.FolderDetailsIntent.GoBack
import net.svaroh.passly.folderdetails.FolderDetailsIntent.GoToLocationDetails
import net.svaroh.passly.folderdetails.FolderDetailsIntent.GoToPermissionDetails
import net.svaroh.passly.folderdetails.FolderDetailsIntent.SharedWithClick
import net.svaroh.passly.folderdetails.FolderDetailsSideEffect.NavigateToFolderLocation
import net.svaroh.passly.folderdetails.FolderDetailsSideEffect.NavigateToFolderPermissions
import net.svaroh.passly.folderdetails.FolderDetailsSideEffect.NavigateToHome
import net.svaroh.passly.folderdetails.FolderDetailsSideEffect.NavigateUp
import net.svaroh.passly.folderdetails.FolderDetailsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.folderdetails.FolderDetailsSideEffect.ShowToast
import net.svaroh.passly.folderdetails.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import net.svaroh.passly.folderdetails.ToastType.CONTENT_NOT_AVAILABLE
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.RbacRuleModel.ALLOW
import kotlinx.coroutines.launch
import timber.log.Timber

internal class FolderDetailsViewModel(
    folderId: String,
    coroutineLaunchContext: CoroutineLaunchContext,
    private val getLocalFolderDetailsUseCase: GetLocalFolderDetailsUseCase,
    private val getLocalFolderLocationUseCase: GetLocalFolderLocationUseCase,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
) : SideEffectViewModel<FolderDetailsState, FolderDetailsSideEffect>(
        initialState = FolderDetailsState(folderId = folderId),
    ) {
    val folderId: String
        get() = requireNotNull(viewState.value.folderId)

    init {
        viewModelScope.launch(coroutineLaunchContext.io) {
            loadFolderDetails(folderId)
        }
        viewModelScope.launch(coroutineLaunchContext.io) {
            synchronizeWithDataRefresh(folderId)
        }
    }

    fun onIntent(intent: FolderDetailsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            GoToLocationDetails -> emitSideEffect(NavigateToFolderLocation(folderId))
            GoToPermissionDetails -> emitSideEffect(NavigateToFolderPermissions(folderId, PermissionsMode.VIEW))
            SharedWithClick -> emitSideEffect(NavigateToFolderPermissions(folderId, PermissionsMode.VIEW))
        }
    }

    private suspend fun loadFolderDetails(folderId: String) {
        try {
            val folder =
                getLocalFolderDetailsUseCase
                    .execute(GetLocalFolderDetailsUseCase.Input(folderId))
                    .folder

            val parentFolders =
                getLocalFolderLocationUseCase
                    .execute(GetLocalFolderLocationUseCase.Input(folderId))
                    .parentFolders

            val canViewPermissions =
                getRbacRulesUseCase
                    .execute(Unit)
                    .rbacModel
                    .shareViewRule == ALLOW

            val permissions =
                if (canViewPermissions) {
                    getLocalFolderPermissionsUseCase
                        .execute(GetLocalFolderPermissionsUseCase.Input(folderId))
                        .permissions
                } else {
                    emptyList()
                }

            updateViewState {
                copy(
                    folder = folder,
                    locationPath = parentFolders.map { it.name },
                    canViewPermissions = canViewPermissions,
                    permissions = permissions,
                )
            }
        } catch (_: NullPointerException) {
            emitSideEffect(ShowToast(CONTENT_NOT_AVAILABLE))
            emitSideEffect(NavigateToHome)
        } catch (throwable: Exception) {
            Timber.e(throwable)
        }
    }

    private suspend fun synchronizeWithDataRefresh(folderId: String) {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect {
            when (it) {
                InProgress -> updateViewState { copy(isRefreshing = true) }
                FinishedWithFailure -> {
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                    updateViewState { copy(isRefreshing = false) }
                }
                FinishedWithSuccess -> {
                    updateViewState { copy(isRefreshing = false) }
                    loadFolderDetails(folderId)
                }
                NotCompleted -> {
                    // do nothing
                }
            }
        }
    }
}
