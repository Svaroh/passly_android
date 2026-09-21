package com.passbolt.mobile.android.domain.accounts

import com.passbolt.mobile.android.domain.accounts.usecase.AccountsInteractor
import com.passbolt.mobile.android.domain.accounts.usecase.CheckAccountExistsUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountsUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetAllAccountsDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetCurrentApiUrlUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.IsServerFingerprintCorrectUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.RemoveAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.RemoveAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.RemoveSelectedAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.SaveAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.SaveServerFingerprintUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.UpdateAccountDataUseCase
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
val accountsDomainModule =
    module {
        singleOf(::GetAccountsUseCase)
        singleOf(::SaveAccountUseCase)
        singleOf(::RemoveAccountUseCase)
        singleOf(::GetAccountDataUseCase)
        singleOf(::UpdateAccountDataUseCase)
        singleOf(::RemoveAccountDataUseCase)
        singleOf(::SaveServerFingerprintUseCase)
        singleOf(::IsServerFingerprintCorrectUseCase)
        singleOf(::AuthenticatedAccountFlow)
        singleOf(::GetSelectedAccountUseCase)
        singleOf(::SaveSelectedAccountUseCase)
        singleOf(::RemoveSelectedAccountUseCase)
        singleOf(::GetCurrentApiUrlUseCase)
        singleOf(::SaveCurrentApiUrlUseCase)
        singleOf(::GetSelectedAccountDataUseCase)
        singleOf(::GetAllAccountsDataUseCase)
        singleOf(::CheckAccountExistsUseCase)
        singleOf(::AccountsInteractor)
    }
