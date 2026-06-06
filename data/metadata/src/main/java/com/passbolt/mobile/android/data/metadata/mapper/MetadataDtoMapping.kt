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

import net.svaroh.passly.domain.metadata.model.MetadataKey
import net.svaroh.passly.domain.metadata.model.MetadataKeysSettings
import net.svaroh.passly.domain.metadata.model.MetadataPrivateKey
import net.svaroh.passly.domain.metadata.model.MetadataSessionKeysBundle
import net.svaroh.passly.domain.metadata.model.MetadataType
import net.svaroh.passly.domain.metadata.model.MetadataTypesSettings
import net.svaroh.passly.dto.response.MetadataKeysResponseDto
import net.svaroh.passly.dto.response.MetadataKeysSettingsResponseDto
import net.svaroh.passly.dto.response.MetadataPrivateKeyDto
import net.svaroh.passly.dto.response.MetadataSessionKeyResponseDto
import net.svaroh.passly.dto.response.MetadataTypeDto
import net.svaroh.passly.dto.response.MetadataTypesSettingsResponseDto
import java.time.ZonedDateTime

internal fun MetadataKeysResponseDto.toDomain(): MetadataKey =
    MetadataKey(
        id = id,
        fingerprint = fingerprint,
        armoredKey = armoredKey,
        modified = ZonedDateTime.parse(modified),
        expired = expired?.let { ZonedDateTime.parse(it) },
        deleted = deleted?.let { ZonedDateTime.parse(it) },
        metadataPrivateKeys = metadataPrivateKeys.map { it.toDomain() },
    )

internal fun MetadataPrivateKeyDto.toDomain(): MetadataPrivateKey =
    MetadataPrivateKey(
        id = id,
        metadataKeyId = metadataKeyId,
        userId = userId,
        pgpMessage = encryptedKeyData,
        created = created,
        createdBy = createdBy,
        modified = modified,
        modifiedBy = modifiedBy,
    )

internal fun MetadataKeysSettingsResponseDto.toDomain(): MetadataKeysSettings =
    MetadataKeysSettings(
        allowUsageOfPersonalKeys = allowUsageOfPersonalKeys,
        zeroKnowledgeKeyShare = zeroKnowledgeKeyShare,
    )

internal fun MetadataTypesSettingsResponseDto.toDomain(): MetadataTypesSettings =
    MetadataTypesSettings(
        defaultMetadataType = defaultMetadataType.toDomain(),
        defaultFolderType = defaultFolderType.toDomain(),
        defaultTagType = defaultTagType.toDomain(),
        allowCreationOfV5Resources = allowCreationOfV5Resources,
        allowCreationOfV5Folders = allowCreationOfV5Folders,
        allowCreationOfV5Tags = allowCreationOfV5Tags,
        allowCreationOfV4Resources = allowCreationOfV4Resources,
        allowCreationOfV4Folders = allowCreationOfV4Folders,
        allowCreationOfV4Tags = allowCreationOfV4Tags,
        allowV4V5Upgrade = allowV4V5Upgrade,
        allowV5V4Downgrade = allowV5V4Downgrade,
    )

internal fun MetadataTypeDto.toDomain(): MetadataType =
    when (this) {
        MetadataTypeDto.V4 -> MetadataType.V4
        MetadataTypeDto.V5 -> MetadataType.V5
    }

internal fun MetadataSessionKeyResponseDto.toDomain(): MetadataSessionKeysBundle =
    MetadataSessionKeysBundle(
        id = id,
        userId = userId,
        data = data,
        created = ZonedDateTime.parse(created),
        modified = ZonedDateTime.parse(modified),
    )
