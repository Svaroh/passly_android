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

import net.svaroh.passly.common.transaction.DatabaseTransactionRunner
import net.svaroh.passly.core.sync.api.SyncApi
import net.svaroh.passly.core.sync.dto.SyncManifestItemDto
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import timber.log.Timber

sealed interface ManifestReconcileResult {
    data class Success(
        val totalRemote: Int,
        val deletedExtraLocal: Int,
        val needsRefetchCount: Int,
    ) : ManifestReconcileResult

    data class Failure(
        val cause: Throwable,
    ) : ManifestReconcileResult
}

/**
 * Reconciles local resources against the server's compact manifest (/sync/manifest.json).
 * Detects silently revoked resources, purging orphaned local rows and identifying discrepancies.
 */
class ManifestReconciler(
    private val syncApi: SyncApi,
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val databaseTransactionRunner: DatabaseTransactionRunner,
) {
    suspend fun execute(): ManifestReconcileResult {
        try {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount) {
                "No selected account during manifest reconciliation"
            }
            val db = databaseProvider.get(userId)

            val manifestResponse = syncApi.getManifest(type = "resources")
            val remoteItems = manifestResponse.body

            val remoteMap = remoteItems.associateBy { it.id }

            var deletedCount = 0
            var needsRefetch = 0

            databaseTransactionRunner.runInTransaction {
                val localResources = db.resourcesDao().getAll()
                val localIds = localResources.map { it.resourceId }.toSet()

                val extraLocalIds = localIds.filter { !remoteMap.containsKey(it) }
                if (extraLocalIds.isNotEmpty()) {
                    Timber.i("Manifest reconciliation: deleting ${extraLocalIds.size} orphaned local resources")
                    db.resourcesDao().deleteAll(extraLocalIds)
                    deletedCount = extraLocalIds.size
                }

                val remoteMissingLocally = remoteItems.count { !localIds.contains(it.id) }
                needsRefetch = remoteMissingLocally

                val currentState = db.syncStateDao().getSyncState()
                if (currentState != null) {
                    db.syncStateDao().upsert(
                        currentState.copy(
                            lastReconcileTimestamp = System.currentTimeMillis(),
                        ),
                    )
                }
            }

            return ManifestReconcileResult.Success(
                totalRemote = remoteItems.size,
                deletedExtraLocal = deletedCount,
                needsRefetchCount = needsRefetch,
            )
        } catch (e: Exception) {
            Timber.e(e, "Error during manifest reconciliation")
            return ManifestReconcileResult.Failure(e)
        }
    }
}
