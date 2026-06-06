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

package net.svaroh.passly.data.metadata

import net.svaroh.passly.domain.metadata.MetadataRepository
import net.svaroh.passly.domain.metadata.datasource.MetadataKeysLocalDataSource
import net.svaroh.passly.domain.metadata.datasource.MetadataSettingsLocalDataSource
import net.svaroh.passly.domain.metadata.model.MetadataKeyPurpose
import net.svaroh.passly.domain.metadata.model.MetadataKeysSettings
import net.svaroh.passly.domain.metadata.model.MetadataTypesSettings
import net.svaroh.passly.domain.metadata.model.ParsedMetadataKey
import net.svaroh.passly.domain.metadata.model.TrustedMetadataKey

internal class MetadataRepositoryImpl(
    private val settingsLocalDataSource: MetadataSettingsLocalDataSource,
    private val keysLocalDataSource: MetadataKeysLocalDataSource,
) : MetadataRepository {
    override suspend fun getMetadataKeysSettings(userId: String): MetadataKeysSettings =
        settingsLocalDataSource.getMetadataKeysSettings(userId)

    override suspend fun saveMetadataKeysSettings(
        metadataKeysSettings: MetadataKeysSettings,
        userId: String,
    ) = settingsLocalDataSource.saveMetadataKeysSettings(metadataKeysSettings, userId)

    override suspend fun getMetadataTypesSettings(userId: String): MetadataTypesSettings =
        settingsLocalDataSource.getMetadataTypesSettings(userId)

    override suspend fun saveMetadataTypesSettings(
        metadataTypesSettings: MetadataTypesSettings,
        userId: String,
    ) = settingsLocalDataSource.saveMetadataTypesSettings(metadataTypesSettings, userId)

    override suspend fun getTrustedMetadataKey(userId: String): TrustedMetadataKey? = settingsLocalDataSource.getTrustedMetadataKey(userId)

    override suspend fun saveTrustedMetadataKey(
        trustedMetadataKey: TrustedMetadataKey,
        userId: String,
    ) = settingsLocalDataSource.saveTrustedMetadataKey(trustedMetadataKey, userId)

    override suspend fun deleteTrustedMetadataKey(userId: String) = settingsLocalDataSource.deleteTrustedMetadataKey(userId)

    override suspend fun getLocalMetadataKeys(
        purpose: MetadataKeyPurpose,
        userId: String,
    ): List<ParsedMetadataKey> = keysLocalDataSource.getMetadataKeys(purpose, userId)

    override suspend fun getLocalMetadataKey(
        metadataKeyId: String,
        userId: String,
    ): ParsedMetadataKey = keysLocalDataSource.getMetadataKey(metadataKeyId, userId)

    override suspend fun rebuildMetadataKeysTables(
        metadataKeys: List<ParsedMetadataKey>,
        userId: String,
    ) = keysLocalDataSource.rebuildMetadataKeys(metadataKeys, userId)
}
