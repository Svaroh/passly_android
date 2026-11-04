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
import net.svaroh.passly.common.transaction.DatabaseTransactionRunner
import net.svaroh.passly.core.sync.api.SyncApi
import net.svaroh.passly.core.sync.dto.SyncManifestItemDto
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.database.ResourceDatabase
import net.svaroh.passly.database.impl.resources.ResourcesDao
import net.svaroh.passly.database.impl.sync.SyncStateDao
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.dto.response.BaseResponse
import net.svaroh.passly.dto.response.HeaderResponse
import net.svaroh.passly.dto.response.StatusResponse
import net.svaroh.passly.entity.resource.Permission
import net.svaroh.passly.entity.resource.ResourceWithMetadata
import net.svaroh.passly.entity.sync.SyncState
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import java.time.ZonedDateTime

class ManifestReconcilerTest {
    private val syncApi: SyncApi = mock()
    private val databaseProvider: DatabaseProvider = mock()
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase = mock()
    private val databaseTransactionRunner: DatabaseTransactionRunner = mock()
    private val db: ResourceDatabase = mock()
    private val resourcesDao: ResourcesDao = mock()
    private val syncStateDao: SyncStateDao = mock()

    private val reconciler = ManifestReconciler(
        syncApi,
        databaseProvider,
        getSelectedAccountUseCase,
        databaseTransactionRunner,
    )

    private fun testHeader() = HeaderResponse(
        id = "1",
        status = StatusResponse.COMPLETE,
        message = "ok",
        url = "/sync/manifest.json",
        code = 200,
        serverTime = 1234567890L,
    )

    @Before
    fun setUp() = runTest {
        whenever(getSelectedAccountUseCase.execute(Unit)).thenReturn(
            GetSelectedAccountUseCase.Output(selectedAccount = "user-1"),
        )
        whenever(databaseProvider.get("user-1")).thenReturn(db)
        whenever(db.resourcesDao()).thenReturn(resourcesDao)
        whenever(db.syncStateDao()).thenReturn(syncStateDao)
        whenever(databaseTransactionRunner.runInTransaction(any<suspend () -> Any?>())).thenAnswer {
            val block = it.getArgument<suspend () -> Any?>(0)
            kotlinx.coroutines.runBlocking { block() }
        }
    }

    @Test
    fun `reconcile deletes local resources that are not present in server manifest`() = runTest {
        val remoteManifest = listOf(
            SyncManifestItemDto(id = "res-1", modified = "2026-11-01T10:00:00Z"),
        )
        whenever(syncApi.getManifest(type = "resources")).thenReturn(
            BaseResponse(header = testHeader(), body = remoteManifest),
        )

        val localRes1 = createResourceWithMetadata("res-1")
        val localRes2 = createResourceWithMetadata("res-2") // Orphaned on server!
        whenever(resourcesDao.getAll()).thenReturn(listOf(localRes1, localRes2))
        whenever(syncStateDao.getSyncState()).thenReturn(SyncState(id = 1, cursor = 100L))

        val result = reconciler.execute()

        assertThat(result).isInstanceOf(ManifestReconcileResult.Success::class.java)
        val success = result as ManifestReconcileResult.Success
        assertThat(success.totalRemote).isEqualTo(1)
        assertThat(success.deletedExtraLocal).isEqualTo(1)

        verify(resourcesDao).deleteAll(listOf("res-2"))
        verify(syncStateDao).upsert(any())
    }

    @Test
    fun `reconcile reports Failure when remote API call fails`() = runTest {
        whenever(syncApi.getManifest(any())).thenAnswer {
            throw IOException("Network error")
        }

        val result = reconciler.execute()

        assertThat(result).isInstanceOf(ManifestReconcileResult.Failure::class.java)
    }

    private fun createResourceWithMetadata(id: String) =
        ResourceWithMetadata(
            resourceId = id,
            folderId = null,
            resourcePermission = Permission.OWNER,
            resourceTypeId = "type-1",
            slug = "password-and-description",
            favouriteId = null,
            modified = ZonedDateTime.now(),
            expiry = null,
            metadataJson = "{}",
            metadataKeyId = null,
            metadataKeyType = null,
        )
}
