package net.svaroh.passly.metadata

import net.svaroh.passly.metadata.interactor.MetadataKeysInteractor
import net.svaroh.passly.metadata.interactor.MetadataKeysSettingsInteractor
import net.svaroh.passly.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.metadata.interactor.MetadataPrivateKeysInteractor
import net.svaroh.passly.metadata.interactor.MetadataSessionKeysInteractor
import net.svaroh.passly.metadata.interactor.MetadataTypesSettingsInteractor
import net.svaroh.passly.metadata.privatekeys.MetadataPrivateKeysValidator
import net.svaroh.passly.metadata.sessionkeys.SessionKeysBundleMerger
import net.svaroh.passly.metadata.sessionkeys.SessionKeysBundleProcessor
import net.svaroh.passly.metadata.sessionkeys.SessionKeysBundleValidator
import net.svaroh.passly.metadata.sessionkeys.SessionKeysMemoryCache
import net.svaroh.passly.metadata.usecase.CanCreateResourceUseCase
import net.svaroh.passly.metadata.usecase.CanShareResourceUseCase
import net.svaroh.passly.metadata.usecase.DeleteTrustedMetadataKeyUseCase
import net.svaroh.passly.metadata.usecase.FetchMetadataKeysSettingsUseCase
import net.svaroh.passly.metadata.usecase.FetchMetadataKeysUseCase
import net.svaroh.passly.metadata.usecase.FetchMetadataSessionKeysUseCase
import net.svaroh.passly.metadata.usecase.FetchMetadataTypesSettingsUseCase
import net.svaroh.passly.metadata.usecase.GetTrustedMetadataKeyUseCase
import net.svaroh.passly.metadata.usecase.PostMetadataSessionKeysUseCase
import net.svaroh.passly.metadata.usecase.SaveTrustedMetadataKeyUseCase
import net.svaroh.passly.metadata.usecase.UpdateMetadataPrivateKeyUseCase
import net.svaroh.passly.metadata.usecase.UpdateMetadataSessionKeysUseCase
import net.svaroh.passly.metadata.usecase.db.AddLocalMetadataKeysUseCase
import net.svaroh.passly.metadata.usecase.db.GetLocalMetadataKeyUseCase
import net.svaroh.passly.metadata.usecase.db.GetLocalMetadataKeysUseCase
import net.svaroh.passly.metadata.usecase.db.RebuildMetadataKeysTablesUseCase
import net.svaroh.passly.metadata.usecase.db.RemoveLocalMetadataKeysUseCase
import net.svaroh.passly.metadata.usecase.metadataSettingsModule
import org.koin.core.module.dsl.singleOf
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

val metadataModule =
    module {
        metadataSettingsModule()

        singleOf(::FetchMetadataKeysUseCase)
        singleOf(::MetadataKeysInteractor)
        singleOf(::AddLocalMetadataKeysUseCase)
        singleOf(::GetLocalMetadataKeysUseCase)
        singleOf(::GetLocalMetadataKeyUseCase)
        singleOf(::RebuildMetadataKeysTablesUseCase)
        singleOf(::RemoveLocalMetadataKeysUseCase)
        singleOf(::FetchMetadataTypesSettingsUseCase)
        singleOf(::MetadataTypesSettingsInteractor)
        singleOf(::FetchMetadataKeysUseCase)
        singleOf(::FetchMetadataKeysSettingsUseCase)
        singleOf(::MetadataKeysSettingsInteractor)
        singleOf(::FetchMetadataSessionKeysUseCase)
        singleOf(::SessionKeysBundleMerger)
        singleOf(::SessionKeysMemoryCache)
        singleOf(::SessionKeysBundleValidator)
        singleOf(::SessionKeysBundleProcessor)
        singleOf(::MetadataPrivateKeysValidator)
        singleOf(::PostMetadataSessionKeysUseCase)
        singleOf(::UpdateMetadataSessionKeysUseCase)
        single {
            MetadataSessionKeysInteractor(
                fetchMetadataSessionKeysUseCase = get(),
                postMetadataSessionKeysUseCase = get(),
                updateMetadataSessionKeysUseCase = get(),
                passphraseMemoryCache = get(),
                getPrivateKeyUseCase = get(),
                openPgp = get(),
                sessionKeysBundleMerger = get(),
                sessionKeysMemoryCache = get(),
                metadataMapper = get(),
                gson = get(),
                sessionKeysBundleValidator = get(),
                sessionKeysBundleProcessor = get(),
            )
        }
        singleOf(::UpdateMetadataPrivateKeyUseCase)
        singleOf(::GetTrustedMetadataKeyUseCase)
        singleOf(::SaveTrustedMetadataKeyUseCase)
        singleOf(::DeleteTrustedMetadataKeyUseCase)
        singleOf(::MetadataPrivateKeysInteractor)
        singleOf(::MetadataPrivateKeysHelperInteractor)
        singleOf(::CanCreateResourceUseCase)
        singleOf(::CanShareResourceUseCase)
    }
