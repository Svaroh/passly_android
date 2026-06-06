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

package net.svaroh.passly.domain.metadata.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.metadata.MetadataRepository
import net.svaroh.passly.domain.metadata.model.TrustedMetadataKey
import java.time.ZonedDateTime
import java.util.UUID

class SaveTrustedMetadataKeyUseCase(
    private val metadataRepository: MetadataRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) : AsyncUseCase<SaveTrustedMetadataKeyUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val accountId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        metadataRepository.saveTrustedMetadataKey(
            TrustedMetadataKey(
                id = input.id,
                userId = input.userId,
                keyData = input.keyData,
                passphrase = input.passphrase,
                created = input.created,
                createdBy = input.createdBy,
                modified = input.modified,
                modifiedBy = input.modifiedBy,
                keyPgpMessage = input.keyPgpMessage,
                signingKeyFingerprint = input.signingKeyFingerprint,
                signatureCreationTimestampSeconds = input.signatureCreationTimestampSeconds,
                signedUsername = input.signedUsername,
                signedName = input.signedName,
            ),
            accountId,
        )
    }

    data class Input(
        val id: UUID,
        val userId: UUID,
        val keyData: String,
        val passphrase: String,
        val created: ZonedDateTime,
        val createdBy: UUID?,
        val modified: ZonedDateTime,
        val modifiedBy: UUID?,
        val keyPgpMessage: String,
        val signingKeyFingerprint: String,
        val signatureCreationTimestampSeconds: Long,
        val signedUsername: String,
        val signedName: String,
    )
}
