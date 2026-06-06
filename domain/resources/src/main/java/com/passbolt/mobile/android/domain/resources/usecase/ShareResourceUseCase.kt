package net.svaroh.passly.domain.resources.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.displayMessage
import net.svaroh.passly.core.mvp.authentication.AuthenticatedUseCaseOutput
import net.svaroh.passly.core.mvp.authentication.CompleteAuthenticatedOutput
import net.svaroh.passly.core.mvp.authentication.IncompleteAuthenticatedOutput
import net.svaroh.passly.domain.share.ShareRepository
import net.svaroh.passly.domain.share.model.EncryptedSecret
import net.svaroh.passly.domain.share.model.SharePermission
import net.svaroh.passly.ui.EncryptedSecretOrError

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
class ShareResourceUseCase(
    private val shareRepository: ShareRepository,
) : AsyncUseCase<ShareResourceUseCase.Input, ShareResourceUseCase.Output> {
    override suspend fun execute(input: Input) =
        when (
            val result =
                shareRepository.shareResource(
                    input.resourceId,
                    input.sharePermissions,
                    input.encryptedSecrets.map { EncryptedSecret(input.resourceId, it.userId, it.data) },
                )
        ) {
            is DomainResult.Finished -> Output.Success
            is DomainResult.Incomplete -> Output.Failure(result)
        }

    sealed class Output : AuthenticatedUseCaseOutput {
        data object Success : Output(), CompleteAuthenticatedOutput

        data class Failure(
            override val incomplete: DomainResult.Incomplete,
        ) : Output(),
            IncompleteAuthenticatedOutput {
            val message: String?
                get() = incomplete.displayMessage()
        }
    }

    data class Input(
        val resourceId: String,
        val sharePermissions: List<SharePermission>,
        val encryptedSecrets: List<EncryptedSecretOrError.EncryptedSecret>,
    )
}
