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

package net.svaroh.passly.feature.authentication.auth.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.domain.accounts.usecase.GetAccountDataUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.auth.AuthRepository
import net.svaroh.passly.domain.auth.SessionRepository
import net.svaroh.passly.domain.auth.model.RefreshedSession
import net.svaroh.passly.domain.auth.model.Session
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

class RefreshSessionUseCaseTest : KoinTest {
    private val mockAuthRepository = mock<AuthRepository>()
    private val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
    private val mockGetAccountDataUseCase = mock<GetAccountDataUseCase>()
    private val mockSessionRepository = mock<SessionRepository>()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mockAuthRepository }
                    single { mockGetSelectedAccountUseCase }
                    single { mockGetAccountDataUseCase }
                    single { mockSessionRepository }
                    singleOf(::RefreshSessionUseCase)
                    singleOf(::SessionRefreshLock)
                },
            )
        }

    @Before
    fun setUp() {
        whenever(mockGetSelectedAccountUseCase.execute(Unit))
            .thenReturn(GetSelectedAccountUseCase.Output(USER_ID))
        whenever(mockGetAccountDataUseCase.execute(UserIdInput(USER_ID))).thenReturn(
            GetAccountDataUseCase.Output(
                firstName = null,
                lastName = null,
                email = null,
                avatarUrl = null,
                url = "https://passbolt.example.com",
                serverId = SERVER_USER_ID,
                label = null,
                role = null,
            ),
        )
        whenever(mockSessionRepository.getSession(USER_ID)).thenReturn(
            Session(accessToken = "accessToken", refreshToken = CURRENT_REFRESH_TOKEN, mfaToken = null),
        )
    }

    @Test
    fun `concurrent refresh requests should share a single refresh`() =
        runTest {
            mockAuthRepository.stub {
                on { refreshSession(CURRENT_REFRESH_TOKEN, SERVER_USER_ID) } doSuspendableAnswer {
                    delay(1.seconds)
                    DomainResult.Finished(RefreshedSession(ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN, mfaToken = null))
                }
            }
            val useCase = get<RefreshSessionUseCase>()

            val results =
                listOf(
                    async { useCase.execute(Unit) },
                    async { useCase.execute(Unit) },
                    async { useCase.execute(Unit) },
                ).awaitAll()

            assertThat(results).containsExactly(
                RefreshSessionUseCase.Output.Success,
                RefreshSessionUseCase.Output.Success,
                RefreshSessionUseCase.Output.Success,
            )
            verify(mockAuthRepository, times(1)).refreshSession(CURRENT_REFRESH_TOKEN, SERVER_USER_ID)
            verify(mockSessionRepository, times(1)).saveSession(USER_ID, ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN)
            verify(mockSessionRepository, never()).saveMfaToken(any(), any())
        }

    @Test
    fun `concurrent refresh requests should share a failed refresh result`() =
        runTest {
            mockAuthRepository.stub {
                on { refreshSession(CURRENT_REFRESH_TOKEN, SERVER_USER_ID) } doSuspendableAnswer {
                    delay(1.seconds)
                    DomainResult.Incomplete.Error(UNKNOWN, null)
                }
            }
            val useCase = get<RefreshSessionUseCase>()

            val results =
                listOf(
                    async { useCase.execute(Unit) },
                    async { useCase.execute(Unit) },
                ).awaitAll()

            assertThat(results).containsExactly(
                RefreshSessionUseCase.Output.Failure,
                RefreshSessionUseCase.Output.Failure,
            )
            verify(mockAuthRepository, times(1)).refreshSession(CURRENT_REFRESH_TOKEN, SERVER_USER_ID)
            verify(mockSessionRepository, never()).saveSession(any(), any(), any())
            verify(mockSessionRepository, never()).saveMfaToken(any(), any())
        }

    @Test
    fun `subsequent refresh requests should each perform an own refresh`() =
        runTest {
            mockAuthRepository.stub {
                on { refreshSession(CURRENT_REFRESH_TOKEN, SERVER_USER_ID) }.thenReturn(
                    DomainResult.Finished(RefreshedSession(ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN, mfaToken = null)),
                )
            }
            val useCase = get<RefreshSessionUseCase>()

            val firstResult = useCase.execute(Unit)
            val secondResult = useCase.execute(Unit)

            assertThat(firstResult).isEqualTo(RefreshSessionUseCase.Output.Success)
            assertThat(secondResult).isEqualTo(RefreshSessionUseCase.Output.Success)
            verify(mockAuthRepository, times(2)).refreshSession(CURRENT_REFRESH_TOKEN, SERVER_USER_ID)
        }

    @Test
    fun `refresh should keep the stored mfa token when the response carries no mfa cookie`() =
        runTest {
            mockAuthRepository.stub {
                on { refreshSession(CURRENT_REFRESH_TOKEN, SERVER_USER_ID) }.thenReturn(
                    DomainResult.Finished(RefreshedSession(ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN, mfaToken = null)),
                )
            }
            val useCase = get<RefreshSessionUseCase>()

            val result = useCase.execute(Unit)

            assertThat(result).isEqualTo(RefreshSessionUseCase.Output.Success)
            verify(mockSessionRepository).saveSession(USER_ID, ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN)
            verify(mockSessionRepository, never()).saveMfaToken(any(), any())
            verify(mockSessionRepository, never()).removeMfaToken(any())
        }

    @Test
    fun `refresh should save the mfa cookie echoed by the response`() =
        runTest {
            mockAuthRepository.stub {
                on { refreshSession(CURRENT_REFRESH_TOKEN, SERVER_USER_ID) }.thenReturn(
                    DomainResult.Finished(
                        RefreshedSession(ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN, mfaToken = ECHOED_MFA_TOKEN),
                    ),
                )
            }
            val useCase = get<RefreshSessionUseCase>()

            val result = useCase.execute(Unit)

            assertThat(result).isEqualTo(RefreshSessionUseCase.Output.Success)
            verify(mockSessionRepository).saveMfaToken(USER_ID, ECHOED_MFA_TOKEN)
        }

    private companion object {
        private const val USER_ID = "userId"
        private const val SERVER_USER_ID = "serverUserId"
        private const val CURRENT_REFRESH_TOKEN = "currentRefreshToken"
        private const val ROTATED_ACCESS_TOKEN = "rotatedAccessToken"
        private const val ROTATED_REFRESH_TOKEN = "rotatedRefreshToken"
        private const val ECHOED_MFA_TOKEN = "passbolt_mfa=echoedMfaToken"
    }
}
