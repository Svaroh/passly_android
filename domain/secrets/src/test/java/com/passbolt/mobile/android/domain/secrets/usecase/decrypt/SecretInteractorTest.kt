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

package net.svaroh.passly.domain.secrets.usecase.decrypt

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.gopenpgp.exception.OpenPgpError
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import kotlin.test.assertIs

class SecretInteractorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<FetchSecretUseCase>() }
                        single { mock<DecryptSecretUseCase>() }
                        factoryOf(::SecretInteractor)
                    },
                ),
            )
        }

    private lateinit var fetchSecretUseCase: FetchSecretUseCase
    private lateinit var decryptSecretUseCase: DecryptSecretUseCase
    private lateinit var interactor: SecretInteractor

    @Before
    fun setUp() {
        fetchSecretUseCase = get()
        decryptSecretUseCase = get()
        interactor = get()
    }

    @Test
    fun `fetch and decrypt success returns Success and stays authenticated`() =
        runTest {
            stubFetch(FetchSecretUseCase.Output.EncryptedSecret(ENCRYPTED_SECRET))
            stubDecrypt(DecryptSecretUseCase.Output.DecryptedSecret(DECRYPTED_SECRET))

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.Success(DECRYPTED_SECRET))
            assertThat(output.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `decrypt failure returns DecryptFailure and stays authenticated`() =
        runTest {
            val error = OpenPgpError("decrypt boom")
            stubFetch(FetchSecretUseCase.Output.EncryptedSecret(ENCRYPTED_SECRET))
            stubDecrypt(DecryptSecretUseCase.Output.Failure(error))

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.DecryptFailure(error))
            assertThat(output.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `decrypt passphrase-missing returns Unauthorized passphrase`() =
        runTest {
            val reason = AuthenticationState.Unauthenticated.Reason.Passphrase
            stubFetch(FetchSecretUseCase.Output.EncryptedSecret(ENCRYPTED_SECRET))
            stubDecrypt(DecryptSecretUseCase.Output.Unauthorized(reason))

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.Unauthorized(reason))
            val state = output.authenticationState
            assertIs<AuthenticationState.Unauthenticated>(state)
            assertThat(state.reason).isEqualTo(AuthenticationState.Unauthenticated.Reason.Passphrase)
        }

    @Test
    fun `fetch unauthorized surfaces as session re-auth`() =
        runTest {
            val failure = DomainResult.Incomplete.Unauthorized
            stubFetch(FetchSecretUseCase.Output.Failure(failure))

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.FetchFailure(failure))
            val state = output.authenticationState
            assertIs<AuthenticationState.Unauthenticated>(state)
            assertThat(state.reason).isEqualTo(AuthenticationState.Unauthenticated.Reason.Session)
        }

    @Test
    fun `fetch mfa-required surfaces as mfa re-auth`() =
        runTest {
            val providers = emptyList<AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider?>()
            val failure = DomainResult.Incomplete.MfaRequired(providers)
            stubFetch(FetchSecretUseCase.Output.Failure(failure))

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.FetchFailure(failure))
            val state = output.authenticationState
            assertIs<AuthenticationState.Unauthenticated>(state)
            assertThat(state.reason).isEqualTo(AuthenticationState.Unauthenticated.Reason.Mfa(providers))
        }

    @Test
    fun `fetch generic error stays authenticated`() =
        runTest {
            val failure = DomainResult.Incomplete.Error(UNKNOWN, "boom")
            stubFetch(FetchSecretUseCase.Output.Failure(failure))

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.FetchFailure(failure))
            assertThat(output.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    private fun stubFetch(output: FetchSecretUseCase.Output) {
        fetchSecretUseCase.stub {
            on { execute(any()) }.thenReturn(output)
        }
    }

    private fun stubDecrypt(output: DecryptSecretUseCase.Output) {
        decryptSecretUseCase.stub {
            on { execute(any()) }.thenReturn(output)
        }
    }

    private companion object {
        const val RESOURCE_ID = "resource-id"
        const val ENCRYPTED_SECRET = "encrypted-secret"
        const val DECRYPTED_SECRET = "decrypted-secret"
    }
}
