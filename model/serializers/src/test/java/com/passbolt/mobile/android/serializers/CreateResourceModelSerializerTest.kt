/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
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

package com.passbolt.mobile.android.serializers

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.request.CreateV4ResourceDto
import com.passbolt.mobile.android.dto.request.CreateV5ResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto
import com.passbolt.mobile.android.serializers.gson.CreateResourceModelSerializer
import org.junit.Test

class CreateResourceModelSerializerTest {
    private val gson =
        GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(CreateResourceDto::class.java, CreateResourceModelSerializer())
            .create()

    @Test
    fun `null secrets are not serialized into the v4 payload`() {
        val json = gson.toJsonTree(v4Dto(secrets = null), CreateResourceDto::class.java).asJsonObject

        assertThat(json.has("secrets")).isFalse()
        assertThat(json.has("name")).isTrue()
        assertThat(json.has("folder_parent_id")).isTrue()
    }

    @Test
    fun `secrets are serialized into the v4 payload when present`() {
        val json =
            gson
                .toJsonTree(
                    v4Dto(secrets = listOf(EncryptedSecret("user-id", "encrypted"))),
                    CreateResourceDto::class.java,
                ).asJsonObject

        assertThat(json.has("secrets")).isTrue()
        assertThat(json.getAsJsonArray("secrets")).hasSize(1)
    }

    @Test
    fun `null secrets are not serialized into the v5 payload`() {
        val json = gson.toJsonTree(v5Dto(secrets = null), CreateResourceDto::class.java).asJsonObject

        assertThat(json.has("secrets")).isFalse()
        assertThat(json.has("metadata")).isTrue()
    }

    @Test
    fun `secrets are serialized into the v5 payload when present`() {
        val json =
            gson
                .toJsonTree(
                    v5Dto(secrets = listOf(EncryptedSecret("user-id", "encrypted"))),
                    CreateResourceDto::class.java,
                ).asJsonObject

        assertThat(json.has("secrets")).isTrue()
        assertThat(json.getAsJsonArray("secrets")).hasSize(1)
    }

    private fun v4Dto(secrets: List<EncryptedSecret>?) =
        CreateV4ResourceDto(
            name = "name",
            resourceTypeId = "resource-type-id",
            secrets = secrets,
            username = "username",
            uri = "uri",
            description = "description",
            folderParentId = null,
            expiry = null,
        )

    private fun v5Dto(secrets: List<EncryptedSecret>?) =
        CreateV5ResourceDto(
            resourceTypeId = "resource-type-id",
            secrets = secrets,
            folderParentId = null,
            expiry = null,
            metadata = "encrypted-metadata",
            metadataKeyId = "metadata-key-id",
            metadataKeyType = MetadataKeyTypeDto.SHARED,
        )
}
