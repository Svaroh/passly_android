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

package net.svaroh.passly.feature.authentication.auth.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.mvp.authentication.AuthenticatedUseCaseOutput
import net.svaroh.passly.core.mvp.authentication.CompleteAuthenticatedOutput
import net.svaroh.passly.core.mvp.authentication.IncompleteAuthenticatedOutput
import net.svaroh.passly.domain.mfa.MfaRepository
import net.svaroh.passly.domain.mfa.model.DuoVerification
import timber.log.Timber

class VerifyDuoCallbackUseCase(
    private val mfaRepository: MfaRepository,
) : AsyncUseCase<VerifyDuoCallbackUseCase.Input, VerifyDuoCallbackUseCase.Output> {
    override suspend fun execute(input: Input): Output =
        when (
            val result =
                mfaRepository.verifyDuoCallback(
                    authToken = input.jwtHeader,
                    duoStateUuid = input.passboltDuoCookieUuid,
                    state = input.duoState,
                    code = input.duoCode,
                )
        ) {
            is DomainResult.Finished ->
                when (val verification = result.value) {
                    is DuoVerification.Succeeded -> Output.Success(verification.mfaHeader)
                    is DuoVerification.Failed -> {
                        Timber.e("Error during verifying duo callback: ${verification.message}")
                        Output.Error(verification.message)
                    }
                }
            is DomainResult.Incomplete -> Output.Failure(result)
        }

    data class Input(
        val jwtHeader: String,
        val passboltDuoCookieUuid: String,
        val duoState: String?,
        val duoCode: String?,
    )

    sealed class Output : AuthenticatedUseCaseOutput {
        data class Success(
            val mfaHeader: String?,
        ) : Output(),
            CompleteAuthenticatedOutput

        data object Unauthorized : Output(), CompleteAuthenticatedOutput

        data class Failure(
            override val incomplete: DomainResult.Incomplete,
        ) : Output(),
            IncompleteAuthenticatedOutput

        class Error(
            val message: String,
        ) : Output(),
            CompleteAuthenticatedOutput
    }
}
