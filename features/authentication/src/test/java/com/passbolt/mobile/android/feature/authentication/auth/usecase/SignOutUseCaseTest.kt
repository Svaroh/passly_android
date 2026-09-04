package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.idlingresource.SignOutIdlingResource
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.RemoveSelectedAccountUseCase
import com.passbolt.mobile.android.domain.auth.AuthRepository
import com.passbolt.mobile.android.domain.auth.SessionRepository
import com.passbolt.mobile.android.domain.auth.model.ServerSignOutStatus
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.RemovePermissionsSnapshotUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
class SignOutUseCaseTest {
    private val passphraseMemoryCache = mock<PassphraseMemoryCache>()
    private val removePermissionsSnapshotUseCase = mock<RemovePermissionsSnapshotUseCase>()
    private val removeSelectedAccountUseCase = mock<RemoveSelectedAccountUseCase>()
    private val getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
    private val authRepository = mock<AuthRepository>()
    private val getSessionUseCase = mock<GetSessionUseCase>()
    private val sessionRepository = mock<SessionRepository>()
    private val signOutIdlingResource = mock<SignOutIdlingResource>()
    private val runtimeAuthenticatedFlag = RuntimeAuthenticatedFlag()

    private val useCase =
        SignOutUseCase(
            passphraseMemoryCache = passphraseMemoryCache,
            removePermissionsSnapshotUseCase = removePermissionsSnapshotUseCase,
            removeSelectedAccountUseCase = removeSelectedAccountUseCase,
            getSelectedAccountUseCase = getSelectedAccountUseCase,
            authRepository = authRepository,
            getSessionUseCase = getSessionUseCase,
            sessionRepository = sessionRepository,
            signOutIdlingResource = signOutIdlingResource,
            runtimeAuthenticatedFlag = runtimeAuthenticatedFlag,
        )

    @Test
    fun `should return SIGNED_OUT and clear local state when server sign out finishes`() =
        runTest {
            whenever(getSessionUseCase.execute(Unit)) doReturn GetSessionUseCase.Output(null, REFRESH_TOKEN, null)
            whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(USER_ID)
            whenever(authRepository.signOut(REFRESH_TOKEN)) doReturn DomainResult.Finished(Unit)

            val output = useCase.execute(Unit)

            assertThat(output.serverSignOutStatus).isEqualTo(ServerSignOutStatus.SIGNED_OUT)
            verify(passphraseMemoryCache).clear()
            verify(sessionRepository).removeSession(USER_ID)
            verify(removePermissionsSnapshotUseCase).execute(UserIdInput(USER_ID))
            verify(removeSelectedAccountUseCase).execute(Unit)
        }

    @Test
    fun `should return SIGN_OUT_FAILED and still clear local state when server sign out is incomplete`() =
        runTest {
            whenever(getSessionUseCase.execute(Unit)) doReturn GetSessionUseCase.Output(null, REFRESH_TOKEN, null)
            whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(USER_ID)
            whenever(authRepository.signOut(REFRESH_TOKEN)) doReturn DomainResult.Incomplete.Unauthorized

            val output = useCase.execute(Unit)

            assertThat(output.serverSignOutStatus).isEqualTo(ServerSignOutStatus.SIGN_OUT_FAILED)
            verify(passphraseMemoryCache).clear()
            verify(sessionRepository).removeSession(USER_ID)
            verify(removeSelectedAccountUseCase).execute(Unit)
        }

    @Test
    fun `should return NO_ACTIVE_SESSION and skip server request when refresh token is missing`() =
        runTest {
            whenever(getSessionUseCase.execute(Unit)) doReturn GetSessionUseCase.Output(null, null, null)
            whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(USER_ID)

            val output = useCase.execute(Unit)

            assertThat(output.serverSignOutStatus).isEqualTo(ServerSignOutStatus.NO_ACTIVE_SESSION)
            verify(authRepository, never()).signOut(any())
            verify(sessionRepository).removeSession(USER_ID)
            verify(removeSelectedAccountUseCase).execute(Unit)
        }

    @Test
    fun `should skip local cleanup when there is no selected account`() =
        runTest {
            whenever(getSessionUseCase.execute(Unit)) doReturn GetSessionUseCase.Output(null, REFRESH_TOKEN, null)
            whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(null)
            whenever(authRepository.signOut(REFRESH_TOKEN)) doReturn DomainResult.Finished(Unit)

            val output = useCase.execute(Unit)

            assertThat(output.serverSignOutStatus).isEqualTo(ServerSignOutStatus.SIGNED_OUT)
            verify(passphraseMemoryCache).clear()
            verify(sessionRepository, never()).removeSession(any())
            verify(removeSelectedAccountUseCase, never()).execute(Unit)
        }

    @Test
    fun `should reset runtime authenticated flag on sign out`() =
        runTest {
            runtimeAuthenticatedFlag.isAuthenticated = true
            whenever(getSessionUseCase.execute(Unit)) doReturn GetSessionUseCase.Output(null, REFRESH_TOKEN, null)
            whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(USER_ID)
            whenever(authRepository.signOut(REFRESH_TOKEN)) doReturn DomainResult.Finished(Unit)

            useCase.execute(Unit)

            assertThat(runtimeAuthenticatedFlag.isAuthenticated).isFalse()
        }

    @Test
    fun `should reset runtime authenticated flag when there is no session and no selected account`() =
        runTest {
            runtimeAuthenticatedFlag.isAuthenticated = true
            whenever(getSessionUseCase.execute(Unit)) doReturn GetSessionUseCase.Output(null, null, null)
            whenever(getSelectedAccountUseCase.execute(Unit)) doReturn GetSelectedAccountUseCase.Output(null)

            useCase.execute(Unit)

            assertThat(runtimeAuthenticatedFlag.isAuthenticated).isFalse()
        }

    private companion object {
        private const val USER_ID = "user-id"
        private const val REFRESH_TOKEN = "refresh-token"
    }
}
