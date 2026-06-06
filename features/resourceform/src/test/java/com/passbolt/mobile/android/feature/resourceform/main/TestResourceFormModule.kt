package net.svaroh.passly.feature.resourceform.main

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.idlingresource.CreateResourceIdlingResource
import net.svaroh.passly.core.idlingresource.UpdateResourceIdlingResource
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.entropy.EntropyCalculator
import net.svaroh.passly.core.policies.usecase.GetPasswordPoliciesUseCase
import net.svaroh.passly.core.resources.actions.ResourceUpdateActionsInteractorFactory
import net.svaroh.passly.core.resources.actions.SecretPropertiesActionsInteractorFactory
import net.svaroh.passly.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import net.svaroh.passly.core.resources.usecase.GetEditContentTypeUseCase
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import net.svaroh.passly.jsonmodel.JSON_MODEL_GSON
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathJsonPathOps
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathsOps
import net.svaroh.passly.mappers.EntropyViewMapper
import net.svaroh.passly.mappers.ResourceFormMapper
import net.svaroh.passly.metadata.interactor.MetadataPrivateKeysHelperInteractor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.Mockito.mock
import java.util.EnumSet

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

internal val mockGetPasswordPoliciesUseCase = mock<GetPasswordPoliciesUseCase>()
internal val mockSecretGenerator = mock<SecretGenerator>()
internal val mockEntropyCalculator = mock<EntropyCalculator>()
internal val mockGetDefaultCreateContentTypeUseCase = mock<GetDefaultCreateContentTypeUseCase>()
internal val mockGetEditContentTypeUseCase = mock<GetEditContentTypeUseCase>()
internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockMetadataPrivateKeysHelperInteractor = mock<MetadataPrivateKeysHelperInteractor>()
internal val mockSecretPropertiesActionsInteractorSecretPropertiesActionsInteractorFactory =
    mock<SecretPropertiesActionsInteractorFactory>()
internal val mockResourceUpdateActionsInteractorFactory = mock<ResourceUpdateActionsInteractorFactory>()

@OptIn(ExperimentalCoroutinesApi::class)
internal val testResourceFormModule =
    module {
        factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
        factoryOf(::ResourceFormMapper)
        factoryOf(::EntropyViewMapper)
        singleOf(::ResourceModelHandler)
        factoryOf(::ResourceTypesUpdatesAdjacencyGraph)
        factoryOf(::CreateResourceIdlingResource)
        factoryOf(::UpdateResourceIdlingResource)

        single { mockGetDefaultCreateContentTypeUseCase }
        single { mockGetEditContentTypeUseCase }
        single { mockGetLocalResourceUseCase }
        single<SecretPropertiesActionsInteractorFactory> { mockSecretPropertiesActionsInteractorSecretPropertiesActionsInteractorFactory }
        single<ResourceUpdateActionsInteractorFactory> { mockResourceUpdateActionsInteractorFactory }
        single {
            mapOf(
                DefaultValue.NAME to "no name",
            )
        }

        viewModel { params ->
            ResourceFormViewModel(
                mode = params.get(),
                getPasswordPoliciesUseCase = mockGetPasswordPoliciesUseCase,
                secretGenerator = mockSecretGenerator,
                entropyCalculator = mockEntropyCalculator,
                metadataPrivateKeysHelperInteractor = mockMetadataPrivateKeysHelperInteractor,
                getLocalResourceUseCase = get(),
                entropyViewMapper = get(),
                resourceFormMapper = get(),
                resourceModelHandler = get(),
                dataRefreshTrackingFlow = get(),
                createResourceIdlingResource = get(),
                updateResourceIdlingResource = get(),
                resourceUpdateActionsInteractorFactory = get(),
            )
        }

        single(named(JSON_MODEL_GSON)) { Gson() }
        single {
            Configuration
                .builder()
                .jsonProvider(GsonJsonProvider())
                .mappingProvider(GsonMappingProvider())
                .options(EnumSet.noneOf(Option::class.java))
                .build()
        }
        singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
        singleOf(::DataRefreshTrackingFlow)
        singleOf(::SessionRefreshTrackingFlow)
    }
