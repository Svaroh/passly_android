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

package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.idlingresource.SignOutIdlingResource
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.RemoveSelectedAccountUseCase
import com.passbolt.mobile.android.domain.auth.AuthRepository
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.RemovePermissionsSnapshotUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class SignOutUseCaseTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<PassphraseMemoryCache>() }
                        single { mock<RemovePermissionsSnapshotUseCase>() }
                        single { mock<RemoveSelectedAccountUseCase>() }
                        single { mock<GetSelectedAccountUseCase>() }
                        single { mock<AuthRepository>() }
                        single { mock<GetSessionUseCase>() }
                        singleOf(::SignOutIdlingResource)
                        single { RuntimeAuthenticatedFlag() }
                        factoryOf(::SignOutUseCase)
                    },
                ),
            )
        }

    private lateinit var getSessionUseCase: GetSessionUseCase
    private lateinit var getSelectedAccountUseCase: GetSelectedAccountUseCase
    private lateinit var authRepository: AuthRepository
    private lateinit var runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag
    private lateinit var useCase: SignOutUseCase

    @Before
    fun setUp() {
        getSessionUseCase = get()
        getSelectedAccountUseCase = get()
        authRepository = get()
        runtimeAuthenticatedFlag = get()
        useCase = get()
    }

    @Test
    fun `sign out should reset runtime authenticated flag`() =
        runTest {
            runtimeAuthenticatedFlag.isAuthenticated = true
            getSessionUseCase.stub {
                on { execute(Unit) }.thenReturn(GetSessionUseCase.Output(ACCESS_TOKEN, REFRESH_TOKEN, mfaToken = null))
            }
            getSelectedAccountUseCase.stub {
                on { execute(Unit) }.thenReturn(GetSelectedAccountUseCase.Output(USER_ID))
            }
            authRepository.stub {
                onBlocking { signOut(REFRESH_TOKEN) }.thenReturn(DomainResult.Finished(Unit))
            }

            useCase.execute(Unit)

            assertThat(runtimeAuthenticatedFlag.isAuthenticated).isFalse()
        }

    @Test
    fun `sign out should reset runtime authenticated flag when there is no session and no selected account`() =
        runTest {
            runtimeAuthenticatedFlag.isAuthenticated = true
            getSessionUseCase.stub {
                on { execute(Unit) }.thenReturn(GetSessionUseCase.Output(null, null, null))
            }
            getSelectedAccountUseCase.stub {
                on { execute(Unit) }.thenReturn(GetSelectedAccountUseCase.Output(null))
            }

            useCase.execute(Unit)

            assertThat(runtimeAuthenticatedFlag.isAuthenticated).isFalse()
        }

    private companion object {
        private const val USER_ID = "user-id"
        private const val ACCESS_TOKEN = "access-token"
        private const val REFRESH_TOKEN = "refresh-token"
    }
}
