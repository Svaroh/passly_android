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

package net.svaroh.passly.data.metadata.datasource.remote

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.map
import net.svaroh.passly.core.networking.NetworkResult
import net.svaroh.passly.core.networking.ResponseHandler
import net.svaroh.passly.core.networking.callWithHandler
import net.svaroh.passly.core.networking.toDomainResult
import net.svaroh.passly.data.metadata.datasource.remote.api.MetadataApi
import net.svaroh.passly.data.metadata.mapper.toDomain
import net.svaroh.passly.domain.metadata.UpdateMetadataSessionKeysResult
import net.svaroh.passly.domain.metadata.datasource.MetadataRemoteDataSource
import net.svaroh.passly.domain.metadata.model.MetadataKey
import net.svaroh.passly.domain.metadata.model.MetadataKeysSettings
import net.svaroh.passly.domain.metadata.model.MetadataSessionKeysBundle
import net.svaroh.passly.domain.metadata.model.MetadataTypesSettings
import net.svaroh.passly.dto.request.EncryptedDataAndModifiedRequest
import net.svaroh.passly.dto.request.EncryptedDataRequest
import java.net.HttpURLConnection.HTTP_CONFLICT
import java.time.ZonedDateTime

internal class MetadataRemoteDataSourceImpl(
    private val metadataApi: MetadataApi,
    private val responseHandler: ResponseHandler,
) : MetadataRemoteDataSource {
    override suspend fun getMetadataKeys(): DomainResult<List<MetadataKey>> =
        callWithHandler(responseHandler) { metadataApi.getMetadataKeys().body }
            .toDomainResult()
            .map { dtos -> dtos.map { it.toDomain() } }

    override suspend fun getMetadataKeysSettings(): DomainResult<MetadataKeysSettings> =
        callWithHandler(responseHandler) { metadataApi.getMetadataKeysSettings().body }
            .toDomainResult()
            .map { it.toDomain() }

    override suspend fun getMetadataTypesSettings(): DomainResult<MetadataTypesSettings> =
        callWithHandler(responseHandler) { metadataApi.getMetadataTypesSettings().body }
            .toDomainResult()
            .map { it.toDomain() }

    override suspend fun getMetadataSessionKeys(): DomainResult<List<MetadataSessionKeysBundle>> =
        callWithHandler(responseHandler) { metadataApi.getMetadataSessionKeys().body }
            .toDomainResult()
            .map { dtos -> dtos.map { it.toDomain() } }

    override suspend fun updateMetadataPrivateKey(
        metadataPrivateKeyId: String,
        privateKeyPgpMessage: String,
    ): DomainResult<Unit> =
        callWithHandler(responseHandler) {
            metadataApi.putMetadataPrivateKey(metadataPrivateKeyId, EncryptedDataRequest(privateKeyPgpMessage)).body
        }.toDomainResult()

    override suspend fun postMetadataSessionKeys(encryptedData: String): DomainResult<Unit> =
        callWithHandler(responseHandler) {
            metadataApi.postMetadataSessionKeys(EncryptedDataRequest(encryptedData)).body
        }.toDomainResult()

    override suspend fun updateMetadataSessionKeys(
        metadataBundleId: String,
        encryptedData: String,
        modifiedDate: ZonedDateTime,
    ): UpdateMetadataSessionKeysResult {
        val response =
            callWithHandler(responseHandler) {
                metadataApi
                    .updateMetadataSessionKeys(
                        metadataBundleId,
                        EncryptedDataAndModifiedRequest(data = encryptedData, modified = modifiedDate),
                    ).body
            }
        if (response is NetworkResult.Failure<*> && response.errorCode == HTTP_CONFLICT) {
            return UpdateMetadataSessionKeysResult.Conflict
        }
        return when (val result = response.toDomainResult()) {
            is DomainResult.Finished -> UpdateMetadataSessionKeysResult.Success
            is DomainResult.Incomplete -> UpdateMetadataSessionKeysResult.Failure(result)
        }
    }
}
