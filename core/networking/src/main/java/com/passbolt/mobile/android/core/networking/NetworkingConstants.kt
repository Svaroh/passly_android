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

package net.svaroh.passly.core.networking

// this is used only as a placeholder and it is replaced in ChangeableBaseUrlInterceptor
const val PLACEHOLDER_BASE_URL = "https://passbolt.baseurl.placeholder"

const val TIMEOUT_SECONDS = 30L

/**
 * How long a single connection attempt may take.
 *
 * A host that is down but still resolves keeps the socket hanging, and OkHttp retries every address it got from DNS,
 * so this budget is spent once per address. Ten seconds is generous for a reachable server and short enough that an
 * unreachable one is recognised quickly.
 */
const val CONNECT_TIMEOUT_SECONDS = 10L

/**
 * Upper bound for a whole call, DNS, redirects and every connection attempt included.
 *
 * Without it the per-attempt timeouts multiply by the number of addresses, which is how an unreachable host turned
 * into a minutes-long wait. It is deliberately well above the read timeout: this is a guard against pathological
 * retries, not a budget for a legitimately large response on a slow link.
 */
const val CALL_TIMEOUT_SECONDS = 60L
