package net.svaroh.passly.feature.resourceform.main

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.common.hash.MessageDigestHash
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.idlingresource.CreateResourceIdlingResource
import net.svaroh.passly.core.idlingresource.UpdateResourceIdlingResource
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passwordgenerator.PinCodeGenerator
import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.entropy.EntropyCalculator
import net.svaroh.passly.core.passwordgenerator.usecase.CheckPasswordPropertiesUseCase
import net.svaroh.passly.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.folders.usecase.FetchFolderPermissionsUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.domain.metadata.usecase.GetMetadataTypesSettingsUseCase
import net.svaroh.passly.domain.passwordexpiry.usecase.PasswordExpiryPoliciesInteractor
import net.svaroh.passly.domain.passwordpolicies.usecase.GetPasswordPoliciesUseCase
import net.svaroh.passly.domain.passwordpolicies.usecase.PasswordPoliciesInteractor
import net.svaroh.passly.domain.resources.actions.ResourceCreateActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourceUpdateActionsInteractorFactory
import net.svaroh.passly.domain.resources.actions.SecretPropertiesActionsInteractorFactory
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsConfirmationInteractor
import net.svaroh.passly.domain.resources.usecase.EditPermissionsConfirmationInteractor
import net.svaroh.passly.domain.resources.usecase.FetchResourcePermissionsUseCase
import net.svaroh.passly.domain.resources.usecase.GetDefaultCreateContentTypeUseCase
import net.svaroh.passly.domain.resources.usecase.GetEditContentTypeUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.entity.featureflags.FeatureFlagsModel
import net.svaroh.passly.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import net.svaroh.passly.jsonmodel.JSON_MODEL_GSON
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathJsonPathOps
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathsOps
import net.svaroh.passly.mappers.ResourceFormMapper
import net.svaroh.passly.ui.MetadataTypeModel.V4
import net.svaroh.passly.ui.MetadataTypesSettingsModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
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

internal val DEFAULT_TEST_FEATURE_FLAGS =
    FeatureFlagsModel(
        privacyPolicyUrl = null,
        termsAndConditionsUrl = null,
        isPreviewPasswordAvailable = false,
        areFoldersAvailable = false,
        areTagsAvailable = false,
        isTotpAvailable = false,
        isRbacAvailable = false,
        isPasswordExpiryAvailable = false,
        arePasswordPoliciesAvailable = false,
        canUpdatePasswordPolicies = false,
        isV5MetadataAvailable = false,
    )

