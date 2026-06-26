package net.svaroh.passly.domain.resources

import net.svaroh.passly.common.search.SearchableMatcher
import net.svaroh.passly.domain.resources.actions.ConfirmedRecipientsPublicKeysResolver
import net.svaroh.passly.domain.resources.actions.ResourceCommonActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourceCreateActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourcePropertiesActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourceShareActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourceUpdateActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourceUpdateActionsInteractorFactory
import net.svaroh.passly.domain.resources.actions.SecretPropertiesActionsInteractor
import net.svaroh.passly.domain.resources.actions.SecretPropertiesActionsInteractorFactory
import net.svaroh.passly.domain.resources.interactor.create.CreateResourceInteractor
import net.svaroh.passly.domain.resources.interactor.update.UpdateResourceInteractor
import net.svaroh.passly.domain.resources.resourceicon.BackgroundColorIconProvider
import net.svaroh.passly.domain.resources.resourceicon.ResourceIconProvider
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsConfirmationInteractor
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import net.svaroh.passly.domain.resources.usecase.DeleteResourceUseCase
import net.svaroh.passly.domain.resources.usecase.EditPermissionsConfirmationInteractor
import net.svaroh.passly.domain.resources.usecase.FetchResourcePermissionsUseCase
import net.svaroh.passly.domain.resources.usecase.GetResourcesPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.ResourceContentTypeProvider
import net.svaroh.passly.domain.resources.usecase.ResourceInteractor
import net.svaroh.passly.domain.resources.usecase.ResourceShareInteractor
import net.svaroh.passly.domain.resources.usecase.ShareResourceUseCase
import net.svaroh.passly.domain.resources.usecase.SimulateShareResourceUseCase
import net.svaroh.passly.domain.resources.usecase.db.RemoveLocalResourcesWithUpdateStateUseCase
import net.svaroh.passly.domain.resources.usecase.db.RemoveLocalUrisUseCase
import net.svaroh.passly.domain.resources.usecase.db.SetLocalResourcesUpdateStateUseCase
import net.svaroh.passly.domain.resources.usecase.db.UpsertLocalResourcesUseCase
import net.svaroh.passly.domain.resources.usecase.db.resourcesDbModule
import net.svaroh.passly.ui.ResourceUiModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

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
val resourcesDomainModule =
    module {
        resourcesDbModule()

        singleOf(::GetResourcesPaginatedUseCase)
        singleOf(::ResourceInteractor)
        singleOf(::SearchableMatcher)
        singleOf(::DeleteResourceUseCase)
        singleOf(::FetchResourcePermissionsUseCase)
        singleOf(::CreatePermissionsSnapshotInteractor)
        singleOf(::SimulateShareResourceUseCase)
        singleOf(::ShareResourceUseCase)
        singleOf(::ResourceContentTypeProvider)
        singleOf(::ResourceShareInteractor)
        singleOf(::ResourceShareActionsInteractor)
        singleOf(::EditPermissionsConfirmationInteractor)
        singleOf(::CreatePermissionsConfirmationInteractor)
        singleOf(::UpdateResourceInteractor)
        singleOf(::CreateResourceInteractor)
        factoryOf(::ResourceIconProvider)
        factoryOf(::BackgroundColorIconProvider)
        factoryOf(::ConfirmedRecipientsPublicKeysResolver)
        singleOf(::SetLocalResourcesUpdateStateUseCase)
        singleOf(::RemoveLocalUrisUseCase)
        singleOf(::RemoveLocalResourcesWithUpdateStateUseCase)
        singleOf(::UpsertLocalResourcesUseCase)

        factory { (resource: ResourceUiModel) ->
            ResourcePropertiesActionsInteractor(
                resource,
            )
        }
        factory { (resource: ResourceUiModel) ->
            ResourceCommonActionsInteractor(
                resource,
                favouritesInteractor = get(),
                deleteResourceUseCase = get(),
                updateLocalResourceUseCase = get(),
            )
        }
        factory { (resource: ResourceUiModel) ->
            SecretPropertiesActionsInteractor(
                resource,
                secretParser = get(),
                secretInteractor = get(),
            )
        }
        factory<SecretPropertiesActionsInteractorFactory> {
            SecretPropertiesActionsInteractorFactory { resource -> get { parametersOf(resource) } }
        }
        factory { (resource: ResourceUiModel) ->
            ResourceUpdateActionsInteractor(
                resource,
                secretPropertiesActionsInteractor = get { parametersOf(resource) },
                updateResourceInteractor = get(),
                resourceTypesUpdateGraph = get(),
                updateLocalResourceUseCase = get(),
                getLocalCurrentUserUseCase = get(),
                metadataPrivateKeysInteractor = get(),
                getLocalFolderPermissionsUseCase = get(),
                getLocalResourcePermissionsUseCase = get(),
                getMetadataKeysSettingsUseCase = get(),
                getMetadataKeysUseCase = get(),
                resourceTypeIdToSlugMappingProvider = get(),
                createPermissionsSnapshotInteractor = get(),
                getPermissionsSnapshotUseCase = get(),
                resourceShareInteractor = get(),
                confirmedRecipientsPublicKeysResolver = get(),
            )
        }
        factory<ResourceUpdateActionsInteractorFactory> {
            ResourceUpdateActionsInteractorFactory { resource -> get { parametersOf(resource) } }
        }
        factory {
            ResourceCreateActionsInteractor(
                createResourceInteractor = get(),
                addLocalResourceUseCase = get(),
                addLocalResourcePermissionsUseCase = get(),
                resourceShareInteractor = get(),
                getLocalParentFolderPermissionsToApplyUseCase = get(),
                getLocalFolderPermissionsUseCase = get(),
                getMetadataKeysSettingsUseCase = get(),
                getMetadataTypesSettingsUseCase = get(),
                getMetadataKeysUseCase = get(),
                getLocalCurrentUserUseCase = get(),
                metadataPrivateKeysInteractor = get(),
                resourceTypeIdToSlugMappingProvider = get(),
                createPermissionsSnapshotInteractor = get(),
                confirmedRecipientsPublicKeysResolver = get(),
            )
        }
    }
