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

package com.passbolt.mobile.android.common

import com.google.common.truth.Truth.assertThat
import okhttp3.Cookie
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

class CookieExtractorTest {
    private val cookieExtractor = CookieExtractor()

    @Test
    fun `get should return name and value of the cookie with exactly the requested name`() {
        val response = okHttpResponse("xpassbolt_mfa_debug=debug; path=/", "passbolt_mfa=jwt; path=/; secure; httponly")

        assertThat(cookieExtractor.get(response, CookieExtractor.MFA_COOKIE)).isEqualTo("passbolt_mfa=jwt")
    }

    @Test
    fun `get should ignore cookies whose name only contains the requested name`() {
        val response = okHttpResponse("xpassbolt_mfa_debug=debug; path=/", "passbolt_mfa_debug=debug; path=/")

        assertThat(cookieExtractor.get(response, CookieExtractor.MFA_COOKIE)).isNull()
    }

    @Test
    fun `get should ignore cookies whose value contains the requested name`() {
        val response = okHttpResponse("other=passbolt_mfa; path=/")

        assertThat(cookieExtractor.get(response, CookieExtractor.MFA_COOKIE)).isNull()
    }

    @Test
    fun `get should ignore set-cookie headers without a value`() {
        val response = okHttpResponse("passbolt_mfa")

        assertThat(cookieExtractor.get(response, CookieExtractor.MFA_COOKIE)).isNull()
    }

    @Test
    fun `get from retrofit response should match the cookie name exactly`() {
        val rawResponse = okHttpResponse("xpassbolt_mfa_debug=debug; path=/", "passbolt_mfa=jwt; path=/")
        val response = Response.success(Unit, rawResponse)

        assertThat(cookieExtractor.get(response, CookieExtractor.MFA_COOKIE)).isEqualTo("passbolt_mfa=jwt")
    }

    @Test
    fun `getCookieValue should return the value of the cookie with exactly the requested name`() {
        val rawResponse = okHttpResponse("refresh_token_debug=debug; path=/", "refresh_token=abc; path=/; httponly")
        val response = Response.success(Unit, rawResponse)

        assertThat(cookieExtractor.getCookieValue(response, CookieExtractor.REFRESH_TOKEN_COOKIE)).isEqualTo("abc")
    }

    @Test
    fun `get from cookie list should match the cookie name exactly`() {
        val cookies = listOf(cookie("xpassbolt_mfa_debug", "debug"), cookie("passbolt_mfa", "jwt"))

        assertThat(cookieExtractor.get(cookies, CookieExtractor.MFA_COOKIE)?.value).isEqualTo("jwt")
        assertThat(cookieExtractor.get(cookies, "passbolt_mf")).isNull()
    }

    private fun okHttpResponse(vararg setCookieHeaders: String) =
        okhttp3.Response
            .Builder()
            .request(Request.Builder().url("https://example.com/auth/jwt/login.json").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody())
            .apply { setCookieHeaders.forEach { addHeader("Set-Cookie", it) } }
            .build()

    private fun cookie(
        name: String,
        value: String,
    ) = Cookie
        .Builder()
        .name(name)
        .value(value)
        .domain("example.com")
        .build()
}
