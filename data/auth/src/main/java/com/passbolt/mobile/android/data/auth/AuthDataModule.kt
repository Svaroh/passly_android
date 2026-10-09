package net.svaroh.passly.data.auth

import net.svaroh.passly.core.networking.RestService
import net.svaroh.passly.data.auth.datasource.local.AuthLocalDataSourceImpl
import net.svaroh.passly.data.auth.datasource.local.DatabasePassphraseLocalDataSourceImpl
import net.svaroh.passly.data.auth.datasource.local.PassphraseLocalDataSourceImpl
import net.svaroh.passly.data.auth.datasource.local.SessionLocalDataSourceImpl
import net.svaroh.passly.data.auth.datasource.remote.AuthRemoteDataSourceImpl
import net.svaroh.passly.data.auth.datasource.remote.api.AuthApi
import net.svaroh.passly.domain.auth.AuthRepository
import net.svaroh.passly.domain.auth.DatabasePassphraseRepository
import net.svaroh.passly.domain.auth.PassphraseRepository
import net.svaroh.passly.domain.auth.SessionRepository
import net.svaroh.passly.domain.auth.datasource.AuthLocalDataSource
import net.svaroh.passly.domain.auth.datasource.AuthRemoteDataSource
import net.svaroh.passly.domain.auth.datasource.DatabasePassphraseLocalDataSource
import net.svaroh.passly.domain.auth.datasource.PassphraseLocalDataSource
import net.svaroh.passly.domain.auth.datasource.SessionLocalDataSource
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
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
val authDataModule =
    module {
        single { get<RestService>().service(AuthApi::class.java) }

        singleOf(::AuthRemoteDataSourceImpl) bind AuthRemoteDataSource::class
        singleOf(::AuthLocalDataSourceImpl) bind AuthLocalDataSource::class
        singleOf(::AuthRepositoryImpl) bind AuthRepository::class
        singleOf(::SessionLocalDataSourceImpl) bind SessionLocalDataSource::class
        singleOf(::SessionRepositoryImpl) bind SessionRepository::class
        single<PassphraseLocalDataSource> {
            PassphraseLocalDataSourceImpl(
                biometricCrypto = get(),
                appContext = androidApplication(),
            )
        }
        singleOf(::PassphraseRepositoryImpl) bind PassphraseRepository::class
        singleOf(::DatabasePassphraseLocalDataSourceImpl) bind DatabasePassphraseLocalDataSource::class
        singleOf(::DatabasePassphraseRepositoryImpl) bind DatabasePassphraseRepository::class
    }
