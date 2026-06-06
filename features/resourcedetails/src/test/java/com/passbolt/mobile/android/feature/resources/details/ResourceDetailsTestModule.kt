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

package net.svaroh.passly.feature.resources.details

import com.google.gson.Gson
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import net.svaroh.passly.common.coroutinetimer.TimerFactory
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import net.svaroh.passly.core.idlingresource.ResourceDetailActionIdlingResource
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.otpcore.TotpParametersProvider
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.core.resources.actions.ResourceCommonActionsInteractor
import net.svaroh.passly.core.resources.actions.ResourcePropertiesActionsInteractor
import net.svaroh.passly.core.resources.actions.ResourcePropertyActionResult
import net.svaroh.passly.core.resources.actions.SecretPropertiesActionsInteractor
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceTagsUseCase
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsViewModel
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import net.svaroh.passly.jsonmodel.JSON_MODEL_GSON
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathJsonPathOps
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathsOps
import net.svaroh.passly.mappers.GroupsModelMapper
import net.svaroh.passly.mappers.OtpModelMapper
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.mappers.ResourceFormMapper
import net.svaroh.passly.mappers.UsersModelMapper
import net.svaroh.passly.metadata.usecase.CanShareResourceUseCase
import net.svaroh.passly.supportedresourceTypes.ContentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.koin.core.Koin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.EnumSet

@OptIn(ExperimentalCoroutinesApi::class)
internal val testModule =
    module {
        single { mock<GetFeatureFlagsUseCase>() }
        single { mock<GetLocalResourceUseCase>() }
        single { mock<GetLocalResourcePermissionsUseCase>() }
        single { mock<GetLocalResourceTagsUseCase>() }
        single { mock<GetLocalFolderLocationUseCase>() }
        single { mock<TotpParametersProvider>() }
        single { mock<GetRbacRulesUseCase>() }
        single { mock<ResourceTypeIdToSlugMappingProvider>() }
        single { mock<CanShareResourceUseCase>() }
        single { mock<ResourceDetailActionIdlingResource>() }
        single { mock<SecretPropertiesActionsInteractor>() }
        single { mock<ResourcePropertiesActionsInteractor>() }
        single { mock<ResourceCommonActionsInteractor>() }
        single { mock<GetSessionExpiryUseCase>() }
        single { mock<PassphraseMemoryCache>() }
        single { mock<TimerFactory>() }
        singleOf(::DataRefreshTrackingFlow)
        singleOf(::SessionRefreshTrackingFlow)
        factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
        factoryOf(::OtpModelMapper)
        factoryOf(::PermissionsModelMapper)
        factoryOf(::GroupsModelMapper)
        factoryOf(::UsersModelMapper)
        factoryOf(::ResourceFormMapper)
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
        factoryOf(::ResourceDetailsViewModel)
    }

internal fun Koin.setupDefaultMocks() {
    setupAuthenticationMocks()
    setupConfigurationMocks()
    setupResourceMocks()
    setupResourceActionsMocks()
    setupUtilsMocks()
}

private fun Koin.setupAuthenticationMocks() {
    val passphraseMemoryCache: PassphraseMemoryCache = get()
    whenever(passphraseMemoryCache.getSessionDurationSeconds()) doReturn 5 * 60

    val getSessionExpiryUseCase: GetSessionExpiryUseCase = get()
    whenever(getSessionExpiryUseCase.execute(Unit)) doReturn
        GetSessionExpiryUseCase.Output.JwtWillExpire(ZonedDateTime.now().plusMinutes(5))
}

private fun Koin.setupConfigurationMocks() {
    val getFeatureFlagsUseCase: GetFeatureFlagsUseCase = get()
    getFeatureFlagsUseCase.stub {
        onBlocking { execute(Unit) } doReturn GetFeatureFlagsUseCase.Output(DEFAULT_FEATURE_FLAGS)
    }

    val getRbacRulesUseCase: GetRbacRulesUseCase = get()
    getRbacRulesUseCase.stub {
        onBlocking { execute(Unit) } doReturn GetRbacRulesUseCase.Output(DEFAULT_RBAC)
    }
}

private fun Koin.setupResourceMocks() {
    val getLocalResourceUseCase: GetLocalResourceUseCase = get()
    getLocalResourceUseCase.stub {
        onBlocking { execute(any()) } doReturn GetLocalResourceUseCase.Output(DEFAULT_RESOURCE_MODEL)
    }

    val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase = get()
    getLocalResourcePermissionsUseCase.stub {
        onBlocking { execute(any()) } doReturn
            GetLocalResourcePermissionsUseCase.Output(listOf(GROUP_PERMISSION, USER_PERMISSION))
    }

    val getLocalResourceTagsUseCase: GetLocalResourceTagsUseCase = get()
    getLocalResourceTagsUseCase.stub {
        onBlocking { execute(any()) } doReturn GetLocalResourceTagsUseCase.Output(RESOURCE_TAGS)
    }

    val getLocalFolderLocationUseCase: GetLocalFolderLocationUseCase = get()
    getLocalFolderLocationUseCase.stub {
        onBlocking { execute(any()) } doReturn GetLocalFolderLocationUseCase.Output(emptyList())
    }

    val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider = get()
    resourceTypeIdToSlugMappingProvider.stub {
        onBlocking { provideMappingForSelectedAccount() } doReturn
            mapOf(RESOURCE_TYPE_ID to ContentType.PasswordAndDescription.slug)
    }

    val canShareResourceUseCase: CanShareResourceUseCase = get()
    canShareResourceUseCase.stub {
        onBlocking { execute(any()) } doReturn CanShareResourceUseCase.Output(canShareResource = true)
    }
}

private fun Koin.setupResourceActionsMocks() {
    val resourcePropertiesActionsInteractor: ResourcePropertiesActionsInteractor = get()
    resourcePropertiesActionsInteractor.stub {
        onBlocking { provideMainUri() } doReturn
            flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.URL_LABEL,
                    isSecret = false,
                    URL,
                ),
            )
        onBlocking { provideAdditionalUris() } doReturn
            flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.URL_LABEL,
                    isSecret = false,
                    listOf(""),
                ),
            )
        onBlocking { provideUsername() } doReturn
            flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.USERNAME_LABEL,
                    isSecret = false,
                    USERNAME,
                ),
            )
        onBlocking { provideMetadataDescription() } doReturn
            flowOf(
                ResourcePropertyActionResult(
                    ResourcePropertiesActionsInteractor.DESCRIPTION_LABEL,
                    isSecret = false,
                    DESCRIPTION,
                ),
            )
    }
}

private fun Koin.setupUtilsMocks() {
    val timerFactory: TimerFactory = get()
    timerFactory.stub {
        onBlocking { createInfiniteTimer(any()) } doReturn flowOf()
    }
}
