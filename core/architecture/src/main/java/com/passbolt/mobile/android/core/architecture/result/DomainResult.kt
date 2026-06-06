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

package net.svaroh.passly.core.architecture.result

import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider

sealed interface DomainResult<out T> {
    data class Finished<out T>(
        val value: T,
    ) : DomainResult<T>

    sealed interface Incomplete : DomainResult<Nothing> {
        data object Unauthorized : Incomplete

        data class MfaRequired(
            val providers: List<MfaProvider?>?,
        ) : Incomplete

        data object NotCached : Incomplete

        data object PassphraseNotInCache : Incomplete

        data class Error(
            val reason: Reason,
            val message: String?,
        ) : Incomplete {
            enum class Reason { OFFLINE, TIMEOUT, SERVER, UNKNOWN }
        }
    }
}

fun DomainResult.Incomplete.displayMessage(): String? = (this as? DomainResult.Incomplete.Error)?.message

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> =
    when (this) {
        is DomainResult.Finished -> DomainResult.Finished(transform(value))
        is DomainResult.Incomplete -> this
    }
