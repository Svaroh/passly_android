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

package net.svaroh.passly.data.metadata.datasource.local

import net.svaroh.passly.data.metadata.mapper.toDomain
import net.svaroh.passly.data.metadata.mapper.toEntity
import net.svaroh.passly.database.DatabaseProvider
import net.svaroh.passly.domain.metadata.datasource.MetadataKeysLocalDataSource
import net.svaroh.passly.domain.metadata.model.MetadataKeyPurpose
import net.svaroh.passly.domain.metadata.model.ParsedMetadataKey

internal class MetadataKeysLocalDataSourceImpl(
    private val databaseProvider: DatabaseProvider,
) : MetadataKeysLocalDataSource {
    override suspend fun getMetadataKeys(
        purpose: MetadataKeyPurpose,
        userId: String,
    ): List<ParsedMetadataKey> {
        val metadataKeysDao =
            databaseProvider
                .get(userId)
                .metadataKeysDao()

        return when (purpose) {
            MetadataKeyPurpose.ENCRYPT -> metadataKeysDao.getEncryptionMetadataKeysWithPrivateKeys()
            MetadataKeyPurpose.DECRYPT -> metadataKeysDao.getDecryptionMetadataKeysWithPrivateKeys()
        }.map { it.toDomain() }
    }

    override suspend fun getMetadataKey(
        metadataKeyId: String,
        userId: String,
    ): ParsedMetadataKey =
        databaseProvider
            .get(userId)
            .metadataKeysDao()
            .getMetadataKey(metadataKeyId)
            .toDomain()

    override suspend fun rebuildMetadataKeys(
        metadataKeys: List<ParsedMetadataKey>,
        userId: String,
    ) {
        removeMetadataKeys(userId)
        addMetadataKeys(metadataKeys, userId)
    }

    private suspend fun addMetadataKeys(
        metadataKeys: List<ParsedMetadataKey>,
        userId: String,
    ) {
        val metadataKeysDao =
            databaseProvider
                .get(userId)
                .metadataKeysDao()

        val metadataPrivateKeysDao =
            databaseProvider
                .get(userId)
                .metadataPrivateKeysDao()

        val keys = metadataKeys.map { it.toEntity() }
        val privateKeys =
            metadataKeys
                .flatMap { metadataKey ->
                    metadataKey.metadataPrivateKeys.map { privateKey ->
                        privateKey.toEntity(metadataKey.id.toString())
                    }
                }

        metadataKeysDao.insertAll(keys)
        metadataPrivateKeysDao.insertAll(privateKeys)
    }

    private suspend fun removeMetadataKeys(userId: String) {
        val metadataKeysDao =
            databaseProvider
                .get(userId)
                .metadataKeysDao()

        val metadataPrivateKeysDao =
            databaseProvider
                .get(userId)
                .metadataPrivateKeysDao()

        metadataPrivateKeysDao.deleteAll()
        metadataKeysDao.deleteAll()
    }
}
