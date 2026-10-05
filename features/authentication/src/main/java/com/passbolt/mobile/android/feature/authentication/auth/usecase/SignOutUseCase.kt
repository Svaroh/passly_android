package net.svaroh.passly.feature.authentication.auth.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.RemoveSelectedAccountUseCase
import net.svaroh.passly.core.authenticationcore.session.GetSessionUseCase
import net.svaroh.passly.core.idlingresource.SignOutIdlingResource
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.mappers.SignOutMapper
import net.svaroh.passly.passboltapi.auth.AuthRepository
import timber.log.Timber

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
/**
 * Signs the user out of this device, and tells the server about it if it can be reached.
 *
 * Signing out is a local act: what makes it real is the passphrase leaving memory and the account no longer being
 * selected. Telling the server to drop the refresh token is a courtesy that keeps a stolen token from outliving the
 * session, so it is attempted, but never waited for - a server that is down must not keep someone signed in, nor make
 * them watch a spinner for a screen that has already done its job.
 */
class SignOutUseCase(
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val removeSelectedAccountUseCase: RemoveSelectedAccountUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val authRepository: AuthRepository,
    private val signOutMapper: SignOutMapper,
    private val getSessionUseCase: GetSessionUseCase,
    private val signOutIdlingResource: SignOutIdlingResource,
    coroutineLaunchContext: CoroutineLaunchContext,
) : AsyncUseCase<Unit, Unit> {
    private val revokeScope = CoroutineScope(SupervisorJob() + coroutineLaunchContext.io)

    override suspend fun execute(input: Unit) {
        signOutIdlingResource.setIdle(false)
        val refreshToken = getSessionUseCase.execute(Unit).refreshToken

        passphraseMemoryCache.clear()
        getSelectedAccountUseCase.execute(Unit).selectedAccount?.let { selectedAccount ->
            removeSelectedAccountUseCase.execute(UserIdInput(selectedAccount))
        }
        signOutIdlingResource.setIdle(true)

        refreshToken?.let { token ->
            revokeScope.launch {
                try {
                    authRepository.signOut(signOutMapper.mapRequestToDto(token))
                } catch (exception: Exception) {
                    Timber.d(exception, "Could not tell the server about the sign out")
                }
            }
        }
    }
}
