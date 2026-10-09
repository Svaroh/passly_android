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

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.metadata.MetadataRemoteRepository
import net.svaroh.passly.domain.metadata.UpdateMetadataSessionKeysResult
import net.svaroh.passly.domain.metadata.datasource.MetadataRemoteDataSource
import net.svaroh.passly.domain.metadata.model.MetadataKey
import net.svaroh.passly.domain.metadata.model.MetadataKeysSettings
import net.svaroh.passly.domain.metadata.model.MetadataSessionKeysBundle
import net.svaroh.passly.domain.metadata.model.MetadataTypesSettings
import java.time.ZonedDateTime

internal class MetadataRemoteRepositoryImpl(
    private val remoteDataSource: MetadataRemoteDataSource,
) : MetadataRemoteRepository {
    override suspend fun fetchMetadataKeys(): DomainResult<List<MetadataKey>> = remoteDataSource.getMetadataKeys()

    override suspend fun fetchMetadataKeysSettings(): DomainResult<MetadataKeysSettings> = remoteDataSource.getMetadataKeysSettings()

    override suspend fun fetchMetadataTypesSettings(): DomainResult<MetadataTypesSettings> = remoteDataSource.getMetadataTypesSettings()

    override suspend fun fetchMetadataSessionKeys(): DomainResult<List<MetadataSessionKeysBundle>> =
        remoteDataSource.getMetadataSessionKeys()

    override suspend fun updateMetadataPrivateKey(
        metadataPrivateKeyId: String,
        privateKeyPgpMessage: String,
    ): DomainResult<Unit> = remoteDataSource.updateMetadataPrivateKey(metadataPrivateKeyId, privateKeyPgpMessage)

    override suspend fun postMetadataSessionKeys(encryptedData: String): DomainResult<Unit> =
        remoteDataSource.postMetadataSessionKeys(encryptedData)

    override suspend fun updateMetadataSessionKeys(
        metadataBundleId: String,
        encryptedData: String,
        modifiedDate: ZonedDateTime,
    ): UpdateMetadataSessionKeysResult = remoteDataSource.updateMetadataSessionKeys(metadataBundleId, encryptedData, modifiedDate)
}
