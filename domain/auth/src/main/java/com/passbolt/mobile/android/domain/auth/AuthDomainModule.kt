package com.passbolt.mobile.android.domain.auth

import com.passbolt.mobile.android.domain.auth.usecase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.FetchServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.GetPassphraseUseCase
import com.passbolt.mobile.android.domain.auth.usecase.GetResourcesDatabasePassphraseUseCase
import com.passbolt.mobile.android.domain.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import com.passbolt.mobile.android.domain.auth.usecase.RemoveAllAccountsPassphrasesUseCase
import com.passbolt.mobile.android.domain.auth.usecase.RemovePassphraseUseCase
import com.passbolt.mobile.android.domain.auth.usecase.RemoveServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.SaveMfaTokenUseCase
import com.passbolt.mobile.android.domain.auth.usecase.SavePassphraseUseCase
import com.passbolt.mobile.android.domain.auth.usecase.SaveResourcesDatabasePassphraseUseCase
import com.passbolt.mobile.android.domain.auth.usecase.SaveServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.domain.auth.usecase.SaveSessionUseCase
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
val authDomainModule =
    module {
        singleOf(::FetchServerPublicPgpKeyUseCase)
        singleOf(::FetchServerPublicRsaKeyUseCase)
        singleOf(::GetServerPublicRsaKeyUseCase)
        singleOf(::SaveServerPublicRsaKeyUseCase)
        singleOf(::RemoveServerPublicRsaKeyUseCase)
        singleOf(::GetSessionUseCase)
        singleOf(::SaveSessionUseCase)
        singleOf(::SaveMfaTokenUseCase)
        singleOf(::GetPassphraseUseCase)
        singleOf(::SavePassphraseUseCase)
        singleOf(::RemovePassphraseUseCase)
        singleOf(::RemoveAllAccountsPassphrasesUseCase)
        singleOf(::CheckIfPassphraseFileExistsUseCase)
        singleOf(::GetResourcesDatabasePassphraseUseCase)
        singleOf(::SaveResourcesDatabasePassphraseUseCase)
    }
