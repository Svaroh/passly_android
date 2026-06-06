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

package net.svaroh.passly.data.share.mapper

import net.svaroh.passly.domain.share.model.EncryptedSecret
import net.svaroh.passly.domain.share.model.ShareChanges
import net.svaroh.passly.domain.share.model.SharePermission
import net.svaroh.passly.domain.share.model.ShareRecipient
import net.svaroh.passly.dto.request.EncryptedSharedSecret
import net.svaroh.passly.dto.response.ShareRecipientDto
import net.svaroh.passly.dto.response.SimulateShareResponse
import net.svaroh.passly.dto.request.SharePermission as SharePermissionDto

internal fun SharePermission.toDto(): SharePermissionDto =
    when (this) {
        is SharePermission.NewSharePermission ->
            SharePermissionDto.NewSharePermission(
                aro = aro,
                aroForeignKey = aroForeignKey,
                aco = aco,
                acoForeignKey = acoForeignKey,
                type = type,
            )
        is SharePermission.UpdatedSharePermission ->
            SharePermissionDto.UpdatedSharePermission(
                id = id,
                aro = aro,
                aroForeignKey = aroForeignKey,
                aco = aco,
                acoForeignKey = acoForeignKey,
                type = type,
            )
        is SharePermission.DeletedSharePermission ->
            SharePermissionDto.DeletedSharePermission(
                id = id,
                aro = aro,
                aroForeignKey = aroForeignKey,
                aco = aco,
                acoForeignKey = acoForeignKey,
                type = type,
            )
    }

internal fun EncryptedSecret.toDto(): EncryptedSharedSecret =
    EncryptedSharedSecret(
        resourceId = resourceId,
        userId = userId,
        data = data,
    )

internal fun SimulateShareResponse.toDomain(): ShareChanges =
    ShareChanges(
        added = changes.added.map { it.toDomain() },
        removed = changes.removed.map { it.toDomain() },
    )

internal fun ShareRecipientDto.toDomain(): ShareRecipient = ShareRecipient(userId = user.id.toString())
