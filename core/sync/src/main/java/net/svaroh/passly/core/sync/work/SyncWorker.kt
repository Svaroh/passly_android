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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.svaroh.passly.core.sync.SyncEngine
import net.svaroh.passly.core.sync.SyncResult
import net.svaroh.passly.core.sync.SyncTrigger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * WorkManager CoroutineWorker for executing background delta synchronization runs.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val syncEngine: SyncEngine by inject()

    override suspend fun doWork(): Result {
        Timber.i("SyncWorker: background delta sync triggered")
        return when (val result = syncEngine.sync(SyncTrigger.BACKGROUND)) {
            is SyncResult.Success -> {
                Timber.i("SyncWorker: finished successfully ($result)")
                Result.success()
            }
            is SyncResult.Skipped -> {
                Timber.i("SyncWorker: skipped ($result)")
                Result.success()
            }
            is SyncResult.Failure -> {
                Timber.w("SyncWorker: failed ($result), scheduling retry")
                Result.retry()
            }
        }
    }
}
