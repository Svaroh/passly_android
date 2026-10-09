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

package net.svaroh.passly.core.mvp.authentication

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa
import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Session

interface IncompleteAuthenticatedOutput : AuthenticatedUseCaseOutput {
    val incomplete: DomainResult.Incomplete

    override val authenticationState: AuthenticationState
        get() = incomplete.toAuthenticationState()
}

interface CompleteAuthenticatedOutput : AuthenticatedUseCaseOutput {
    override val authenticationState: AuthenticationState
        get() = AuthenticationState.Authenticated
}

fun DomainResult.Incomplete.toAuthenticationState(): AuthenticationState =
    when (this) {
        is DomainResult.Incomplete.Unauthorized -> AuthenticationState.Unauthenticated(Session)
        is DomainResult.Incomplete.MfaRequired -> AuthenticationState.Unauthenticated(Mfa(providers))
        is DomainResult.Incomplete.PassphraseNotInCache -> AuthenticationState.Unauthenticated(Passphrase)
        is DomainResult.Incomplete.NotCached,
        is DomainResult.Incomplete.Error,
        -> AuthenticationState.Authenticated
    }
