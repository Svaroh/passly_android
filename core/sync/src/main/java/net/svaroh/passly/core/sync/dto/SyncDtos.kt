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
package net.svaroh.passly.core.sync.dto

import com.google.gson.annotations.SerializedName
import net.svaroh.passly.dto.response.FolderResponseDto
import net.svaroh.passly.dto.response.GroupsResponseDto
import net.svaroh.passly.dto.response.MetadataKeysResponseDto
import net.svaroh.passly.dto.response.ResourceResponseDto
import net.svaroh.passly.dto.response.ResourceTypeDto
import net.svaroh.passly.dto.response.SecretDto
import net.svaroh.passly.dto.response.UserDto

/**
 * Snapshot entities keyed by collection, returned during delta and bootstrap sync.
 */
data class SyncUpsertsDto(
    @SerializedName("resources")
    val resources: List<ResourceResponseDto> = emptyList(),
    @SerializedName("secrets")
    val secrets: List<SecretDto> = emptyList(),
    @SerializedName("folders")
    val folders: List<FolderResponseDto> = emptyList(),
    @SerializedName("users")
    val users: List<UserDto> = emptyList(),
    @SerializedName("groups")
    val groups: List<GroupsResponseDto> = emptyList(),
    @SerializedName("resource_types")
    val resourceTypes: List<ResourceTypeDto> = emptyList(),
    @SerializedName("metadata_keys")
    val metadataKeys: List<MetadataKeysResponseDto> = emptyList(),
)

/**
 * Identifier lists of entities that the caller can no longer see (deleted or revoked access).
 */
data class SyncDeletionsDto(
    @SerializedName("resources")
    val resources: List<String> = emptyList(),
    @SerializedName("folders")
    val folders: List<String> = emptyList(),
    @SerializedName("users")
    val users: List<String> = emptyList(),
    @SerializedName("groups")
    val groups: List<String> = emptyList(),
)

/**
 * Response payload for `GET /sync/changes.json`.
 */
data class SyncChangesDto(
    @SerializedName("cursor")
    val cursor: Long,
    @SerializedName("has_more")
    val hasMore: Boolean = false,
    @SerializedName("upserts")
    val upserts: SyncUpsertsDto = SyncUpsertsDto(),
    @SerializedName("deletions")
    val deletions: SyncDeletionsDto = SyncDeletionsDto(),
)

/**
 * Response payload for `GET /sync/bootstrap.json`.
 */
data class SyncBootstrapDto(
    @SerializedName("cursor")
    val cursor: Long,
    @SerializedName("epoch")
    val epoch: String,
    @SerializedName("upserts")
    val upserts: SyncUpsertsDto = SyncUpsertsDto(),
)

/**
 * One item from the lightweight reconciliation list returned by `GET /sync/manifest.json`.
 */
data class SyncManifestItemDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("modified")
    val modified: String,
    @SerializedName("secret_modified")
    val secretModified: String? = null,
)

/**
 * Server protocol metadata from `GET /sync/meta.json`.
 */
data class SyncMetaDto(
    @SerializedName("protocol")
    val protocol: String,
    @SerializedName("epoch")
    val epoch: String,
    @SerializedName("min_client")
    val minClient: String,
    @SerializedName("max_page_size")
    val maxPageSize: Int,
    @SerializedName("retention_days")
    val retentionDays: Int,
)
