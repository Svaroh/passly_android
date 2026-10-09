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

package net.svaroh.passly.domain.passwordexpiry.usecase

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.mvp.authentication.AuthenticatedUseCaseOutput
import net.svaroh.passly.core.mvp.authentication.CompleteAuthenticatedOutput
import net.svaroh.passly.core.mvp.authentication.IncompleteAuthenticatedOutput
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.passwordexpiry.PasswordExpiryRepository
import net.svaroh.passly.domain.passwordexpiry.model.PasswordExpirySettings

class PasswordExpiryPoliciesInteractor(
    private val passwordExpiryRepository: PasswordExpiryRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) {
    suspend fun fetchAndSavePasswordExpiryPolicies(): Output {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        return when (val result = passwordExpiryRepository.getPasswordExpirySettings(userId)) {
            is DomainResult.Incomplete -> Output.Failure.FetchFailure(result)
            is DomainResult.Finished -> Output.Success(result.value)
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {
        data class Success(
            val passwordExpirySettings: PasswordExpirySettings,
        ) : Output(),
            CompleteAuthenticatedOutput

        sealed class Failure : Output() {
            data class FetchFailure(
                override val incomplete: DomainResult.Incomplete,
            ) : Failure(),
                IncompleteAuthenticatedOutput
        }
    }
}
