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
import net.svaroh.passly.ui.MergedSessionKeys
import net.svaroh.passly.ui.MetadataKeyTypeModel
import net.svaroh.passly.ui.SessionKeyIdentifier
import net.svaroh.passly.ui.SessionKeyModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MetadataMapper {
    fun mapToDto(metadataKeyTypeModel: MetadataKeyTypeModel?) =
        metadataKeyTypeModel?.let {
            when (it) {
                MetadataKeyTypeModel.SHARED -> MetadataKeyTypeDto.SHARED
                MetadataKeyTypeModel.PERSONAL -> MetadataKeyTypeDto.PERSONAL
            }
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
