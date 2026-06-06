package net.svaroh.passly.core.fulldatarefresh

import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.fulldatarefresh.HomeDataInteractor.Output.Failure
import net.svaroh.passly.core.fulldatarefresh.HomeDataInteractor.Output.Success
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

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

class FullDataRefreshExecutor(
    private val homeDataInteractor: HomeDataInteractor,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) {
    suspend fun performFullDataRefresh() {
        Timber.d("Full data refresh initiated")
        if (!dataRefreshTrackingFlow.isInProgress()) {
            dataRefreshTrackingFlow.updateStatus(InProgress(progress = 0f))
            val output =
                runAuthenticatedOperation {
                    withContext(coroutineLaunchContext.default) {
                        homeDataInteractor.refreshAllHomeScreenData { progress ->
                            dataRefreshTrackingFlow.updateStatus(InProgress(progress))
                        }
                    }
                }

            when (output) {
                is Success -> {
                    dataRefreshTrackingFlow.updateStatus(InProgress(progress = 1f))
                    delay(FULL_PROGRESS_DISPLAY_MILLIS.milliseconds)
                    dataRefreshTrackingFlow.updateStatus(FinishedWithSuccess)
                }
                is Failure -> dataRefreshTrackingFlow.updateStatus(FinishedWithFailure)
            }
        }
    }

    private companion object {
        private const val FULL_PROGRESS_DISPLAY_MILLIS = 300L
    }
}
