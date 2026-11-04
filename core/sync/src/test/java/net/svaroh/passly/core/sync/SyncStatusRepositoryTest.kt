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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.ZonedDateTime

class SyncStatusRepositoryTest {
    private val repository = SyncStatusRepository()

    @Test
    fun `initial status is Idle with null lastSuccessAt`() {
        val initial = repository.status.value
        assertThat(initial).isInstanceOf(SyncStatus.Idle::class.java)
        assertThat(initial.lastSuccessAt).isNull()
    }

    @Test
    fun `onRunStarted updates status to Running with specified phase`() {
        repository.onRunStarted(SyncStatus.Running.Phase.BOOTSTRAP)
        val status = repository.status.value
        assertThat(status).isInstanceOf(SyncStatus.Running::class.java)
        val running = status as SyncStatus.Running
        assertThat(running.phase).isEqualTo(SyncStatus.Running.Phase.BOOTSTRAP)
    }

    @Test
    fun `onSuccess transitions to Idle with timestamp`() {
        val now = ZonedDateTime.now()
        repository.onSuccess(now)
        val status = repository.status.value
        assertThat(status).isEqualTo(SyncStatus.Idle(lastSuccessAt = now))
    }

    @Test
    fun `onOffline transitions to Offline preserving lastSuccessAt`() {
        val now = ZonedDateTime.now()
        repository.onSuccess(now)
        repository.onOffline()
        val status = repository.status.value
        assertThat(status).isEqualTo(SyncStatus.Offline(lastSuccessAt = now))
    }

    @Test
    fun `onFailure transitions to Failed with reason and retry schedule`() {
        val now = ZonedDateTime.now()
        val retry = now.plusMinutes(5)
        repository.onSuccess(now)
        repository.onFailure(SyncFailureReason.UNREACHABLE, retry)

        val status = repository.status.value
        assertThat(status).isEqualTo(
            SyncStatus.Failed(lastSuccessAt = now, reason = SyncFailureReason.UNREACHABLE, retryAt = retry),
        )
    }
}
