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

package net.svaroh.passly.domain.metadata.mapper

import net.svaroh.passly.domain.metadata.model.MetadataKey
import net.svaroh.passly.domain.metadata.model.MetadataKeysSettings
import net.svaroh.passly.domain.metadata.model.MetadataPrivateKey
import net.svaroh.passly.domain.metadata.model.MetadataSessionKeysBundle
import net.svaroh.passly.domain.metadata.model.MetadataType
import net.svaroh.passly.domain.metadata.model.MetadataTypesSettings
import net.svaroh.passly.domain.metadata.model.ParsedMetadataKey
import net.svaroh.passly.domain.metadata.model.ParsedMetadataPrivateKey
import net.svaroh.passly.ui.MetadataKeyModel
import net.svaroh.passly.ui.MetadataKeysSettingsModel
import net.svaroh.passly.ui.MetadataPrivateKeyModel
import net.svaroh.passly.ui.MetadataSessionKeysBundleModel
import net.svaroh.passly.ui.MetadataTypeModel
import net.svaroh.passly.ui.MetadataTypesSettingsModel
import net.svaroh.passly.ui.ParsedMetadataKeyModel
import net.svaroh.passly.ui.ParsedMetadataPrivateKeyModel

fun MetadataKeysSettings.toUiModel(): MetadataKeysSettingsModel =
    MetadataKeysSettingsModel(
        allowUsageOfPersonalKeys = allowUsageOfPersonalKeys,
        zeroKnowledgeKeyShare = zeroKnowledgeKeyShare,
    )

fun MetadataTypesSettings.toUiModel(): MetadataTypesSettingsModel =
    MetadataTypesSettingsModel(
        defaultMetadataType = defaultMetadataType.toUiModel(),
        defaultFolderType = defaultFolderType.toUiModel(),
        defaultTagType = defaultTagType.toUiModel(),
        allowCreationOfV5Resources = allowCreationOfV5Resources,
        allowCreationOfV5Folders = allowCreationOfV5Folders,
        allowCreationOfV5Tags = allowCreationOfV5Tags,
        allowCreationOfV4Resources = allowCreationOfV4Resources,
        allowCreationOfV4Folders = allowCreationOfV4Folders,
        allowCreationOfV4Tags = allowCreationOfV4Tags,
        allowV4V5Upgrade = allowV4V5Upgrade,
        allowV5V4Downgrade = allowV5V4Downgrade,
    )

fun MetadataType.toUiModel(): MetadataTypeModel =
    when (this) {
        MetadataType.V4 -> MetadataTypeModel.V4
        MetadataType.V5 -> MetadataTypeModel.V5
    }

fun MetadataKey.toUiModel(): MetadataKeyModel =
    MetadataKeyModel(
        id = id,
        fingerprint = fingerprint,
        armoredKey = armoredKey,
        modified = modified,
        expired = expired,
        deleted = deleted,
        metadataPrivateKeys = metadataPrivateKeys.map { it.toUiModel() },
    )

fun MetadataPrivateKey.toUiModel(): MetadataPrivateKeyModel =
    MetadataPrivateKeyModel(
        id = id,
        metadataKeyId = metadataKeyId,
        userId = userId,
        pgpMessage = pgpMessage,
        created = created,
        createdBy = createdBy,
        modified = modified,
        modifiedBy = modifiedBy,
    )

fun ParsedMetadataKey.toUiModel(): ParsedMetadataKeyModel =
    ParsedMetadataKeyModel(
        id = id,
        armoredKey = armoredKey,
        fingerprint = fingerprint,
        modified = modified,
        expired = expired,
        deleted = deleted,
        metadataPrivateKeys = metadataPrivateKeys.map { it.toUiModel() },
    )

fun ParsedMetadataPrivateKey.toUiModel(): ParsedMetadataPrivateKeyModel =
    ParsedMetadataPrivateKeyModel(
        id = id,
        userId = userId,
        keyData = keyData,
        passphrase = passphrase,
        created = created,
        createdBy = createdBy,
        modified = modified,
        modifiedBy = modifiedBy,
        fingerprint = fingerprint,
        domain = domain,
        pgpMessage = pgpMessage,
    )

fun MetadataSessionKeysBundle.toUiModel(): MetadataSessionKeysBundleModel =
    MetadataSessionKeysBundleModel(
        id = id,
        userId = userId,
        data = data,
        created = created,
        modified = modified,
    )

fun MetadataKeysSettingsModel.toDomain(): MetadataKeysSettings =
    MetadataKeysSettings(
        allowUsageOfPersonalKeys = allowUsageOfPersonalKeys,
        zeroKnowledgeKeyShare = zeroKnowledgeKeyShare,
    )

fun MetadataTypesSettingsModel.toDomain(): MetadataTypesSettings =
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

fun MetadataTypeModel.toDomain(): MetadataType =
    when (this) {
        MetadataTypeModel.V4 -> MetadataType.V4
        MetadataTypeModel.V5 -> MetadataType.V5
    }

fun ParsedMetadataKeyModel.toDomain(): ParsedMetadataKey =
    ParsedMetadataKey(
        id = id,
        armoredKey = armoredKey,
        fingerprint = fingerprint,
        modified = modified,
        expired = expired,
        deleted = deleted,
        metadataPrivateKeys = metadataPrivateKeys.map { it.toDomain() },
    )

fun ParsedMetadataPrivateKeyModel.toDomain(): ParsedMetadataPrivateKey =
    ParsedMetadataPrivateKey(
        id = id,
        userId = userId,
        keyData = keyData,
        passphrase = passphrase,
        created = created,
        createdBy = createdBy,
        modified = modified,
        modifiedBy = modifiedBy,
        fingerprint = fingerprint,
        domain = domain,
        pgpMessage = pgpMessage,
    )
