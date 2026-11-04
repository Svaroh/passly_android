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
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.database.ResourceDatabase
import net.svaroh.passly.database.impl.sync.SyncStateDao
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.entity.sync.SyncState
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

class SyncEngineTest {
    private val networkStateProvider: NetworkStateProvider = mock()
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase = mock()
    private val databaseProvider: DatabaseProvider = mock()
    private val deltaSyncStrategy: DeltaSyncStrategy = mock()
    private val bootstrapStrategy: BootstrapStrategy = mock()
    private val manifestReconciler: ManifestReconciler = mock()
    private val syncStatusRepository: SyncStatusRepository = mock()
    private val db: ResourceDatabase = mock()
    private val syncStateDao: SyncStateDao = mock()

    private val engine = SyncEngine(
        networkStateProvider,
        getSelectedAccountUseCase,
        databaseProvider,
        deltaSyncStrategy,
        bootstrapStrategy,
        manifestReconciler,
        syncStatusRepository,
    )

    @Before
    fun setUp() = runTest {
        whenever(networkStateProvider.isAvailable).thenReturn(true)
        whenever(getSelectedAccountUseCase.execute(Unit)).thenReturn(
            GetSelectedAccountUseCase.Output(selectedAccount = "user-1"),
        )
        whenever(databaseProvider.get("user-1")).thenReturn(db)
        whenever(db.syncStateDao()).thenReturn(syncStateDao)
        whenever(manifestReconciler.execute()).thenReturn(
            ManifestReconcileResult.Success(totalRemote = 0, deletedExtraLocal = 0, needsRefetchCount = 0),
        )
    }

    @Test
    fun `sync is skipped when network is unavailable`() = runTest {
        whenever(networkStateProvider.isAvailable).thenReturn(false)

        val result = engine.sync(SyncTrigger.BACKGROUND)

        assertThat(result).isEqualTo(SyncResult.Skipped(SyncResult.Skipped.Reason.NO_NETWORK))
        verify(syncStatusRepository).onOffline()
    }

    @Test
    fun `sync is skipped when no account is selected`() = runTest {
        whenever(getSelectedAccountUseCase.execute(Unit)).thenReturn(
            GetSelectedAccountUseCase.Output(selectedAccount = null),
        )

        val result = engine.sync(SyncTrigger.BACKGROUND)

        assertThat(result).isEqualTo(SyncResult.Skipped(SyncResult.Skipped.Reason.NO_ACCOUNT))
    }

    @Test
    fun `initial sync triggers bootstrap when cursor is null`() = runTest {
        whenever(syncStateDao.getSyncState()).thenReturn(null)
        whenever(bootstrapStrategy.execute()).thenReturn(
            BootstrapResult.Success(
                stats = SyncStats(upserted = 15),
                pinnedCursor = 500L,
                epoch = "ep-1",
            ),
        )

        val result = engine.sync(SyncTrigger.BACKGROUND)

        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        val success = result as SyncResult.Success
        assertThat(success.stats.upserted).isEqualTo(15)
        verify(bootstrapStrategy).execute()
        verify(syncStatusRepository).onRunStarted(SyncStatus.Running.Phase.BOOTSTRAP)
        verify(syncStatusRepository).onSuccess(any())
        verify(syncStateDao).updateFailureCount(0)
    }

    @Test
    fun `subsequent sync triggers delta sync`() = runTest {
        whenever(syncStateDao.getSyncState()).thenReturn(
            SyncState(id = 1, cursor = 500L, epoch = "ep-1", lastSyncTimestamp = 1000L, lastReconcileTimestamp = System.currentTimeMillis()),
        )
        whenever(deltaSyncStrategy.execute(sinceCursor = 500L, epoch = "ep-1")).thenReturn(
            DeltaSyncResult.Success(
                stats = SyncStats(upserted = 2, deleted = 1),
                newCursor = 505L,
            ),
        )

        val result = engine.sync(SyncTrigger.BACKGROUND)

        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        val success = result as SyncResult.Success
        assertThat(success.stats.upserted).isEqualTo(2)
        assertThat(success.stats.deleted).isEqualTo(1)
        verify(deltaSyncStrategy).execute(sinceCursor = 500L, epoch = "ep-1")
        verify(syncStatusRepository).onRunStarted(SyncStatus.Running.Phase.DELTA)
    }

    @Test
    fun `delta 410 Gone triggers transparent fallback to bootstrap`() = runTest {
        whenever(syncStateDao.getSyncState()).thenReturn(
            SyncState(id = 1, cursor = 500L, epoch = "ep-1", lastSyncTimestamp = 1000L, lastReconcileTimestamp = System.currentTimeMillis()),
        )
        whenever(deltaSyncStrategy.execute(sinceCursor = 500L, epoch = "ep-1")).thenReturn(
            DeltaSyncResult.Gone,
        )
        whenever(bootstrapStrategy.execute()).thenReturn(
            BootstrapResult.Success(
                stats = SyncStats(upserted = 100),
                pinnedCursor = 1500L,
                epoch = "ep-2",
            ),
        )

        val result = engine.sync(SyncTrigger.BACKGROUND)

        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        verify(bootstrapStrategy).execute()
        verify(syncStatusRepository).onRunStarted(SyncStatus.Running.Phase.BOOTSTRAP)
    }

    @Test
    fun `manual sync forces manifest reconciliation pass`() = runTest {
        whenever(syncStateDao.getSyncState()).thenReturn(
            SyncState(id = 1, cursor = 500L, epoch = "ep-1", lastSyncTimestamp = 1000L, lastReconcileTimestamp = System.currentTimeMillis()),
        )
        whenever(deltaSyncStrategy.execute(sinceCursor = 500L, epoch = "ep-1")).thenReturn(
            DeltaSyncResult.Success(stats = SyncStats(), newCursor = 500L),
        )
        whenever(manifestReconciler.execute()).thenReturn(
            ManifestReconcileResult.Success(totalRemote = 10, deletedExtraLocal = 0, needsRefetchCount = 0),
        )

        val result = engine.sync(SyncTrigger.MANUAL)

        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        verify(manifestReconciler).execute()
        verify(syncStatusRepository).onRunStarted(SyncStatus.Running.Phase.RECONCILE)
    }

    @Test
    fun `session failure reports Failure with SESSION reason and schedules backoff`() = runTest {
        whenever(syncStateDao.getSyncState()).thenReturn(
            SyncState(id = 1, cursor = 500L, epoch = "ep-1", lastSyncTimestamp = 1000L, lastReconcileTimestamp = null, failureCount = 0),
        )
        val http401 = HttpException(Response.error<Any>(401, "".toResponseBody()))
        whenever(deltaSyncStrategy.execute(sinceCursor = 500L, epoch = "ep-1")).thenReturn(
            DeltaSyncResult.Failure(http401),
        )

        val result = engine.sync(SyncTrigger.BACKGROUND)

        assertThat(result).isEqualTo(SyncResult.Failure(SyncFailureReason.SESSION))
        verify(syncStateDao).updateFailureCount(1)
        verify(syncStatusRepository).onFailure(any(), any())
    }
}
