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

package net.svaroh.passly.core.secrets.usecase.decrypt.parser

import com.google.gson.GsonBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import net.svaroh.passly.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.jsonmodel.JSON_MODEL_GSON
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathJsonPathOps
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathsOps
import net.svaroh.passly.serializers.gson.validation.JsonSchemaValidationRunner
import net.svaroh.passly.serializers.jsonschema.schamarepository.JSFJsonSchemaValidator
import net.svaroh.passly.serializers.jsonschema.schamarepository.JSFSchemaRepository
import net.svaroh.passly.serializers.jsonschema.schamarepository.JsonSchemaRepository
import net.svaroh.passly.serializers.jsonschema.schamarepository.JsonSchemaValidator
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.Validator
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.mock
import java.util.EnumSet

internal val mockJSFSchemaRepository = mock<JSFSchemaRepository>()
internal val mockIdToSlugMappingProvider = mock<ResourceTypeIdToSlugMappingProvider>()

val testParserModule =
    module {
        single { GsonBuilder().create() }
        singleOf(::JsonSchemaValidationRunner)
        single { Validator() }
        single<JsonSchemaRepository<Schema>> {
            mockJSFSchemaRepository
        }
        single<JsonSchemaValidator> {
            JSFJsonSchemaValidator(
                schemaRepository = get(),
                validator = get(),
            )
        }
        single {
            SecretParser(
                secretValidationRunner = get(),
                resourceTypeIdToSlugMappingProvider = mockIdToSlugMappingProvider,
            )
        }
        single(named(JSON_MODEL_GSON)) { GsonBuilder().serializeNulls().create() }
        single {
            Configuration
                .builder()
                .jsonProvider(GsonJsonProvider())
                .mappingProvider(GsonMappingProvider())
                .options(EnumSet.noneOf(Option::class.java))
                .build()
        }
        singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
    }
