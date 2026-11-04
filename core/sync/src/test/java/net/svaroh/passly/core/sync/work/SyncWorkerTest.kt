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
package net.svaroh.passly.core.sync.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.sync.SyncEngine
import net.svaroh.passly.core.sync.SyncFailureReason
import net.svaroh.passly.core.sync.SyncResult
import net.svaroh.passly.core.sync.SyncStats
import net.svaroh.passly.core.sync.SyncTrigger
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SyncWorkerTest : KoinTest {
    private val syncEngine: SyncEngine = mock()
    private val context: Context = mock()
    private val workerParams: WorkerParameters = mock()

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { syncEngine }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `doWork returns success when engine returns Success`() = runTest {
        whenever(syncEngine.sync(SyncTrigger.BACKGROUND)).thenReturn(
            SyncResult.Success(stats = SyncStats(upserted = 5)),
        )

        val worker = SyncWorker(context, workerParams)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncEngine).sync(SyncTrigger.BACKGROUND)
    }

    @Test
    fun `doWork returns success when engine is Skipped`() = runTest {
        whenever(syncEngine.sync(SyncTrigger.BACKGROUND)).thenReturn(
            SyncResult.Skipped(SyncResult.Skipped.Reason.NO_NETWORK),
        )

        val worker = SyncWorker(context, workerParams)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(syncEngine).sync(SyncTrigger.BACKGROUND)
    }

    @Test
    fun `doWork returns retry when engine returns Failure`() = runTest {
        whenever(syncEngine.sync(SyncTrigger.BACKGROUND)).thenReturn(
            SyncResult.Failure(SyncFailureReason.UNREACHABLE),
        )

        val worker = SyncWorker(context, workerParams)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        verify(syncEngine).sync(SyncTrigger.BACKGROUND)
    }
}
