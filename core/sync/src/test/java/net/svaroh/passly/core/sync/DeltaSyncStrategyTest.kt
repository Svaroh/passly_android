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
import net.svaroh.passly.core.sync.dto.SyncChangesDto
import net.svaroh.passly.core.sync.dto.SyncDeletionsDto
import net.svaroh.passly.core.sync.dto.SyncUpsertsDto
import net.svaroh.passly.dto.response.BaseResponse
import net.svaroh.passly.dto.response.HeaderResponse
import net.svaroh.passly.dto.response.StatusResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class DeltaSyncStrategyTest {
    private val syncApi: SyncApi = mock()
    private val changesApplier: ChangesApplier = mock()
    private val strategy = DeltaSyncStrategy(syncApi, changesApplier)

    private fun testHeader() = HeaderResponse(
        id = "1",
        status = StatusResponse.COMPLETE,
        message = "ok",
        url = "/sync/changes.json",
        code = 200,
        serverTime = 1234567890L,
    )

    @Test
    fun `single page delta applies changes and returns success`() = runTest {
        val changes = SyncChangesDto(
            cursor = 100L,
            hasMore = false,
            upserts = SyncUpsertsDto(),
            deletions = SyncDeletionsDto(),
        )
        whenever(syncApi.getChanges(since = 50L, limit = 500)).thenReturn(
            BaseResponse(header = testHeader(), body = changes),
        )
        whenever(changesApplier.applyChanges(eq(changes), eq("epoch-1"))).thenReturn(
            SyncStats(upserted = 3, deleted = 1),
        )

        val result = strategy.execute(sinceCursor = 50L, epoch = "epoch-1")

        assertThat(result).isInstanceOf(DeltaSyncResult.Success::class.java)
        val success = result as DeltaSyncResult.Success
        assertThat(success.newCursor).isEqualTo(100L)
        assertThat(success.stats.upserted).isEqualTo(3)
        assertThat(success.stats.deleted).isEqualTo(1)
        verify(changesApplier).applyChanges(changes, "epoch-1")
    }

    @Test
    fun `multi page delta queries until hasMore is false and accumulates stats`() = runTest {
        val page1 = SyncChangesDto(
            cursor = 100L,
            hasMore = true,
            upserts = SyncUpsertsDto(),
            deletions = SyncDeletionsDto(),
        )
        val page2 = SyncChangesDto(
            cursor = 150L,
            hasMore = false,
            upserts = SyncUpsertsDto(),
            deletions = SyncDeletionsDto(),
        )
        whenever(syncApi.getChanges(since = 50L, limit = 500)).thenReturn(
            BaseResponse(header = testHeader(), body = page1),
        )
        whenever(syncApi.getChanges(since = 100L, limit = 500)).thenReturn(
            BaseResponse(header = testHeader(), body = page2),
        )

        whenever(changesApplier.applyChanges(eq(page1), eq("epoch-1"))).thenReturn(SyncStats(upserted = 5))
        whenever(changesApplier.applyChanges(eq(page2), eq("epoch-1"))).thenReturn(SyncStats(upserted = 2, deleted = 1))

        val result = strategy.execute(sinceCursor = 50L, epoch = "epoch-1")

        assertThat(result).isInstanceOf(DeltaSyncResult.Success::class.java)
        val success = result as DeltaSyncResult.Success
        assertThat(success.newCursor).isEqualTo(150L)
        assertThat(success.stats.upserted).isEqualTo(7)
        assertThat(success.stats.deleted).isEqualTo(1)
    }

    @Test
    fun `server 410 Gone returns DeltaSyncResult Gone`() = runTest {
        val httpException = HttpException(Response.error<Any>(410, "".toResponseBody()))
        whenever(syncApi.getChanges(any(), any())).thenThrow(httpException)

        val result = strategy.execute(sinceCursor = 50L, epoch = "epoch-1")

        assertThat(result).isEqualTo(DeltaSyncResult.Gone)
    }

    @Test
    fun `network error returns DeltaSyncResult Failure`() = runTest {
        whenever(syncApi.getChanges(any(), any())).thenAnswer {
            throw IOException("Connection reset")
        }

        val result = strategy.execute(sinceCursor = 50L, epoch = "epoch-1")

        assertThat(result).isInstanceOf(DeltaSyncResult.Failure::class.java)
        val failure = result as DeltaSyncResult.Failure
        assertThat(failure.cause).isInstanceOf(IOException::class.java)
    }
}
