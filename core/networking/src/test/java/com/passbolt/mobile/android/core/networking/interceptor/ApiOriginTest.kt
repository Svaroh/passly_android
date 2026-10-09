package net.svaroh.passly.core.networking.interceptor

import com.google.common.truth.Truth.assertThat
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

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

class ApiOriginTest {
    private val apiOrigin = ApiOrigin("https://example.com/passbolt".toHttpUrl())

    @Test
    fun `should match any path and query on the api origin`() {
        assertThat(apiOrigin.matches("https://example.com/other/path.json?query=1".toHttpUrl())).isTrue()
    }

    @Test
    fun `should match the api origin with an explicit default port`() {
        assertThat(apiOrigin.matches("https://example.com:443/passbolt/resources.json".toHttpUrl())).isTrue()
    }

    @Test
    fun `should match the api host regardless of letter case`() {
        assertThat(apiOrigin.matches("https://EXAMPLE.com/passbolt/resources.json".toHttpUrl())).isTrue()
    }

    @Test
    fun `should not match a subdomain of the api host`() {
        assertThat(apiOrigin.matches("https://api.example.com/passbolt/resources.json".toHttpUrl())).isFalse()
    }
}
