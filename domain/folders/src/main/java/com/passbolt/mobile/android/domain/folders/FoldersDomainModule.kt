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

package net.svaroh.passly.domain.folders

import net.svaroh.passly.domain.folders.usecase.AddLocalFolderPermissionsUseCase
import net.svaroh.passly.domain.folders.usecase.AddLocalFolderUseCase
import net.svaroh.passly.domain.folders.usecase.CreateFolderUseCase
import net.svaroh.passly.domain.folders.usecase.FetchFolderPermissionsUseCase
import net.svaroh.passly.domain.folders.usecase.FolderShareInteractor
import net.svaroh.passly.domain.folders.usecase.FoldersInteractor
import net.svaroh.passly.domain.folders.usecase.GetFoldersPaginatedUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalDirectChildFoldersPaginatedUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderDetailsUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderLocationUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalParentFolderPermissionsToApplyToNewItemUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalSubFoldersForFolderPaginatedUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalSubFoldersForFolderUseCase
import net.svaroh.passly.domain.folders.usecase.RemoveLocalFolderPermissionsUseCase
import net.svaroh.passly.domain.folders.usecase.RemoveLocalFoldersWithUpdateStateUseCase
import net.svaroh.passly.domain.folders.usecase.SetLocalFoldersUpdateStateUseCase
import net.svaroh.passly.domain.folders.usecase.UpdateLocalFoldersIsSharedUseCase
import net.svaroh.passly.domain.folders.usecase.UpsertLocalFoldersUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val foldersDomainModule =
    module {
        factoryOf(::CreateFolderUseCase)
        singleOf(::GetFoldersPaginatedUseCase)
        singleOf(::FetchFolderPermissionsUseCase)
        singleOf(::FoldersInteractor)
        singleOf(::RemoveLocalFolderPermissionsUseCase)
        singleOf(::AddLocalFolderPermissionsUseCase)
        singleOf(::FolderShareInteractor)
        singleOf(::SetLocalFoldersUpdateStateUseCase)
        singleOf(::UpsertLocalFoldersUseCase)
        singleOf(::RemoveLocalFoldersWithUpdateStateUseCase)
        singleOf(::GetLocalDirectChildFoldersPaginatedUseCase)
        singleOf(::GetLocalSubFoldersForFolderUseCase)
        singleOf(::GetLocalSubFoldersForFolderPaginatedUseCase)
        singleOf(::GetLocalFolderDetailsUseCase)
        singleOf(::GetLocalFolderLocationUseCase)
        singleOf(::GetLocalFolderPermissionsUseCase)
        singleOf(::GetLocalParentFolderPermissionsToApplyToNewItemUseCase)
        singleOf(::AddLocalFolderUseCase)
        singleOf(::UpdateLocalFoldersIsSharedUseCase)
    }
