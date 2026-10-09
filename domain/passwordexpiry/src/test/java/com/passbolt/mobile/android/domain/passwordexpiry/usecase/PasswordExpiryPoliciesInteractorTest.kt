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

package net.svaroh.passly.domain.passwordexpiry.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.passwordexpiry.PasswordExpiryRepository
import net.svaroh.passly.domain.passwordexpiry.model.PasswordExpirySettings
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

class PasswordExpiryPoliciesInteractorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<PasswordExpiryRepository>() }
                        single { mock<GetSelectedAccountUseCase>() }
                        factoryOf(::PasswordExpiryPoliciesInteractor)
                    },
                ),
            )
        }

    private lateinit var repository: PasswordExpiryRepository
    private lateinit var getSelectedAccountUseCase: GetSelectedAccountUseCase
    private lateinit var interactor: PasswordExpiryPoliciesInteractor

    @Before
    fun setUp() {
        repository = get()
        getSelectedAccountUseCase = get()
        interactor = get()
        whenever(getSelectedAccountUseCase.execute(Unit))
            .thenReturn(GetSelectedAccountUseCase.Output(USER_ID))
    }

    @Test
    fun `success returns Success with settings`() =
        runTest {
            val settings = PasswordExpirySettings.defaults()
            repository.stub {
                on { getPasswordExpirySettings(USER_ID) }.thenReturn(DomainResult.Finished(settings))
            }

            val output = interactor.fetchAndSavePasswordExpiryPolicies()

            assertThat(output).isEqualTo(PasswordExpiryPoliciesInteractor.Output.Success(settings))
            assertThat(output.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `unauthorized failure surfaces as session re-auth`() =
        runTest {
            val failure = DomainResult.Incomplete.Unauthorized
            repository.stub {
                on { getPasswordExpirySettings(USER_ID) }.thenReturn(failure)
            }

            val output = interactor.fetchAndSavePasswordExpiryPolicies()

            assertThat(output).isEqualTo(PasswordExpiryPoliciesInteractor.Output.Failure.FetchFailure(failure))
            val state = output.authenticationState
            assertIs<AuthenticationState.Unauthenticated>(state)
            assertThat(state.reason).isEqualTo(AuthenticationState.Unauthenticated.Reason.Session)
        }

    @Test
    fun `mfa-required failure surfaces with mfa providers`() =
        runTest {
            val providers = emptyList<AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider?>()
            val failure = DomainResult.Incomplete.MfaRequired(providers)
            repository.stub {
                on { getPasswordExpirySettings(USER_ID) }.thenReturn(failure)
            }

            val output = interactor.fetchAndSavePasswordExpiryPolicies()

            assertThat(output).isEqualTo(PasswordExpiryPoliciesInteractor.Output.Failure.FetchFailure(failure))
            val state = output.authenticationState
            assertIs<AuthenticationState.Unauthenticated>(state)
            assertThat(state.reason).isEqualTo(AuthenticationState.Unauthenticated.Reason.Mfa(providers))
        }

    @Test
    fun `unknown failure stays authenticated`() =
        runTest {
            val failure = DomainResult.Incomplete.Error(UNKNOWN, "boom")
            repository.stub {
                on { getPasswordExpirySettings(USER_ID) }.thenReturn(failure)
            }

            val output = interactor.fetchAndSavePasswordExpiryPolicies()

            assertThat(output).isEqualTo(PasswordExpiryPoliciesInteractor.Output.Failure.FetchFailure(failure))
            assertThat(output.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `notcached failure stays authenticated`() =
        runTest {
            val failure = DomainResult.Incomplete.NotCached
            repository.stub {
                on { getPasswordExpirySettings(USER_ID) }.thenReturn(failure)
            }

            val output = interactor.fetchAndSavePasswordExpiryPolicies()

            assertThat(output).isEqualTo(PasswordExpiryPoliciesInteractor.Output.Failure.FetchFailure(failure))
            assertThat(output.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    private companion object {
        const val USER_ID = "user-id"
    }
}
