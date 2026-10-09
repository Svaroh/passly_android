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

package net.svaroh.passly.data.metadata.mapper

import net.svaroh.passly.domain.metadata.model.ParsedMetadataKey
import net.svaroh.passly.domain.metadata.model.ParsedMetadataPrivateKey
import net.svaroh.passly.entity.metadata.MetadataKeyWithPrivateKeys
import java.util.UUID
import net.svaroh.passly.entity.metadata.MetadataKey as MetadataKeyEntity
import net.svaroh.passly.entity.metadata.MetadataPrivateKey as MetadataPrivateKeyEntity

internal fun MetadataKeyWithPrivateKeys.toDomain(): ParsedMetadataKey =
    ParsedMetadataKey(
        id = UUID.fromString(metadataKey.id),
        armoredKey = metadataKey.armoredKey,
        fingerprint = metadataKey.fingerprint,
        modified = metadataKey.modified,
        expired = metadataKey.expired,
        deleted = metadataKey.deleted,
        metadataPrivateKeys = metadataPrivateKeys.map { it.toDomain() },
    )

internal fun MetadataPrivateKeyEntity.toDomain(): ParsedMetadataPrivateKey =
    ParsedMetadataPrivateKey(
        id = UUID.fromString(id),
        userId = UUID.fromString(userId),
        keyData = data,
        passphrase = passphrase,
        created = created,
        createdBy = createdBy?.let { UUID.fromString(it) },
        modified = modified,
        modifiedBy = modifiedBy?.let { UUID.fromString(it) },
        fingerprint = fingerprint,
        domain = domain,
        pgpMessage = pgpMessage,
    )

internal fun ParsedMetadataKey.toEntity(): MetadataKeyEntity =
    MetadataKeyEntity(
        id = id.toString(),
        fingerprint = fingerprint,
        armoredKey = armoredKey,
        modified = modified,
        expired = expired,
        deleted = deleted,
    )

internal fun ParsedMetadataPrivateKey.toEntity(metadataKeyId: String): MetadataPrivateKeyEntity =
    MetadataPrivateKeyEntity(
        id = id.toString(),
        metadataKeyId = metadataKeyId,
        userId = userId.toString(),
        data = keyData,
        passphrase = passphrase,
        created = created,
        createdBy = createdBy?.toString(),
        modified = modified,
        modifiedBy = modifiedBy?.toString(),
        pgpMessage = pgpMessage,
        domain = domain,
        fingerprint = fingerprint,
    )
