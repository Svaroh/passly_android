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

package net.svaroh.passly.benchmark.pagesize.server

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import java.net.InetAddress

class LoopbackMockServer(
    pageDispatcher: Dispatcher,
) : AutoCloseable {
    private val server =
        MockWebServer().apply {
            start(InetAddress.getByName(IPV4_LOOPBACK), ANY_FREE_PORT)
            dispatcher = pageDispatcher
        }

    val url: String get() = "http://$IPV4_LOOPBACK:${server.port}"

    val handledRequestCount: Int get() = server.requestCount

    override fun close() = server.shutdown()

    private companion object {
        private const val IPV4_LOOPBACK = "127.0.0.1"
        private const val ANY_FREE_PORT = 0
    }
}
