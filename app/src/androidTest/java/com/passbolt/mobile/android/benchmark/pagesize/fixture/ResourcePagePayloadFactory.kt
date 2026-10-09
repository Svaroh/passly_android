/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package net.svaroh.passly.benchmark.pagesize.fixture

import com.google.gson.Gson
import net.svaroh.passly.dto.response.MetadataKeyTypeDto
import net.svaroh.passly.dto.response.PermissionDto
import net.svaroh.passly.dto.response.ResourceResponseV5Dto
import okio.Buffer
import java.util.UUID

class ResourcePagePayloadFactory(
    private val gson: Gson,
    private val resourceTypeId: UUID,
    private val ownerUserId: UUID,
    documentFactory: MetadataDocumentFactory,
    encryptor: PeskEncryptor,
) {
    private val encryptedMetadataPool: List<EncryptedMetadata> =
        (0 until POOL_SIZE).map { seed -> encryptor.encrypt(documentFactory.createDocument(seed)) }

    val poolSize: Int get() = encryptedMetadataPool.size

    val approximateMetadataPlaintextBytes: Int = documentFactory.approximatePlaintextBytes()

    val probe: EncryptedMetadata get() = encryptedMetadataPool.first()

    fun sessionKeyHexForCorpusIndex(corpusIndex: Int): String =
        encryptedMetadataPool[corpusIndex % encryptedMetadataPool.size].sessionKeyHex

    fun renderPage(
        ids: List<UUID>,
        totalCount: Int,
        page: Int,
        limit: Int,
        startIndex: Int,
    ): Buffer {
        val buffer = Buffer()
        buffer.writeUtf8("{\"header\":")
        buffer.writeUtf8(gson.toJson(headerMap(totalCount, page, limit)))
        buffer.writeUtf8(",\"body\":[")
        ids.forEachIndexed { index, id ->
            if (index > 0) buffer.writeUtf8(",")
            val encryptedMetadata = encryptedMetadataPool[(startIndex + index) % encryptedMetadataPool.size]
            buffer.writeUtf8(
                gson.toJson(resourceDto(id, encryptedMetadata.armoredMessage), ResourceResponseV5Dto::class.java),
            )
        }
        buffer.writeUtf8("]}")
        return buffer
    }

    private fun headerMap(
        totalCount: Int,
        page: Int,
        limit: Int,
    ): Map<String, Any?> =
        linkedMapOf(
            "id" to UUID.randomUUID().toString(),
            "status" to "complete",
            "message" to "OK",
            "url" to "/resources.json",
            "code" to HTTP_OK,
            "servertime" to SERVER_TIME_EPOCH,
            "pagination" to
                linkedMapOf(
                    "count" to totalCount,
                    "page" to page,
                    "limit" to limit,
                ),
        )

    private fun resourceDto(
        id: UUID,
        encryptedMetadata: String,
    ) = ResourceResponseV5Dto(
        metadata = encryptedMetadata,
        metadataKeyId = ownerUserId,
        metadataKeyType = MetadataKeyTypeDto.PERSONAL,
        id = id,
        resourceTypeId = resourceTypeId,
        resourceFolderId = null,
        permission =
            PermissionDto(
                id = UUID.randomUUID(),
                type = PERMISSION_TYPE_OWNER,
                aco = "Resource",
                acoForeignKey = id,
                aro = "User",
                aroForeignKey = ownerUserId,
                created = null,
                modified = null,
            ),
        favorite = null,
        modified = MODIFIED_TIMESTAMP,
        tags = null,
        expired = null,
        permissions = null,
    )

    private companion object {
        private const val POOL_SIZE = 8
        private const val PERMISSION_TYPE_OWNER = 15
        private const val HTTP_OK = 200
        private const val SERVER_TIME_EPOCH = 1_700_000_000L
        private const val MODIFIED_TIMESTAMP = "2026-01-01T00:00:00Z"
    }
}
