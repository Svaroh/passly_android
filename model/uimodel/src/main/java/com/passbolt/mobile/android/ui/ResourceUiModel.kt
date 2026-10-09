package net.svaroh.passly.ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.svaroh.passly.common.extension.isInFuture
import net.svaroh.passly.jsonmodel.CachedJsonModel
import net.svaroh.passly.jsonmodel.ParsedJson
import net.svaroh.passly.jsonmodel.delegates.RootRelativeJsonPathNullableStringDelegate
import net.svaroh.passly.jsonmodel.delegates.RootRelativeJsonPathNullableStringListDelegate
import net.svaroh.passly.jsonmodel.delegates.RootRelativeJsonPathStringDelegate
import net.svaroh.passly.supportedresourceTypes.ContentType
import java.time.ZonedDateTime

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

@Serializable
data class ResourceUiModel(
    val resourceId: String,
    val resourceTypeId: String,
    val slug: String,
    val folderId: String?,
    val permission: ResourcePermission,
    val favouriteId: String?,
    @Serializable(with = ZonedDateTimeKSerializer::class)
    val modified: ZonedDateTime,
    @Serializable(with = ZonedDateTimeKSerializer::class)
    val expiry: ZonedDateTime?,
    val metadataKeyId: String?,
    val metadataKeyType: MetadataKeyTypeModel?,
    val metadataJsonModel: MetadataJsonModel,
)

fun ResourceUiModel.contentType(): ContentType = ContentType.fromSlug(slug)

fun ResourceUiModel.isFavourite() = favouriteId != null

fun ResourceUiModel.isExpired() = expiry != null && !expiry.isInFuture()

data class ResourceUiModelWithAttributes(
    val resourceModel: ResourceUiModel,
    val resourceTags: List<TagModel>,
    val resourcePermissions: List<PermissionModel>,
    val favouriteId: String?,
)

enum class MetadataKeyTypeModel {
    SHARED,
    PERSONAL,
}

open class CreateResourceModel(
    val contentType: ContentType,
    val folderId: String?,
    val expiry: ZonedDateTime?,
    val metadataKeyId: String?,
    val metadataKeyType: MetadataKeyTypeModel?,
    val metadataJsonModel: MetadataJsonModel,
)

class UpdateResourceModel(
    val resourceId: String,
    contentType: ContentType,
    folderId: String?,
    expiry: ZonedDateTime?,
    metadataKeyId: String?,
    metadataKeyType: MetadataKeyTypeModel?,
    metadataJsonModel: MetadataJsonModel,
) : CreateResourceModel(
        contentType = contentType,
        folderId = folderId,
        expiry = expiry,
        metadataKeyId = metadataKeyId,
        metadataKeyType = metadataKeyType,
        metadataJsonModel = metadataJsonModel,
    )

@Serializable
data class MetadataJsonModel(
    override var json: String?,
) : CachedJsonModel {
    @Transient
    override var parsedCache: ParsedJson? = null

    var objectType: String by RootRelativeJsonPathStringDelegate(jsonPath = "object_type")

    var resourceTypeId: String by RootRelativeJsonPathStringDelegate(jsonPath = "resource_type_id")

    var name: String by RootRelativeJsonPathStringDelegate(jsonPath = "name")

    var username: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "username")

    var description: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "description")

    var uri: String? by RootRelativeJsonPathNullableStringDelegate(jsonPath = "uri")

    var uris: List<String>? by RootRelativeJsonPathNullableStringListDelegate(jsonPath = "uris")

    var icon: MetadataIconModel? by RootRelativeJsonPathIconDelegate(jsonPath = "icon")

    var customFields: MetadataCustomFieldsModel? by RootRelativeJsonPathMetadataCustomFieldsDelegate(jsonPath = "custom_fields")

    fun getMainUri(contentType: ContentType) =
        if (contentType.isV5()) {
            uris?.firstOrNull()
        } else {
            uri
        }.orEmpty()

    @Suppress("NestedBlockDepth")
    fun setMainUri(
        contentType: ContentType,
        mainUri: String,
    ) {
        if (contentType.isV5()) {
            uris =
                uris.let {
                    if (it.isNullOrEmpty()) {
                        listOf(mainUri)
                    } else {
                        it.toMutableList().apply {
                            set(0, mainUri)
                        }
                    }
                }
        } else {
            uri = mainUri
        }
    }

    /**
     * Warms the JsonPath parse cache for the fields the resource list row reads ([icon], [name],
     * [username]). The first access parses the whole document (cached via [parsedCache]); the rest warm
     * their per-path reads. Call off the main thread so UI composition reads the warm cache instead of
     * parsing inline.
     */
    fun warmCache(): MetadataJsonModel =
        apply {
            runCatching { listOf(icon, name, username) }
        }

    companion object {
        const val OBJECT_TYPE = "PASSBOLT_RESOURCE_METADATA"

        fun empty(): MetadataJsonModel =
            MetadataJsonModel(
                """
                {"name": ""}
                """.trimIndent(),
            )
    }
}
