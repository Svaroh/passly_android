/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.networking.AuthPaths.AUTH_SIGN_IN
import com.passbolt.mobile.android.domain.accounts.usecase.GetCurrentApiUrlUseCase
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class HttpClientsTest : KoinTest {
    private val getCurrentApiUrlUseCase =
        mock<GetCurrentApiUrlUseCase> {
            on { execute(Unit) } doReturn GetCurrentApiUrlUseCase.Output(SUB_PATH_API_URL)
        }
    private val getSessionUseCase =
        mock<GetSessionUseCase> {
            on { execute(Unit) } doReturn GetSessionUseCase.Output(ACCESS_TOKEN, REFRESH_TOKEN, MFA_COOKIE)
        }

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            modules(
                networkingModule,
                module {
                    single { getCurrentApiUrlUseCase }
                    single { getSessionUseCase }
                },
            )
        }

    @Test
    fun `sign in on sub-path api should be sent without access token and mfa cookie`() {
        AUTHENTICATED_CLIENTS.forEach { clientName ->
            val client = get<OkHttpClient>(named(clientName))

            val sent = send(client, "$PLACEHOLDER_BASE_URL$AUTH_SIGN_IN")

            assertThat(sent.url.toString()).isEqualTo("$SUB_PATH_API_URL$AUTH_SIGN_IN")
            assertThat(sent.header(AUTHORIZATION_HEADER)).isNull()
            assertThat(sent.header(COOKIE_HEADER)).isNull()
        }
    }

    @Test
    fun `authenticated request on sub-path api should be sent with access token and mfa cookie`() {
        AUTHENTICATED_CLIENTS.forEach { clientName ->
            val client = get<OkHttpClient>(named(clientName))

            val sent = send(client, "$PLACEHOLDER_BASE_URL/resources.json")

            assertThat(sent.url.toString()).isEqualTo("$SUB_PATH_API_URL/resources.json")
            assertThat(sent.header(AUTHORIZATION_HEADER)).isEqualTo("Bearer $ACCESS_TOKEN")
            assertThat(sent.header(COOKIE_HEADER)).isEqualTo(MFA_COOKIE)
        }
    }

    private fun send(
        client: OkHttpClient,
        url: String,
    ): Request {
        val server = FakeServer()
        client
            .newBuilder()
            .addInterceptor(server)
            .build()
            .newCall(Request.Builder().url(url).build())
            .execute()
            .close()
        return server.receivedRequest
    }

    private class FakeServer : Interceptor {
        lateinit var receivedRequest: Request

        override fun intercept(chain: Interceptor.Chain): Response {
            receivedRequest = chain.request()
            return Response
                .Builder()
                .request(receivedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody())
                .build()
        }
    }

    private companion object {
        private val AUTHENTICATED_CLIENTS = listOf(DEFAULT_HTTP_CLIENT, NO_REDIRECT_HTTP_CLIENT)
        private const val SUB_PATH_API_URL = "https://example.com/passbolt"
        private const val ACCESS_TOKEN = "access-token"
        private const val REFRESH_TOKEN = "refresh-token"
        private const val MFA_COOKIE = "passbolt_mfa=mfa-jwt"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val COOKIE_HEADER = "Cookie"
    }
}
