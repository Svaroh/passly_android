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

package net.svaroh.passly.serializers

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.svaroh.passly.database.snapshot.ResourcesSnapshot
import net.svaroh.passly.dto.request.CreateResourceDto
import net.svaroh.passly.dto.response.ResourceResponseDto
import net.svaroh.passly.dto.response.ResourceTypeDto
import net.svaroh.passly.serializers.gson.CreateResourceModelSerializer
import net.svaroh.passly.serializers.gson.MetadataDecryptor
import net.svaroh.passly.serializers.gson.MetadataEncryptor
import net.svaroh.passly.serializers.gson.ResourceListDeserializer
import net.svaroh.passly.serializers.gson.ResourceListItemDeserializer
import net.svaroh.passly.serializers.gson.ResourceTypesListDeserializer
import net.svaroh.passly.serializers.gson.SingleResourceDeserializer
import net.svaroh.passly.serializers.gson.serializer.ZonedDateTimeSerializer
import net.svaroh.passly.serializers.gson.strictTypeAdapters
import net.svaroh.passly.serializers.gson.validation.JsonSchemaValidationRunner
import net.svaroh.passly.serializers.jsonschema.jsonSchemaModule
import net.svaroh.passly.ui.ParsedMetadataKeyModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.time.ZonedDateTime
import java.util.UUID

const val STRICT_ADAPTERS_ONLY_GSON = "RESOURCE_DTO_GSON"

val serializersModule =
    module {
        jsonSchemaModule()

        singleOf(::JsonSchemaValidationRunner)
        singleOf(::ResourceListDeserializer)
        singleOf(::ResourceTypesListDeserializer)
        singleOf(::ZonedDateTimeSerializer)
        factory {
            SingleResourceDeserializer(
                resourceTypeIdToSlugMappingProvider = get(),
                jsonSchemaValidationRunner = get(),
                getLocalMetadataKeys = get(),
                gson = get(named(STRICT_ADAPTERS_ONLY_GSON)),
            )
        }
        factory {
            (
                resourceTypeIdToSlugMapping: Map<UUID, String>,
                supportedResourceTypesIds: Set<UUID>,
                metadataKeys: List<ParsedMetadataKeyModel>,
                resourcesSnapshot: ResourcesSnapshot,
            ),
            ->
            ResourceListItemDeserializer(
                jsonSchemaValidationRunner = get(),
                gson = get(named(STRICT_ADAPTERS_ONLY_GSON)),
                metadataDecryptor = get<MetadataDecryptor> { parametersOf(metadataKeys) },
                coroutineLaunchContext = get(),
                resourceTypeIdToSlugMapping = resourceTypeIdToSlugMapping,
                supportedResourceTypesIds = supportedResourceTypesIds,
                resourcesSnapshot = resourcesSnapshot,
            )
        }
        factory { (metadataKeys: List<ParsedMetadataKeyModel>) ->
            MetadataDecryptor(
                getSelectedUserPrivateKeyUseCase = get(),
                passphraseMemoryCache = get(),
                openPgp = get(),
                sessionKeysCache = get(),
                coroutineLaunchContext = get(),
                metadataKeys = metadataKeys,
            )
        }
        factoryOf(::MetadataEncryptor)
        single {
            GsonBuilder()
                .serializeNulls()
                .apply {
                    strictTypeAdapters.forEach {
                        registerTypeAdapter(it.key, it.value)
                    }
                    registerTypeAdapter(
                        object : TypeToken<List<@JvmSuppressWildcards ResourceResponseDto>>() {}.type,
                        get<ResourceListDeserializer>(),
                    )
                    registerTypeAdapter(
                        object : TypeToken<List<@JvmSuppressWildcards ResourceTypeDto>>() {}.type,
                        get<ResourceTypesListDeserializer>(),
                    )
                    registerTypeAdapter(ZonedDateTime::class.java, get<ZonedDateTimeSerializer>())
                    registerTypeAdapter(
                        ResourceResponseDto::class.java,
                        get<SingleResourceDeserializer>(),
                    )
                    registerTypeAdapter(CreateResourceDto::class.java, CreateResourceModelSerializer())
                }.create()
        }

        single(named(STRICT_ADAPTERS_ONLY_GSON)) {
            GsonBuilder()
                .apply {
                    strictTypeAdapters.forEach {
                        registerTypeAdapter(it.key, it.value)
                    }
                }.create()
        }
    }
