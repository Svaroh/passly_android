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
import net.svaroh.passly.core.sync.api.SyncApi
import net.svaroh.passly.core.sync.dto.SyncBootstrapDto
import net.svaroh.passly.core.sync.dto.SyncUpsertsDto
import net.svaroh.passly.dto.response.BasePaginatedResponse
import net.svaroh.passly.dto.response.HeaderWithPaginationResponse
import net.svaroh.passly.dto.response.Pagination
import net.svaroh.passly.dto.response.StatusResponse
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

class BootstrapStrategyTest {
    private val syncApi: SyncApi = mock()
    private val changesApplier: ChangesApplier = mock()
    private val strategy = BootstrapStrategy(syncApi, changesApplier)

    @Test
    fun `single page bootstrap completes and returns pinned cursor`() = runTest {
        val bootstrap = SyncBootstrapDto(
            cursor = 1000L,
            epoch = "epoch-abc",
            upserts = SyncUpsertsDto(),
        )
        val response = BasePaginatedResponse(
            header = HeaderWithPaginationResponse(
                id = "1",
                status = StatusResponse.COMPLETE,
                message = "ok",
                url = "/sync/bootstrap.json",
                code = 200,
                serverTime = 1234567890L,
                pagination = Pagination(count = 10, page = 1, limit = 500),
            ),
            body = bootstrap,
        )
        whenever(syncApi.getBootstrap(page = 1, limit = 500, cursor = null)).thenReturn(response)
        whenever(changesApplier.applyBootstrapPage(bootstrap, 1)).thenReturn(SyncStats(upserted = 10))

        val result = strategy.execute(limit = 500)

        assertThat(result).isInstanceOf(BootstrapResult.Success::class.java)
        val success = result as BootstrapResult.Success
        assertThat(success.pinnedCursor).isEqualTo(1000L)
        assertThat(success.epoch).isEqualTo("epoch-abc")
        assertThat(success.stats.upserted).isEqualTo(10)
        verify(changesApplier).applyBootstrapPage(bootstrap, 1)
    }

    @Test
    fun `multi page bootstrap pins cursor from page 1 and passes to page 2`() = runTest {
        val page1Body = SyncBootstrapDto(
            cursor = 5000L,
            epoch = "epoch-123",
            upserts = SyncUpsertsDto(),
        )
        val page2Body = SyncBootstrapDto(
            cursor = 5000L,
            epoch = "epoch-123",
            upserts = SyncUpsertsDto(),
        )
        val page1Response = BasePaginatedResponse(
            header = HeaderWithPaginationResponse(
                id = "1",
                status = StatusResponse.COMPLETE,
                message = "ok",
                url = "/sync/bootstrap.json",
                code = 200,
                serverTime = 1234567890L,
                pagination = Pagination(count = 800, page = 1, limit = 500),
            ),
            body = page1Body,
        )
        val page2Response = BasePaginatedResponse(
            header = HeaderWithPaginationResponse(
                id = "2",
                status = StatusResponse.COMPLETE,
                message = "ok",
                url = "/sync/bootstrap.json",
                code = 200,
                serverTime = 1234567890L,
                pagination = Pagination(count = 800, page = 2, limit = 500),
            ),
            body = page2Body,
        )

        whenever(syncApi.getBootstrap(page = 1, limit = 500, cursor = null)).thenReturn(page1Response)
        whenever(syncApi.getBootstrap(page = 2, limit = 500, cursor = 5000L)).thenReturn(page2Response)

        whenever(changesApplier.applyBootstrapPage(page1Body, 1)).thenReturn(SyncStats(upserted = 500))
        whenever(changesApplier.applyBootstrapPage(page2Body, 2)).thenReturn(SyncStats(upserted = 300))

        val result = strategy.execute(limit = 500)

        assertThat(result).isInstanceOf(BootstrapResult.Success::class.java)
        val success = result as BootstrapResult.Success
        assertThat(success.pinnedCursor).isEqualTo(5000L)
        assertThat(success.stats.upserted).isEqualTo(800)
    }

    @Test
    fun `bootstrap error returns Failure`() = runTest {
        whenever(syncApi.getBootstrap(any(), any(), anyOrNull())).thenAnswer {
            throw IOException("Timeout")
        }

        val result = strategy.execute()

        assertThat(result).isInstanceOf(BootstrapResult.Failure::class.java)
        val failure = result as BootstrapResult.Failure
        assertThat(failure.cause).isInstanceOf(IOException::class.java)
    }
}
