package net.svaroh.passly.domain.mobiletransfer.usecase

import kotlinx.coroutines.withContext
import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.domain.mobiletransfer.MobileTransferRepository
import net.svaroh.passly.domain.mobiletransfer.mapper.toUiModel
import net.svaroh.passly.ui.Status
import net.svaroh.passly.ui.UpdateTransferUiModel

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
class UpdateTransferUseCase(
    private val mobileTransferRepository: MobileTransferRepository,
    private val coroutineContext: CoroutineLaunchContext,
) : AsyncUseCase<UpdateTransferUseCase.Input, UpdateTransferUseCase.Output> {
    override suspend fun execute(input: Input): Output =
        withContext(coroutineContext.io) {
            when (
                val result =
                    mobileTransferRepository.turnPage(
                        input.uuid,
                        input.authToken,
                        input.currentPage,
                        input.status,
                    )
            ) {
                is DomainResult.Finished -> Output.Success(result.value.toUiModel())
                is DomainResult.Incomplete -> Output.Failure(result)
            }
        }

    data class Input(
        val uuid: String,
        val authToken: String,
        val currentPage: Int,
        val status: Status,
    )

    sealed class Output {
        data class Success(
            val updateTransferModel: UpdateTransferUiModel,
        ) : Output()

        data class Failure(
            val incomplete: DomainResult.Incomplete,
        ) : Output()
    }
}
