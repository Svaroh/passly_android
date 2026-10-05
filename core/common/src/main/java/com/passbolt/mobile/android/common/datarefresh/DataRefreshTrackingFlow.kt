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
package net.svaroh.passly.common.datarefresh

import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class DataRefreshTrackingFlow {
    val dataRefreshStatusFlow: StateFlow<DataRefreshStatus>
        get() = _dataRefreshStatusFlow
    private val _dataRefreshStatusFlow = MutableStateFlow<DataRefreshStatus>(NotCompleted)

    /**
     * Whether the refresh currently being tracked was asked for by the user.
     *
     * A background refresh must stay invisible: with a local replica there is nothing for the user to wait for, and a
     * server that cannot be reached is a freshness problem, not an application error. Progress and failures are only
     * worth showing to someone who explicitly pulled to refresh.
     */
    @Volatile
    var isUserInitiated: Boolean = false
        private set

    fun startTracking(isUserInitiated: Boolean) {
        this.isUserInitiated = isUserInitiated
        _dataRefreshStatusFlow.value = InProgress(progress = 0f)
    }

    fun updateStatus(newStatus: DataRefreshStatus) {
        _dataRefreshStatusFlow.value = newStatus
    }

    fun isInProgress(): Boolean = _dataRefreshStatusFlow.value is InProgress

    suspend fun awaitIdle() {
        _dataRefreshStatusFlow.first { it is Idle }
    }
}
