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

package net.svaroh.passly.mappers

import net.svaroh.passly.dto.request.SessionKeyDto
import net.svaroh.passly.dto.request.SessionKeysBundleDto
import net.svaroh.passly.dto.response.DecryptedMetadataSessionKeysBundleModel
import net.svaroh.passly.dto.response.MetadataKeyTypeDto
import net.svaroh.passly.dto.response.MetadataKeysResponseDto
import net.svaroh.passly.dto.response.MetadataKeysSettingsResponseDto
import net.svaroh.passly.dto.response.MetadataSessionKeyResponseDto
import net.svaroh.passly.dto.response.MetadataTypeDto
import net.svaroh.passly.dto.response.MetadataTypesSettingsResponseDto
import net.svaroh.passly.entity.metadata.MetadataKey
import net.svaroh.passly.entity.metadata.MetadataKeyWithPrivateKeys
import net.svaroh.passly.entity.metadata.MetadataPrivateKey
import net.svaroh.passly.ui.MergedSessionKeys
import net.svaroh.passly.ui.MetadataKeyModel
import net.svaroh.passly.ui.MetadataKeyTypeModel
import net.svaroh.passly.ui.MetadataKeysSettingsModel
import net.svaroh.passly.ui.MetadataPrivateKeyModel
import net.svaroh.passly.ui.MetadataSessionKeysBundleModel
import net.svaroh.passly.ui.MetadataTypeModel
import net.svaroh.passly.ui.MetadataTypesSettingsModel
import net.svaroh.passly.ui.ParsedMetadataKeyModel
import net.svaroh.passly.ui.ParsedMetadataPrivateKeyModel
import net.svaroh.passly.ui.SessionKeyIdentifier
import net.svaroh.passly.ui.SessionKeyModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MetadataMapper {
    fun map(dtos: List<MetadataKeysResponseDto>): List<MetadataKeyModel> = dtos.map(::mapToEntity)

    private fun mapToEntity(dto: MetadataKeysResponseDto) =
        MetadataKeyModel(
            id = dto.id,
            fingerprint = dto.fingerprint,
            armoredKey = dto.armoredKey,
            modified = ZonedDateTime.parse(dto.modified),
            expired = dto.expired?.let { ZonedDateTime.parse(it) },
            deleted = dto.deleted?.let { ZonedDateTime.parse(it) },
            metadataPrivateKeys =
                dto.metadataPrivateKeys.map { metadataPrivateKey ->
                    MetadataPrivateKeyModel(
                        id = metadataPrivateKey.id,
                        userId = metadataPrivateKey.userId,
                        pgpMessage = metadataPrivateKey.encryptedKeyData,
                        metadataKeyId = metadataPrivateKey.metadataKeyId,
                        created = metadataPrivateKey.created,
                        createdBy = metadataPrivateKey.createdBy,
                        modified = metadataPrivateKey.modified,
                        modifiedBy = metadataPrivateKey.modifiedBy,
                    )
                },
        )

    fun map(uiModel: ParsedMetadataKeyModel) =
        MetadataKey(
            id = uiModel.id.toString(),
            fingerprint = uiModel.fingerprint,
            armoredKey = uiModel.armoredKey,
            modified = uiModel.modified,
            expired = uiModel.expired,
            deleted = uiModel.deleted,
        )

    fun map(
        uiModel: ParsedMetadataPrivateKeyModel,
        metadataKeyId: String,
    ) = MetadataPrivateKey(
        id = uiModel.id.toString(),
        metadataKeyId = metadataKeyId,
        userId = uiModel.userId.toString(),
        data = uiModel.keyData,
        passphrase = uiModel.passphrase,
        created = uiModel.created,
        createdBy = uiModel.createdBy?.toString(),
        modified = uiModel.modified,
        modifiedBy = uiModel.modifiedBy?.toString(),
        pgpMessage = uiModel.pgpMessage,
        domain = uiModel.domain,
        fingerprint = uiModel.fingerprint,
    )

    private fun MetadataTypeDto.mapToUi() =
        when (this) {
            MetadataTypeDto.V4 -> MetadataTypeModel.V4
            MetadataTypeDto.V5 -> MetadataTypeModel.V5
        }

    fun map(dto: MetadataTypesSettingsResponseDto) =
        MetadataTypesSettingsModel(
            defaultMetadataType = dto.defaultMetadataType.mapToUi(),
            defaultFolderType = dto.defaultFolderType.mapToUi(),
            defaultTagType = dto.defaultTagType.mapToUi(),
            allowCreationOfV5Resources = dto.allowCreationOfV5Resources,
            allowCreationOfV5Folders = dto.allowCreationOfV5Folders,
            allowCreationOfV5Tags = dto.allowCreationOfV5Tags,
            allowCreationOfV4Resources = dto.allowCreationOfV4Resources,
            allowCreationOfV4Folders = dto.allowCreationOfV4Folders,
            allowCreationOfV4Tags = dto.allowCreationOfV4Tags,
            allowV4V5Upgrade = dto.allowV4V5Upgrade,
            allowV5V4Downgrade = dto.allowV5V4Downgrade,
        )

    fun map(dto: MetadataKeysSettingsResponseDto) =
        MetadataKeysSettingsModel(
            allowUsageOfPersonalKeys = dto.allowUsageOfPersonalKeys,
            zeroKnowledgeKeyShare = dto.zeroKnowledgeKeyShare,
        )

    fun mapToDto(metadataKeyTypeModel: MetadataKeyTypeModel?) =
        metadataKeyTypeModel?.let {
            when (it) {
                MetadataKeyTypeModel.SHARED -> MetadataKeyTypeDto.SHARED
                MetadataKeyTypeModel.PERSONAL -> MetadataKeyTypeDto.PERSONAL
            }
        }

    fun mapToUiModel(value: List<MetadataSessionKeyResponseDto>): List<MetadataSessionKeysBundleModel> =
        value.map {
            MetadataSessionKeysBundleModel(
                id = it.id,
                userId = it.userId,
                data = it.data,
                created = ZonedDateTime.parse(it.created),
                modified = ZonedDateTime.parse(it.modified),
            )
        }

    fun map(value: ConcurrentHashMap<SessionKeyIdentifier, SessionKeyModel>): SessionKeysBundleDto =
        value
            .mapTo(mutableListOf()) { (id, keyModel) ->
                SessionKeyDto(
                    foreignModel = id.foreignModel,
                    foreignId = id.foreignId,
                    sessionKey = keyModel.sessionKey,
                    modified = keyModel.modified.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                )
            }.toList()
            .let {
                SessionKeysBundleDto(
                    objectType = "PASSBOLT_SESSION_KEYS",
                    sessionKeys = it,
                )
            }

    fun mapToUi(dao: MetadataKeyWithPrivateKeys): ParsedMetadataKeyModel =
        ParsedMetadataKeyModel(
            id = UUID.fromString(dao.metadataKey.id),
            fingerprint = dao.metadataKey.fingerprint,
            armoredKey = dao.metadataKey.armoredKey,
            modified = dao.metadataKey.modified,
            expired = dao.metadataKey.expired,
            deleted = dao.metadataKey.deleted,
            metadataPrivateKeys =
                dao.metadataPrivateKeys.map {
                    ParsedMetadataPrivateKeyModel(
                        id = UUID.fromString(it.id),
                        userId = UUID.fromString(it.userId),
                        keyData = it.data,
                        passphrase = it.passphrase,
                        created = it.created,
                        createdBy = it.createdBy?.let { UUID.fromString(it) },
                        modified = it.modified,
                        modifiedBy = it.modifiedBy?.let { UUID.fromString(it) },
                        pgpMessage = it.pgpMessage,
                        domain = it.domain,
                        fingerprint = it.fingerprint,
                    )
                },
        )

    fun map(
        mergedSessionKeys: MergedSessionKeys,
        bundleId: UUID,
    ): DecryptedMetadataSessionKeysBundleModel =
        DecryptedMetadataSessionKeysBundleModel(
            id = bundleId,
            bundle = map(mergedSessionKeys.keys),
            created = ZonedDateTime.now(),
            modified = ZonedDateTime.now(),
        )
}
