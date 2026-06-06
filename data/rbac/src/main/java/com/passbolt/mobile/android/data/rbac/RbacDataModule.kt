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

package net.svaroh.passly.data.rbac

import net.svaroh.passly.core.networking.RestService
import net.svaroh.passly.data.rbac.datasource.local.RbacLocalDataSourceImpl
import net.svaroh.passly.data.rbac.datasource.remote.RbacRemoteDataSourceImpl
import net.svaroh.passly.data.rbac.datasource.remote.api.RbacApi
import net.svaroh.passly.domain.rbac.RbacLocalDataSource
import net.svaroh.passly.domain.rbac.RbacRemoteDataSource
import net.svaroh.passly.domain.rbac.RbacRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val rbacDataModule =
    module {
        single { get<RestService>().service(RbacApi::class.java) }

        singleOf(::RbacLocalDataSourceImpl) bind RbacLocalDataSource::class
        singleOf(::RbacRemoteDataSourceImpl) bind RbacRemoteDataSource::class
        singleOf(::RbacRepositoryImpl) bind RbacRepository::class
    }
