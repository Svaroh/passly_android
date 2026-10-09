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

package net.svaroh.passly.domain.rbac.usecase

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.rbac.RbacRepository
import net.svaroh.passly.domain.rbac.mapper.toUiModel
import net.svaroh.passly.ui.RbacModel
import timber.log.Timber

class RbacInteractor(
    private val rbacRepository: RbacRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) {
    suspend fun fetchAndSaveRbacRulesFlags(): Output {
        Timber.d("Refreshing RBAC rules")
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        return when (val result = rbacRepository.refreshRbac(userId)) {
            is DomainResult.Incomplete -> {
                Timber.e("Failed to refresh RBAC rules")
                Output.Failure
            }
            is DomainResult.Finished -> {
                Timber.d("RBAC rules refreshed")
                Output.Success(result.value.toUiModel())
            }
        }
    }

    sealed class Output {
        data class Success(
            val rbacModel: RbacModel,
        ) : Output()

        data object Failure : Output()
    }
}
