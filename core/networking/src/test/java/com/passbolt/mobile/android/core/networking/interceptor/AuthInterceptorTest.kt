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

package com.passbolt.mobile.android.core.networking.interceptor

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.networking.AuthPaths
import com.passbolt.mobile.android.core.networking.PLACEHOLDER_BASE_URL
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AuthInterceptorTest {
    private val getSessionUseCase =
        mock<GetSessionUseCase> {
            on { execute(Unit) } doReturn GetSessionUseCase.Output(ACCESS_TOKEN, REFRESH_TOKEN, null)
        }
    private val authInterceptor = AuthInterceptor(getSessionUseCase)

    @Test
    fun `should not add access token to anonymous paths`() {
        val anonymousPaths =
            listOf(
                AuthPaths.AUTH_SIGN_IN,
                AuthPaths.AUTH_RSA,
                AuthPaths.AUTH_VERIFY,
                AuthPaths.AUTH_JWT_REFRESH,
                AuthPaths.MFA_VERIFICATION_TOTP,
                AuthPaths.MFA_VERIFICATION_YUBIKEY,
                AuthPaths.MFA_VERIFICATION_DUO_PROMPT,
                AuthPaths.MFA_VERIFICATION_DUO_VERIFY,
            )

        anonymousPaths.forEach { path ->
            assertThat(intercept("$PLACEHOLDER_BASE_URL$path").header(AUTHORIZATION_HEADER)).isNull()
        }
    }

    @Test
    fun `should not add access token to anonymous paths with query parameters`() {
        val url = "$PLACEHOLDER_BASE_URL${AuthPaths.MFA_VERIFICATION_DUO_VERIFY}?state=state&duo_code=code&mobile=1"

        assertThat(intercept(url).header(AUTHORIZATION_HEADER)).isNull()
    }

    @Test
    fun `should add access token to authenticated paths`() {
        val sent = intercept("$PLACEHOLDER_BASE_URL/resources.json")

        assertThat(sent.header(AUTHORIZATION_HEADER)).isEqualTo("Bearer $ACCESS_TOKEN")
    }

    @Test
    fun `should match anonymous paths exactly and not by containment`() {
        val sent = intercept("$PLACEHOLDER_BASE_URL/other${AuthPaths.AUTH_SIGN_IN}")

        assertThat(sent.header(AUTHORIZATION_HEADER)).isEqualTo("Bearer $ACCESS_TOKEN")
    }

    @Test
    fun `should not add access token to avatar and transfer paths`() {
        assertThat(intercept("$PLACEHOLDER_BASE_URL/img/avatar/50/uuid.jpg").header(AUTHORIZATION_HEADER)).isNull()
        assertThat(intercept("$PLACEHOLDER_BASE_URL/mobile/transfers/uuid.json").header(AUTHORIZATION_HEADER)).isNull()
    }

    private fun intercept(url: String): Request {
        val chain =
            mock<Interceptor.Chain> {
                on { request() } doReturn Request.Builder().url(url).build()
                on { proceed(any()) } doAnswer { emptyResponse(it.getArgument(0)) }
            }

        authInterceptor.intercept(chain)

        return argumentCaptor<Request> { verify(chain).proceed(capture()) }.firstValue
    }

    private fun emptyResponse(request: Request) =
        Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody())
            .build()

    private companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val ACCESS_TOKEN = "access-token"
        private const val REFRESH_TOKEN = "refresh-token"
    }
}
