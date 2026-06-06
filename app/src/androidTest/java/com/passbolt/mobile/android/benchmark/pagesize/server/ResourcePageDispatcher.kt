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

import net.svaroh.passly.benchmark.pagesize.fixture.ResourceCorpus
import net.svaroh.passly.benchmark.pagesize.fixture.ResourcePagePayloadFactory
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import timber.log.Timber

class ResourcePageDispatcher(
    private val corpus: ResourceCorpus,
    private val payloadFactory: ResourcePagePayloadFactory,
) : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse =
        try {
            val requestUrl = request.requestUrl
            val ids = corpus.ids
            val page = requestUrl?.queryParameter("page")?.toIntOrNull() ?: FIRST_PAGE
            val limit = requestUrl?.queryParameter("limit")?.toIntOrNull() ?: ids.size
            val from = (page - 1) * limit
            val pageIds = if (from in ids.indices) ids.subList(from, minOf(from + limit, ids.size)) else emptyList()
            MockResponse()
                .setResponseCode(HTTP_OK)
                .setHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .setBody(
                    payloadFactory.renderPage(
                        ids = pageIds,
                        totalCount = ids.size,
                        page = page,
                        limit = limit,
                        startIndex = from,
                    ),
                )
        } catch (throwable: Throwable) {
            Timber.e(throwable, "Resource page dispatch failed while building the response body")
            MockResponse()
                .setResponseCode(HTTP_INTERNAL_ERROR)
                .setBody("dispatch failed: ${throwable.javaClass.name}: ${throwable.message}")
        }

    private companion object {
        private const val FIRST_PAGE = 1
        private const val HTTP_OK = 200
        private const val HTTP_INTERNAL_ERROR = 500
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val APPLICATION_JSON = "application/json"
    }
}
