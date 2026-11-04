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
import net.svaroh.passly.core.sync.dto.SyncBootstrapDto
import net.svaroh.passly.core.sync.dto.SyncChangesDto
import net.svaroh.passly.core.sync.dto.SyncDeletionsDto
import net.svaroh.passly.core.sync.dto.SyncManifestItemDto
import net.svaroh.passly.core.sync.dto.SyncUpsertsDto
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.database.ResourceDatabase
import net.svaroh.passly.database.impl.folders.FoldersDao
import net.svaroh.passly.database.impl.groups.GroupsDao
import net.svaroh.passly.database.impl.resources.ResourcesDao
import net.svaroh.passly.database.impl.sync.SyncStateDao
import net.svaroh.passly.database.impl.users.UsersDao
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.resourcetypes.usecase.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.dto.response.BasePaginatedResponse
import net.svaroh.passly.dto.response.BaseResponse
import net.svaroh.passly.dto.response.HeaderResponse
import net.svaroh.passly.dto.response.HeaderWithPaginationResponse
import net.svaroh.passly.dto.response.Pagination
import net.svaroh.passly.dto.response.StatusResponse
import net.svaroh.passly.entity.resource.Permission
import net.svaroh.passly.entity.resource.ResourceWithMetadata
import net.svaroh.passly.entity.sync.SyncState
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.mappers.ResourceModelMapper
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.ZonedDateTime

/**
 * End-to-end integration test verifying the full lifecycle of the sync/1 engine:
 * 1. Cold start bootstrap
 * 2. Incremental delta sync
 * 3. 410 Gone recovery
 * 4. Manifest reconciliation & access revocation
 * 5. Offline skipped runs
 */
class SyncIntegrationE2ETest {
    private val syncApi: SyncApi = mock()
    private val networkStateProvider: NetworkStateProvider = mock()
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase = mock()
    private val databaseProvider: DatabaseProvider = mock()
    private val databaseTransactionRunner: DatabaseTransactionRunner = mock()
    private val resourceModelMapper: ResourceModelMapper = mock()
    private val permissionsModelMapper: PermissionsModelMapper = mock()
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider = mock()

    private val db: ResourceDatabase = mock()
    private val syncStateDao: SyncStateDao = mock()
    private val resourcesDao: ResourcesDao = mock()
    private val foldersDao: FoldersDao = mock()
    private val usersDao: UsersDao = mock()
    private val groupsDao: GroupsDao = mock()

    private val syncStatusRepository = SyncStatusRepository()

    private lateinit var changesApplier: ChangesApplier
    private lateinit var deltaSyncStrategy: DeltaSyncStrategy
    private lateinit var bootstrapStrategy: BootstrapStrategy
    private lateinit var manifestReconciler: ManifestReconciler
    private lateinit var syncEngine: SyncEngine

    // Simulated local persistent state in Room
    private var persistedSyncState: SyncState? = null

    private fun testHeader(url: String = "") = HeaderResponse(
        id = "1",
        status = StatusResponse.COMPLETE,
        message = "ok",
        url = url,
        code = 200,
        serverTime = 1234567890L,
    )

    private fun testPaginationHeader(url: String, count: Int, page: Int = 1, limit: Int = 500) =
        HeaderWithPaginationResponse(
            id = "1",
            status = StatusResponse.COMPLETE,
            message = "ok",
            url = url,
            code = 200,
            serverTime = 1234567890L,
            pagination = Pagination(count = count, page = page, limit = limit),
        )

    @Before
    fun setUp() = runTest {
        persistedSyncState = null

        whenever(networkStateProvider.isAvailable).thenReturn(true)
        whenever(getSelectedAccountUseCase.execute(Unit)).thenReturn(
            GetSelectedAccountUseCase.Output(selectedAccount = "user-alice"),
        )
        whenever(databaseProvider.get("user-alice")).thenReturn(db)
        whenever(db.syncStateDao()).thenReturn(syncStateDao)
        whenever(db.resourcesDao()).thenReturn(resourcesDao)
        whenever(db.foldersDao()).thenReturn(foldersDao)
        whenever(db.usersDao()).thenReturn(usersDao)
        whenever(db.groupsDao()).thenReturn(groupsDao)

        whenever(syncStateDao.getSyncState()).thenAnswer { persistedSyncState }
        whenever(syncStateDao.upsert(any())).thenAnswer {
            val state = it.getArgument<SyncState>(0)
            persistedSyncState = state
            1L
        }
        whenever(syncStateDao.updateFailureCount(any())).thenAnswer {
            val count = it.getArgument<Int>(0)
            persistedSyncState = persistedSyncState?.copy(failureCount = count)
            1
        }

        whenever(databaseTransactionRunner.runInTransaction(any<suspend () -> Any?>())).thenAnswer {
            val block = it.getArgument<suspend () -> Any?>(0)
            kotlinx.coroutines.runBlocking { block() }
        }

        changesApplier = ChangesApplier(
            databaseProvider,
            getSelectedAccountUseCase,
            databaseTransactionRunner,
            resourceModelMapper,
            permissionsModelMapper,
            resourceTypeIdToSlugMappingProvider,
        )
        deltaSyncStrategy = DeltaSyncStrategy(syncApi, changesApplier)
        bootstrapStrategy = BootstrapStrategy(syncApi, changesApplier)
        manifestReconciler = ManifestReconciler(
            syncApi,
            databaseProvider,
            getSelectedAccountUseCase,
            databaseTransactionRunner,
        )
        syncEngine = SyncEngine(
            networkStateProvider,
            getSelectedAccountUseCase,
            databaseProvider,
            deltaSyncStrategy,
            bootstrapStrategy,
            manifestReconciler,
            syncStatusRepository,
        )
    }

