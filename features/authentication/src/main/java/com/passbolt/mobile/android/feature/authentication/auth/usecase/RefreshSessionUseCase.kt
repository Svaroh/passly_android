package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountDataUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.auth.AuthRepository
import com.passbolt.mobile.android.domain.auth.SessionRepository
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SessionRefreshLock.CompletedRefresh
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
class RefreshSessionUseCase(
    private val authRepository: AuthRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val sessionRepository: SessionRepository,
    private val sessionRefreshLock: SessionRefreshLock,
) : AsyncUseCase<Unit, RefreshSessionUseCase.Output> {
    override suspend fun execute(input: Unit): Output =
        sessionRefreshLock.refreshOrShareOutcome(
            currentUserId = { getSelectedAccountUseCase.execute(Unit).selectedAccount },
            refresh = { refreshSessionForSelectedAccount() },
        )

    private suspend fun refreshSessionForSelectedAccount(): CompletedRefresh =
        try {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val serverUserId = requireNotNull(getAccountDataUseCase.execute(UserIdInput(userId)).serverId)
            val singleUseRefreshToken = requireNotNull(sessionRepository.getSession(userId).refreshToken)

            Timber.d("[Session] Refreshing backend session")
            when (val result = authRepository.refreshSession(singleUseRefreshToken, serverUserId)) {
                is DomainResult.Finished -> {
                    with(sessionRepository) {
                        saveSession(
                            userId = userId,
                            accessToken = result.value.accessToken,
                            refreshToken = result.value.refreshToken,
                        )
                        result.value.mfaToken?.let { echoedMfaToken ->
                            saveMfaToken(
                                userId = userId,
                                mfaToken = echoedMfaToken,
                            )
                        }
                    }
                    Timber.d("[Session] Backend session refresh succeeded")
                    CompletedRefresh(userId, Output.Success)
                }
                is DomainResult.Incomplete -> {
                    Timber.d("[Session] Backend session refresh request did not succeed")
                    CompletedRefresh(userId, Output.Failure)
                }
            }
        } catch (throwable: Throwable) {
            Timber.e(throwable, "[Session] Backend session refresh attempt failed")
            CompletedRefresh(userId = null, output = Output.Failure)
        }

    sealed class Output {
        data object Success : Output()

        data object Failure : Output()
    }
}
