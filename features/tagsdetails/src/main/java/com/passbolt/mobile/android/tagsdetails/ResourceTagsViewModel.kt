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

package net.svaroh.passly.tagsdetails

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceTagsUseCase
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.tagsdetails.ResourceTagsIntent.GoBack
import net.svaroh.passly.tagsdetails.ResourceTagsSideEffect.NavigateBack
import net.svaroh.passly.tagsdetails.ResourceTagsSideEffect.NavigateToHome
import net.svaroh.passly.tagsdetails.ResourceTagsSideEffect.ShowContentNotAvailable
import net.svaroh.passly.tagsdetails.ResourceTagsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.tagsdetails.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ResourceTagsViewModel(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val resourceId: String,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalResourceTagsUseCase: GetLocalResourceTagsUseCase,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
) : SideEffectViewModel<ResourceTagsState, ResourceTagsSideEffect>(ResourceTagsState()) {
    init {
        viewModelScope.launch(coroutineLaunchContext.io) {
            synchronizeWithDataRefresh(resourceId)
        }
        viewModelScope.launch(coroutineLaunchContext.io) {
            loadData(resourceId)
        }
    }

    fun onIntent(intent: ResourceTagsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
        }
    }

    private suspend fun synchronizeWithDataRefresh(resourceId: String) {
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
                    loadData(resourceId)
                }
                NotCompleted -> {
                    // do nothing
                }
            }
        }
    }

    private suspend fun loadData(resourceId: String) {
        try {
            val resourceResult =
                getLocalResourceUseCase.execute(
                    GetLocalResourceUseCase.Input(resourceId),
                )
            val tagsResult =
                getLocalResourceTagsUseCase.execute(
                    GetLocalResourceTagsUseCase.Input(resourceId),
                )
            updateViewState {
                copy(
                    resourceModel = resourceResult.resource,
                    tags = tagsResult.tags,
                )
            }
        } catch (_: NullPointerException) {
            emitSideEffect(ShowContentNotAvailable)
            emitSideEffect(NavigateToHome)
        } catch (throwable: Exception) {
            Timber.e(throwable)
        }
    }
}
