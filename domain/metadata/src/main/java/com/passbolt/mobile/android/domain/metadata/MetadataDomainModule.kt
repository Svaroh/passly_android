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

package net.svaroh.passly.domain.metadata

import net.svaroh.passly.domain.metadata.interactor.MetadataKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataKeysSettingsInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataSessionKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataTypesSettingsInteractor
import net.svaroh.passly.domain.metadata.interactor.ResourceAccessInteractor
import net.svaroh.passly.domain.metadata.privatekeys.MetadataPrivateKeysValidator
import net.svaroh.passly.domain.metadata.sessionkeys.SessionKeysBundleMerger
import net.svaroh.passly.domain.metadata.sessionkeys.SessionKeysBundleProcessor
import net.svaroh.passly.domain.metadata.sessionkeys.SessionKeysBundleValidator
import net.svaroh.passly.domain.metadata.sessionkeys.SessionKeysMemoryCache
import net.svaroh.passly.domain.metadata.usecase.CanCreateResourceUseCase
import net.svaroh.passly.domain.metadata.usecase.CanShareResourceUseCase
import net.svaroh.passly.domain.metadata.usecase.DeleteTrustedMetadataKeyUseCase
import net.svaroh.passly.domain.metadata.usecase.FetchMetadataKeysSettingsUseCase
import net.svaroh.passly.domain.metadata.usecase.FetchMetadataKeysUseCase
import net.svaroh.passly.domain.metadata.usecase.FetchMetadataSessionKeysUseCase
import net.svaroh.passly.domain.metadata.usecase.FetchMetadataTypesSettingsUseCase
import net.svaroh.passly.domain.metadata.usecase.GetMetadataKeysSettingsUseCase
import net.svaroh.passly.domain.metadata.usecase.GetMetadataTypesSettingsUseCase
import net.svaroh.passly.domain.metadata.usecase.GetTrustedMetadataKeyUseCase
import net.svaroh.passly.domain.metadata.usecase.PostMetadataSessionKeysUseCase
import net.svaroh.passly.domain.metadata.usecase.SaveMetadataKeysSettingsUseCase
import net.svaroh.passly.domain.metadata.usecase.SaveMetadataTypesSettingsUseCase
import net.svaroh.passly.domain.metadata.usecase.SaveTrustedMetadataKeyUseCase
import net.svaroh.passly.domain.metadata.usecase.UpdateMetadataPrivateKeyUseCase
import net.svaroh.passly.domain.metadata.usecase.UpdateMetadataSessionKeysUseCase
import net.svaroh.passly.domain.metadata.usecase.db.GetLocalMetadataKeyUseCase
import net.svaroh.passly.domain.metadata.usecase.db.GetLocalMetadataKeysUseCase
import net.svaroh.passly.domain.metadata.usecase.db.RebuildMetadataKeysTablesUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val metadataDomainModule =
    module {
        singleOf(::GetMetadataTypesSettingsUseCase)
        singleOf(::SaveMetadataTypesSettingsUseCase)
        singleOf(::GetMetadataKeysSettingsUseCase)
        singleOf(::SaveMetadataKeysSettingsUseCase)

        singleOf(::FetchMetadataKeysUseCase)
        singleOf(::GetLocalMetadataKeysUseCase)
        singleOf(::GetLocalMetadataKeyUseCase)
        singleOf(::RebuildMetadataKeysTablesUseCase)
        singleOf(::MetadataKeysInteractor)

        singleOf(::FetchMetadataTypesSettingsUseCase)
        singleOf(::MetadataTypesSettingsInteractor)
        singleOf(::FetchMetadataKeysSettingsUseCase)
        singleOf(::MetadataKeysSettingsInteractor)

        singleOf(::FetchMetadataSessionKeysUseCase)
        singleOf(::PostMetadataSessionKeysUseCase)
        singleOf(::UpdateMetadataSessionKeysUseCase)
        singleOf(::SessionKeysBundleMerger)
        singleOf(::SessionKeysMemoryCache)
        singleOf(::SessionKeysBundleValidator)
        singleOf(::SessionKeysBundleProcessor)
        singleOf(::MetadataSessionKeysInteractor)

        singleOf(::MetadataPrivateKeysValidator)
        singleOf(::UpdateMetadataPrivateKeyUseCase)
        singleOf(::GetTrustedMetadataKeyUseCase)
        singleOf(::SaveTrustedMetadataKeyUseCase)
        singleOf(::DeleteTrustedMetadataKeyUseCase)
        singleOf(::MetadataPrivateKeysInteractor)
        singleOf(::MetadataPrivateKeysHelperInteractor)

        singleOf(::CanCreateResourceUseCase)
        singleOf(::CanShareResourceUseCase)
        singleOf(::ResourceAccessInteractor)
    }
