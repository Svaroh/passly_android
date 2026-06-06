package net.svaroh.passly.feature.authentication.auth.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.RemoveSelectedAccountUseCase
import net.svaroh.passly.core.authenticationcore.session.GetSessionUseCase
import net.svaroh.passly.core.idlingresource.SignOutIdlingResource
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.mappers.SignOutMapper
import net.svaroh.passly.passboltapi.auth.AuthRepository

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
class SignOutUseCase(
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val removeSelectedAccountUseCase: RemoveSelectedAccountUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val authRepository: AuthRepository,
    private val signOutMapper: SignOutMapper,
    private val getSessionUseCase: GetSessionUseCase,
    private val signOutIdlingResource: SignOutIdlingResource,
) : AsyncUseCase<Unit, Unit> {
    override suspend fun execute(input: Unit) {
        signOutIdlingResource.setIdle(false)
        getSessionUseCase.execute(Unit).refreshToken?.let {
            authRepository.signOut(signOutMapper.mapRequestToDto(it))
        }
        passphraseMemoryCache.clear()
        getSelectedAccountUseCase.execute(Unit).selectedAccount?.let { selectedAccount ->
            removeSelectedAccountUseCase.execute(UserIdInput(selectedAccount))
        }
        signOutIdlingResource.setIdle(true)
    }
}