internal val mockGetPasswordPoliciesUseCase = mock<GetPasswordPoliciesUseCase>()
internal val mockPasswordPoliciesInteractor = mock<PasswordPoliciesInteractor>()
internal val mockPasswordExpiryPoliciesInteractor = mock<PasswordExpiryPoliciesInteractor>()
internal val mockGetFeatureFlagsUseCase =
    mock<GetFeatureFlagsUseCase>().apply {
        stub { on { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(DEFAULT_TEST_FEATURE_FLAGS) }
    }
internal val mockSecretGenerator = mock<SecretGenerator>()
internal val mockPinCodeGenerator = mock<PinCodeGenerator>()
internal val mockEntropyCalculator = mock<EntropyCalculator>()
internal val mockGetDefaultCreateContentTypeUseCase = mock<GetDefaultCreateContentTypeUseCase>()
internal val mockGetEditContentTypeUseCase = mock<GetEditContentTypeUseCase>()
internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockMetadataPrivateKeysHelperInteractor = mock<MetadataPrivateKeysHelperInteractor>()
internal val mockSecretPropertiesActionsInteractorSecretPropertiesActionsInteractorFactory =
    mock<SecretPropertiesActionsInteractorFactory>()
internal val mockResourceUpdateActionsInteractorFactory = mock<ResourceUpdateActionsInteractorFactory>()
internal val mockResourceCreateActionsInteractor = mock<ResourceCreateActionsInteractor>()
internal val mockCheckPasswordPropertiesUseCase = mock<CheckPasswordPropertiesUseCase>()
internal val mockGetMetadataTypesSettingsUseCase = mock<GetMetadataTypesSettingsUseCase>()
internal val mockGetLocalFolderPermissionsUseCase = mock<GetLocalFolderPermissionsUseCase>()
internal val mockFetchFolderPermissionsUseCase = mock<FetchFolderPermissionsUseCase>()
internal val mockFetchResourcePermissionsUseCase = mock<FetchResourcePermissionsUseCase>()
internal val mockGetLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>()
internal val mockGetSelectedAccountDataUseCase = mock<GetSelectedAccountDataUseCase>()

internal val DEFAULT_FEATURE_FLAGS =
    FeatureFlagsModel(
        privacyPolicyUrl = null,
        termsAndConditionsUrl = null,
        isPreviewPasswordAvailable = false,
        areFoldersAvailable = false,
        areTagsAvailable = false,
        isTotpAvailable = false,
        isRbacAvailable = false,
        isPasswordExpiryAvailable = false,
        arePasswordPoliciesAvailable = false,
        canUpdatePasswordPolicies = false,
        isV5MetadataAvailable = false,
    )

internal val DEFAULT_METADATA_TYPES_SETTINGS =
    MetadataTypesSettingsModel(
        defaultMetadataType = V4,
        defaultFolderType = V4,
        defaultTagType = V4,
        allowCreationOfV5Resources = false,
        allowCreationOfV5Folders = false,
        allowCreationOfV5Tags = false,
        allowCreationOfV4Resources = true,
        allowCreationOfV4Folders = true,
        allowCreationOfV4Tags = true,
        allowV4V5Upgrade = false,
        allowV5V4Downgrade = false,
    )

@OptIn(ExperimentalCoroutinesApi::class)
internal val testResourceFormModule =
    module {
        factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
        factoryOf(::ResourceFormMapper)
        singleOf(::ResourceModelHandler)
        singleOf(::MessageDigestHash)
        factoryOf(::ResourceTypesUpdatesAdjacencyGraph)
        factoryOf(::CreateResourceIdlingResource)
        factoryOf(::UpdateResourceIdlingResource)

        single { mockGetDefaultCreateContentTypeUseCase }
        single { mockGetEditContentTypeUseCase }
        single { mockGetLocalResourceUseCase }
        single<SecretPropertiesActionsInteractorFactory> { mockSecretPropertiesActionsInteractorSecretPropertiesActionsInteractorFactory }
        single<ResourceUpdateActionsInteractorFactory> { mockResourceUpdateActionsInteractorFactory }
        single<ResourceCreateActionsInteractor> { mockResourceCreateActionsInteractor }
        single {
            mapOf(
                DefaultValue.NAME to "no name",
            )
        }

        single { mock<GetSessionExpiryUseCase>() }
        single { mock<PassphraseMemoryCache>() }

        viewModel { params ->
            ResourceFormViewModel(
                mode = params.get(),
                getPasswordPoliciesUseCase = mockGetPasswordPoliciesUseCase,
                getOrLoadGeneratorSettingsUseCase = GetOrLoadGeneratorSettingsUseCase(mockGetPasswordPoliciesUseCase),
                passwordPoliciesInteractor = mockPasswordPoliciesInteractor,
                passwordExpiryPoliciesInteractor = mockPasswordExpiryPoliciesInteractor,
                getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
                coroutineLaunchContext = get(),
                secretGenerator = mockSecretGenerator,
                pinCodeGenerator = mockPinCodeGenerator,
                entropyCalculator = mockEntropyCalculator,
                metadataPrivateKeysHelperInteractor = mockMetadataPrivateKeysHelperInteractor,
                getLocalResourceUseCase = get(),
                resourceFormMapper = get(),
                resourceModelHandler = get(),
                dataRefreshTrackingFlow = get(),
                createResourceIdlingResource = get(),
                updateResourceIdlingResource = get(),
                resourceUpdateActionsInteractorFactory = get(),
                checkPasswordPropertiesUseCase = mockCheckPasswordPropertiesUseCase,
                getMetadataTypesSettingsUseCase = mockGetMetadataTypesSettingsUseCase,
                editPermissionsConfirmationInteractor =
                    EditPermissionsConfirmationInteractor(
                        fetchResourcePermissionsUseCase = mockFetchResourcePermissionsUseCase,
                        getLocalResourcePermissionsUseCase = mockGetLocalResourcePermissionsUseCase,
                        getSelectedAccountDataUseCase = mockGetSelectedAccountDataUseCase,
                    ),
                createPermissionsConfirmationInteractor =
                    CreatePermissionsConfirmationInteractor(
                        fetchFolderPermissionsUseCase = mockFetchFolderPermissionsUseCase,
                        getLocalFolderPermissionsUseCase = mockGetLocalFolderPermissionsUseCase,
                        getSelectedAccountDataUseCase = mockGetSelectedAccountDataUseCase,
                    ),
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
