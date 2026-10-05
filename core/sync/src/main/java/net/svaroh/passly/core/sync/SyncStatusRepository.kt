/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime

/**
 * In-memory publisher of the current [SyncStatus].
 *
 * The durable part of sync state (cursor, last successful run, failure counter) lives in the account database and is
 * owned by the sync state store; this class only holds what the UI observes during a session.
 */
class SyncStatusRepository {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle(lastSuccessAt = null))

    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun onRunStarted(phase: SyncStatus.Running.Phase) {
        _status.update { SyncStatus.Running(it.lastSuccessAt, phase) }
    }

    fun onProgress(
        phase: SyncStatus.Running.Phase,
        processed: Int,
        total: Int? = null,
    ) {
        _status.update { SyncStatus.Running(it.lastSuccessAt, phase, processed, total) }
    }

    fun onSuccess(at: ZonedDateTime) {
        _status.value = SyncStatus.Idle(lastSuccessAt = at)
    }

    fun onOffline() {
        _status.update { SyncStatus.Offline(it.lastSuccessAt) }
    }

    fun onFailure(
        reason: SyncFailureReason,
        retryAt: ZonedDateTime?,
    ) {
        _status.update { SyncStatus.Failed(it.lastSuccessAt, reason, retryAt) }
    }

    /**
     * Seeds the observable state from the persisted last successful run, so that a freshly started process shows the
     * real freshness of the replica instead of "never synchronised".
     */
    fun restoreLastSuccess(at: ZonedDateTime?) {
        _status.value = SyncStatus.Idle(lastSuccessAt = at)
    }
}
