/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.core.secrets.usecase.decrypt

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.secrets.usecase.db.GetLocalSecretUseCase
import net.svaroh.passly.core.secrets.usecase.db.UpsertLocalSecretsUseCase
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.io.IOException

class SecretInteractorTest : KoinTest {
    private val fetchSecretUseCase = mock<FetchSecretUseCase>()
    private val decryptSecretUseCase = mock<DecryptSecretUseCase>()
    private val getLocalSecretUseCase = mock<GetLocalSecretUseCase>()
    private val upsertLocalSecretsUseCase = mock<UpsertLocalSecretsUseCase>()

    private val interactor =
        SecretInteractor(
            fetchSecretUseCase = fetchSecretUseCase,
            decryptSecretUseCase = decryptSecretUseCase,
            getLocalSecretUseCase = getLocalSecretUseCase,
            upsertLocalSecretsUseCase = upsertLocalSecretsUseCase,
        )

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            modules(
                module {
                    single {
                        mock<GetSelectedAccountUseCase> {
                            on { execute(Unit) } doReturn GetSelectedAccountUseCase.Output(ACCOUNT_ID)
                        }
                    }
                },
            )
        }

    @Test
    fun `a cached secret is decrypted without touching the network`() =
        runTest {
            getLocalSecretUseCase.stub {
                onBlocking { execute(GetLocalSecretUseCase.Input(RESOURCE_ID)) } doReturn
                    GetLocalSecretUseCase.Output.Cached(ARMORED_SECRET)
            }
            decryptSecretUseCase.stub {
                onBlocking { execute(DecryptSecretUseCase.Input(ARMORED_SECRET)) } doReturn
                    DecryptSecretUseCase.Output.DecryptedSecret(PLAIN_SECRET)
            }

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.Success(PLAIN_SECRET))
            verifyNoInteractions(fetchSecretUseCase)
        }

    @Test
    fun `a secret fetched from the server is stored for later offline use`() =
        runTest {
            getLocalSecretUseCase.stub {
                onBlocking { execute(any()) } doReturn GetLocalSecretUseCase.Output.NotCached
            }
            fetchSecretUseCase.stub {
                onBlocking { execute(FetchSecretUseCase.Input(RESOURCE_ID)) } doReturn
                    FetchSecretUseCase.Output.EncryptedSecret(ARMORED_SECRET)
            }
            decryptSecretUseCase.stub {
                onBlocking { execute(DecryptSecretUseCase.Input(ARMORED_SECRET)) } doReturn
                    DecryptSecretUseCase.Output.DecryptedSecret(PLAIN_SECRET)
            }

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.Success(PLAIN_SECRET))
            verify(upsertLocalSecretsUseCase).execute(
                UpsertLocalSecretsUseCase.Input(
                    listOf(
                        UpsertLocalSecretsUseCase.LocalSecret(
                            resourceId = RESOURCE_ID,
                            secretId = null,
                            armoredData = ARMORED_SECRET,
                            modified = null,
                        ),
                    ),
                ),
            )
        }

    @Test
    fun `failing to store a fetched secret does not fail the read`() =
        runTest {
            getLocalSecretUseCase.stub {
                onBlocking { execute(any()) } doReturn GetLocalSecretUseCase.Output.NotCached
            }
            fetchSecretUseCase.stub {
                onBlocking { execute(any()) } doReturn FetchSecretUseCase.Output.EncryptedSecret(ARMORED_SECRET)
            }
            upsertLocalSecretsUseCase.stub {
                onBlocking { execute(any()) } doThrow IllegalStateException("database is busy")
            }
            decryptSecretUseCase.stub {
                onBlocking { execute(DecryptSecretUseCase.Input(ARMORED_SECRET)) } doReturn
                    DecryptSecretUseCase.Output.DecryptedSecret(PLAIN_SECRET)
            }

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.Success(PLAIN_SECRET))
        }

    @Test
    fun `an unreachable server only matters when there is no local copy`() =
        runTest {
            val networkError = IOException("server is gone")
            getLocalSecretUseCase.stub {
                onBlocking { execute(any()) } doReturn GetLocalSecretUseCase.Output.NotCached
            }
            fetchSecretUseCase.stub {
                onBlocking { execute(any()) } doReturn FetchSecretUseCase.Output.Failure(networkError)
            }

            val output = interactor.fetchAndDecrypt(RESOURCE_ID)

            assertThat(output).isEqualTo(SecretInteractor.Output.FetchFailure(networkError))
            verify(upsertLocalSecretsUseCase, never()).execute(any())
        }

    private companion object {
        private const val ACCOUNT_ID = "account-id"
        private const val RESOURCE_ID = "resource-id"
        private const val ARMORED_SECRET = "-----BEGIN PGP MESSAGE-----"
        private const val PLAIN_SECRET = """{"password":"secret"}"""
    }
}
