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
import java.util.UUID

class MetadataDocumentFactory(
    private val gson: Gson,
    private val profile: MetadataProfile,
) {
    fun approximatePlaintextBytes(): Int = createDocument(seed = 0).toByteArray().size

    fun createDocument(seed: Int): String {
        val document =
            linkedMapOf<String, Any?>(
                OBJECT_TYPE to RESOURCE_METADATA_OBJECT_TYPE,
                NAME to "Benchmark resource #$seed",
                USERNAME to "benchmark$seed@passbolt.test",
            )
        if (profile.uris > 0) {
            document[URIS] =
                (1..profile.uris).map { uriIndex ->
                    "https://host-$seed-$uriIndex.passbolt.test/some/deep/path/segment/login?ref=$uriIndex"
                }
        }
        if (profile.descriptionChars > 0) {
            document[DESCRIPTION] = filler(profile.descriptionChars, seed)
        }
        if (profile.customFields > 0) {
            document[CUSTOM_FIELDS] =
                (1..profile.customFields).map { fieldIndex ->
                    linkedMapOf(
                        "id" to UUID.randomUUID().toString(),
                        "type" to "text",
                        "metadata_key" to "Custom field $fieldIndex",
                        "metadata_value" to filler(profile.customFieldValueChars, seed + fieldIndex),
                    )
                }
        }
        return gson.toJson(document)
    }

    private fun filler(
        length: Int,
        seed: Int,
    ): String {
        if (length <= 0) return ""
        val base = "Lorem ipsum dolor sit amet $seed consectetur adipiscing elit sed do eiusmod tempor. "
        return buildString(length + base.length) {
            while (this.length < length) append(base)
        }.substring(0, length)
    }

    private companion object {
        private const val OBJECT_TYPE = "object_type"
        private const val RESOURCE_METADATA_OBJECT_TYPE = "PASSBOLT_RESOURCE_METADATA"
        private const val NAME = "name"
        private const val USERNAME = "username"
        private const val URIS = "uris"
        private const val DESCRIPTION = "description"
        private const val CUSTOM_FIELDS = "custom_fields"
    }
}
