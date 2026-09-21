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

package com.passbolt.mobile.android.data.preferences

import android.app.ActivityManager
import com.passbolt.mobile.android.data.preferences.datasource.local.AccountPreferencesLocalDataSourceImpl
import com.passbolt.mobile.android.data.preferences.datasource.local.DefaultFilterSerializer
import com.passbolt.mobile.android.data.preferences.datasource.local.GlobalPreferencesLocalDataSourceImpl
import com.passbolt.mobile.android.data.preferences.datasource.local.HomeDisplayViewSerializer
import com.passbolt.mobile.android.domain.preferences.AccountPreferencesLocalDataSource
import com.passbolt.mobile.android.domain.preferences.AccountPreferencesRepository
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesLocalDataSource
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesRepository
import com.passbolt.mobile.android.domain.preferences.pagesize.DevicePerformanceFingerprintProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val preferencesDataModule =
    module {
        singleOf(::HomeDisplayViewSerializer)
        singleOf(::DefaultFilterSerializer)
        singleOf(::GlobalPreferencesLocalDataSourceImpl) bind GlobalPreferencesLocalDataSource::class
        singleOf(::AccountPreferencesLocalDataSourceImpl) bind AccountPreferencesLocalDataSource::class
        singleOf(::GlobalPreferencesRepositoryImpl) bind GlobalPreferencesRepository::class
        singleOf(::AccountPreferencesRepositoryImpl) bind AccountPreferencesRepository::class
        factory { androidContext().getSystemService(ActivityManager::class.java) }
        singleOf(::DevicePerformanceFingerprintProviderImpl) bind DevicePerformanceFingerprintProvider::class
    }