    @Test
    fun `full lifecycle E2E - bootstrap then delta then manifest reconciliation`() = runTest {
        // --- STEP 1: Cold start (database is empty, cursor is null) ---
        val bootstrapBody = SyncBootstrapDto(
            cursor = 1000L,
            epoch = "epoch-2026-v1",
            upserts = SyncUpsertsDto(),
        )
        whenever(syncApi.getBootstrap(page = eq(1), limit = any(), cursor = anyOrNull())).thenReturn(
            BasePaginatedResponse(
                header = testPaginationHeader(url = "/sync/bootstrap.json", count = 0),
                body = bootstrapBody,
            ),
        )

        val bootstrapResult = syncEngine.sync(SyncTrigger.BACKGROUND)
        assertThat(bootstrapResult).isInstanceOf(SyncResult.Success::class.java)
        assertThat(persistedSyncState?.cursor).isEqualTo(1000L)
        assertThat(persistedSyncState?.epoch).isEqualTo("epoch-2026-v1")
        assertThat(syncStatusRepository.status.value).isInstanceOf(SyncStatus.Idle::class.java)

        // --- STEP 2: Incremental delta sync (cursor advances to 1025) ---
        val deltaChanges = SyncChangesDto(
            cursor = 1025L,
            hasMore = false,
            upserts = SyncUpsertsDto(),
            deletions = SyncDeletionsDto(folders = listOf("deleted-folder-id")),
        )
        whenever(syncApi.getChanges(since = 1000L, limit = 500)).thenReturn(
            BaseResponse(header = testHeader("/sync/changes.json"), body = deltaChanges),
        )

        val deltaResult = syncEngine.sync(SyncTrigger.BACKGROUND)
        assertThat(deltaResult).isInstanceOf(SyncResult.Success::class.java)
        val deltaSuccess = deltaResult as SyncResult.Success
        assertThat(deltaSuccess.stats.deleted).isEqualTo(1)
        assertThat(persistedSyncState?.cursor).isEqualTo(1025L)
        verify(foldersDao).deleteAll(listOf("deleted-folder-id"))

        // --- STEP 3: Manual sync triggers Manifest Reconciliation & Access Revocation ---
        val remoteManifest = listOf(
            SyncManifestItemDto(id = "res-valid-1", modified = "2026-11-01T10:00:00Z"),
        )
        whenever(syncApi.getChanges(since = 1025L, limit = 500)).thenReturn(
            BaseResponse(
                header = testHeader("/sync/changes.json"),
                body = SyncChangesDto(cursor = 1025L, hasMore = false, upserts = SyncUpsertsDto(), deletions = SyncDeletionsDto()),
            ),
        )
        whenever(syncApi.getManifest(type = "resources")).thenReturn(
            BaseResponse(header = testHeader("/sync/manifest.json"), body = remoteManifest),
        )

        val localResValid = createResourceWithMetadata("res-valid-1")
        val localResRevoked = createResourceWithMetadata("res-revoked-secret")
        whenever(resourcesDao.getAll()).thenReturn(listOf(localResValid, localResRevoked))

        val manualResult = syncEngine.sync(SyncTrigger.MANUAL)
        assertThat(manualResult).isInstanceOf(SyncResult.Success::class.java)
        // Access revoked on server -> resource removed locally
        verify(resourcesDao).deleteAll(listOf("res-revoked-secret"))
        assertThat(persistedSyncState?.lastReconcileTimestamp).isNotNull()
    }

    @Test
    fun `410 Gone recovery E2E - transparent bootstrap resets cursor without data loss`() = runTest {
        persistedSyncState = SyncState(id = 1, cursor = 200L, epoch = "old-epoch")

        val http410 = HttpException(Response.error<Any>(410, "Gone".toResponseBody()))
        whenever(syncApi.getChanges(since = 200L, limit = 500)).thenThrow(http410)

        val newBootstrap = SyncBootstrapDto(
            cursor = 9999L,
            epoch = "new-epoch-2026",
            upserts = SyncUpsertsDto(),
        )
        whenever(syncApi.getBootstrap(page = 1, limit = 500, cursor = null)).thenReturn(
            BasePaginatedResponse(
                header = testPaginationHeader(url = "/sync/bootstrap.json", count = 0),
                body = newBootstrap,
            ),
        )

        val result = syncEngine.sync(SyncTrigger.BACKGROUND)

        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        assertThat(persistedSyncState?.cursor).isEqualTo(9999L)
        assertThat(persistedSyncState?.epoch).isEqualTo("new-epoch-2026")
        // Verify no local data was purged
        verify(resourcesDao, never()).deleteAll(any())
    }

    @Test
    fun `offline behavior E2E - no network round trips and status updated to Offline`() = runTest {
        whenever(networkStateProvider.isAvailable).thenReturn(false)

        val result = syncEngine.sync(SyncTrigger.BACKGROUND)

        assertThat(result).isEqualTo(SyncResult.Skipped(SyncResult.Skipped.Reason.NO_NETWORK))
        assertThat(syncStatusRepository.status.value).isInstanceOf(SyncStatus.Offline::class.java)
        verify(syncApi, never()).getChanges(any(), any())
        verify(syncApi, never()).getBootstrap(any(), any(), anyOrNull())
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
