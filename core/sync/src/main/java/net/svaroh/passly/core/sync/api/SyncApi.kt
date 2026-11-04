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
package net.svaroh.passly.core.sync.api

import net.svaroh.passly.core.sync.dto.SyncBootstrapDto
import net.svaroh.passly.core.sync.dto.SyncChangesDto
import net.svaroh.passly.core.sync.dto.SyncManifestItemDto
import net.svaroh.passly.core.sync.dto.SyncMetaDto
import net.svaroh.passly.dto.response.BasePaginatedResponse
import net.svaroh.passly.dto.response.BaseResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SyncApi {
    @GET("sync/changes.json")
    suspend fun getChanges(
        @Query("since") since: Long,
        @Query("limit") limit: Int = DEFAULT_LIMIT,
    ): BaseResponse<SyncChangesDto>

    @GET("sync/bootstrap.json")
    suspend fun getBootstrap(
        @Query("page") page: Int = FIRST_PAGE,
        @Query("limit") limit: Int = DEFAULT_LIMIT,
        @Query("cursor") cursor: Long? = null,
    ): BasePaginatedResponse<SyncBootstrapDto>

    @GET("sync/manifest.json")
    suspend fun getManifest(
        @Query("type") type: String = DEFAULT_MANIFEST_TYPE,
    ): BaseResponse<List<SyncManifestItemDto>>

    @GET("sync/meta.json")
    suspend fun getMeta(): BaseResponse<SyncMetaDto>

    companion object {
        const val DEFAULT_LIMIT = 500
        const val FIRST_PAGE = 1
        const val DEFAULT_MANIFEST_TYPE = "resources"
    }
}
