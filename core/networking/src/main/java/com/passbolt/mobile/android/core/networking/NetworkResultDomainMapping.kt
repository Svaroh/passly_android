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

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.OFFLINE
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.SERVER
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.TIMEOUT
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.dto.PassphraseNotInCacheException

fun <T : Any> NetworkResult<T>.toDomainResult(): DomainResult<T> =
    when (this) {
        is NetworkResult.Success -> DomainResult.Finished(value)
        is NetworkResult.Failure -> toIncomplete()
    }

private fun NetworkResult.Failure<*>.toIncomplete(): DomainResult.Incomplete =
    when {
        isUnauthorized -> DomainResult.Incomplete.Unauthorized
        isMfaRequired -> DomainResult.Incomplete.MfaRequired(MfaTypeProvider.get(this))
        exception is PassphraseNotInCacheException -> DomainResult.Incomplete.PassphraseNotInCache
        isNoNetworkException || this is NetworkResult.Failure.NetworkError ->
            DomainResult.Incomplete.Error(OFFLINE, null)
        isServerNotReachable ->
            DomainResult.Incomplete.Error(TIMEOUT, null)
        this is NetworkResult.Failure.ServerError ->
            DomainResult.Incomplete.Error(SERVER, headerMessage)
        else -> DomainResult.Incomplete.Error(UNKNOWN, exception.message)
    }
