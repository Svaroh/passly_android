package com.passbolt.mobile.android.core.networking.interceptor

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
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

internal class StripForeignOriginCredentialsInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.targetsApiOrigin()) {
            return chain.proceed(request)
        }
        if (request.carriesSessionCredentials()) {
            Timber.w("Request target is outside the API origin, sending it without session credentials")
        }
        val response = chain.proceed(request.withoutSessionCredentials())
        if (response.setsCookies()) {
            Timber.w("Response comes from outside the API origin, ignoring the cookies it sets")
        }
        return response.withoutSetCookies()
    }

    private fun Request.targetsApiOrigin() = apiOrigin?.matches(url) == true

    private fun Request.carriesSessionCredentials() = SESSION_CREDENTIAL_HEADERS.any { header(it) != null }

    private fun Request.withoutSessionCredentials() =
        newBuilder()
            .apply { SESSION_CREDENTIAL_HEADERS.forEach { removeHeader(it) } }
            .build()

    private fun Response.setsCookies() = headers(SET_COOKIE_HEADER).isNotEmpty()

    private fun Response.withoutSetCookies() =
        newBuilder()
            .removeHeader(SET_COOKIE_HEADER)
            .build()

    private companion object {
        private val SESSION_CREDENTIAL_HEADERS = listOf(AuthInterceptor.AUTH_HEADER, CookiesInterceptor.COOKIE_HEADER)
        private const val SET_COOKIE_HEADER = "Set-Cookie"
    }
}
