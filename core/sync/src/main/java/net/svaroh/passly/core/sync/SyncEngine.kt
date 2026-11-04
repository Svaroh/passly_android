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

import kotlinx.coroutines.sync.Mutex
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.time.ZonedDateTime

/**
 * Orchestrates client synchronization against the Passly server according to the `sync/1` protocol.
 * Coordinates delta fetch, transparent bootstrap fallback, and manifest reconciliation.
 */
class SyncEngine(
    private val networkStateProvider: NetworkStateProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val databaseProvider: DatabaseProvider,
    private val deltaSyncStrategy: DeltaSyncStrategy,
    private val bootstrapStrategy: BootstrapStrategy,
    private val manifestReconciler: ManifestReconciler,
    private val syncStatusRepository: SyncStatusRepository,
) {
    private val syncMutex = Mutex()

    suspend fun sync(trigger: SyncTrigger): SyncResult {
        if (!syncMutex.tryLock()) {
            Timber.i("Sync skipped: already running")
            return SyncResult.Skipped(SyncResult.Skipped.Reason.ALREADY_RUNNING)
        }

        try {
            if (!networkStateProvider.isAvailable) {
                Timber.i("Sync skipped: no network connection")
                syncStatusRepository.onOffline()
                return SyncResult.Skipped(SyncResult.Skipped.Reason.NO_NETWORK)
            }

            val userId = getSelectedAccountUseCase.execute(Unit).selectedAccount
            if (userId == null) {
                Timber.w("Sync skipped: no active account selected")
                return SyncResult.Skipped(SyncResult.Skipped.Reason.NO_ACCOUNT)
            }

            val db = databaseProvider.get(userId)
            val currentState = db.syncStateDao().getSyncState()
            var accumulatedStats = SyncStats.EMPTY

            val currentCursor = currentState?.cursor
            // 1. Delta or Bootstrap
            if (currentCursor == null || currentCursor == 0L) {
                syncStatusRepository.onRunStarted(SyncStatus.Running.Phase.BOOTSTRAP)
                when (val result = bootstrapStrategy.execute()) {
                    is BootstrapResult.Success -> {
                        accumulatedStats = accumulatedStats + result.stats
                    }
                    is BootstrapResult.Failure -> {
                        return handleFailure(db, result.cause)
                    }
                }
            } else {
                syncStatusRepository.onRunStarted(SyncStatus.Running.Phase.DELTA)
                when (val result = deltaSyncStrategy.execute(sinceCursor = currentCursor, epoch = currentState.epoch)) {
                    is DeltaSyncResult.Success -> {
                        accumulatedStats = accumulatedStats + result.stats
                    }
                    is DeltaSyncResult.Gone -> {
                        Timber.i("Cursor expired on server (410 Gone); falling back to bootstrap")
                        syncStatusRepository.onRunStarted(SyncStatus.Running.Phase.BOOTSTRAP)
                        when (val bResult = bootstrapStrategy.execute()) {
                            is BootstrapResult.Success -> {
                                accumulatedStats = accumulatedStats + bResult.stats
                            }
                            is BootstrapResult.Failure -> {
                                return handleFailure(db, bResult.cause)
                            }
                        }
                    }
                    is DeltaSyncResult.Failure -> {
                        return handleFailure(db, result.cause)
                    }
                }
            }

            // 2. Manifest Reconciliation check
            val updatedState = db.syncStateDao().getSyncState()
            val lastReconcile = updatedState?.lastReconcileTimestamp
            val shouldReconcile = trigger == SyncTrigger.MANUAL ||
                lastReconcile == null ||
                (System.currentTimeMillis() - lastReconcile > RECONCILE_INTERVAL_MS)

            if (shouldReconcile) {
                syncStatusRepository.onRunStarted(SyncStatus.Running.Phase.RECONCILE)
                when (val mResult = manifestReconciler.execute()) {
                    is ManifestReconcileResult.Success -> {
                        Timber.d("Manifest reconciliation completed: $mResult")
                    }
                    is ManifestReconcileResult.Failure -> {
                        Timber.w(mResult.cause, "Manifest reconciliation warning")
                    }
                }
            }

            // 3. Mark success
            val now = ZonedDateTime.now()
            syncStatusRepository.onSuccess(now)
            db.syncStateDao().updateFailureCount(0)

            return SyncResult.Success(accumulatedStats)
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun handleFailure(
        db: net.svaroh.passly.database.ResourceDatabase,
        cause: Throwable,
    ): SyncResult.Failure {
        val failureReason = when (cause) {
            is HttpException -> {
                when (cause.code()) {
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> SyncFailureReason.SESSION
                    else -> SyncFailureReason.UNREACHABLE
                }
            }
            is IOException -> SyncFailureReason.UNREACHABLE
            else -> SyncFailureReason.PROTOCOL
        }

        val currentState = db.syncStateDao().getSyncState()
        val nextFailureCount = (currentState?.failureCount ?: 0) + 1
        db.syncStateDao().updateFailureCount(nextFailureCount)

        val retryDuration = SyncBackoff.delayAfter(nextFailureCount)
        val retryAt = ZonedDateTime.now().plus(retryDuration)

        syncStatusRepository.onFailure(failureReason, retryAt)
        return SyncResult.Failure(failureReason)
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val RECONCILE_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
}
