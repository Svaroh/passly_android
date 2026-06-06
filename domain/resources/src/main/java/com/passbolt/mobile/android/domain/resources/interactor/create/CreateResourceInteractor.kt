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

package net.svaroh.passly.domain.resources.interactor.create

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.displayMessage
import net.svaroh.passly.core.mvp.authentication.AuthenticatedUseCaseOutput
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.core.mvp.authentication.toAuthenticationState
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.usePassphraseCopy
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.passwordexpiry.usecase.GetPasswordExpirySettingsUseCase
import net.svaroh.passly.domain.privatekey.PrivateKeyRepository
import net.svaroh.passly.domain.resources.ResourcesRepository
import net.svaroh.passly.domain.resources.mapper.toUiModel
import net.svaroh.passly.domain.resourcetypes.usecase.GetResourceTypeIdToSlugMappingUseCase
import net.svaroh.passly.domain.secrets.model.SecretJsonModel
import net.svaroh.passly.dto.request.CreateV4ResourceDto
import net.svaroh.passly.dto.request.CreateV5ResourceDto
import net.svaroh.passly.dto.request.EncryptedSecret
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult
import net.svaroh.passly.mappers.MetadataMapper
import net.svaroh.passly.serializers.gson.MetadataEncryptor
import net.svaroh.passly.serializers.gson.validation.JsonSchemaValidationRunner
import net.svaroh.passly.serializers.jsonschema.SchemaEntity
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.RESOURCE
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.SECRET
import net.svaroh.passly.serializers.validationwrapper.PlainSecretValidationWrapper
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.supportedresourceTypes.SupportedContentTypes
import net.svaroh.passly.ui.CreateResourceModel
import net.svaroh.passly.ui.EncryptedSecretOrError
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.ResourceUiModelWithAttributes
import java.time.ZonedDateTime

class CreateResourceInteractor(
    private val resourcesRepository: ResourcesRepository,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val jsonSchemaValidationRunner: JsonSchemaValidationRunner,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val openPgp: OpenPgp,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val passwordExpirySettingsUseCase: GetPasswordExpirySettingsUseCase,
    private val metadataMapper: MetadataMapper,
    private val metadataEncryptor: MetadataEncryptor,
) {
    suspend fun execute(
        resourceInput: CreateResourceModel,
        secretInput: SecretJsonModel,
    ): Output =
        passphraseMemoryCache.usePassphraseCopy(
            onPassphraseNotPresent = { Output.PasswordExpired },
        ) { passphrase ->
            if (resourceInput.contentType.slug in SupportedContentTypes.v5Slugs) {
                val resourceTypeId = getResourceTypeIdForSlug(resourceInput.contentType.slug)
                secretInput.apply {
                    this.objectType = SecretJsonModel.OBJECT_TYPE
                    this.resourceTypeId = resourceTypeId
                }
                resourceInput.apply {
                    this.metadataJsonModel.objectType = MetadataJsonModel.OBJECT_TYPE
                    this.metadataJsonModel.resourceTypeId = resourceTypeId
                }
            }

            val isSecretValid =
                isSecretValid(
                    PlainSecretValidationWrapper(secretInput.json, resourceInput.contentType)
                        .validationPlainSecret,
                    resourceInput.contentType,
                )
            val isResourceValid = isResourceValid(resourceInput.metadataJsonModel.json, resourceInput.contentType)

            if (isSecretValid && isResourceValid) {
                when (val encryptedSecret = encryptSecret(secretInput.json!!, passphrase)) {
                    is EncryptedSecretOrError.Error -> Output.OpenPgpError(encryptedSecret.message)
                    is EncryptedSecretOrError.EncryptedSecret -> {
                        createResource(resourceInput, encryptedSecret, passphrase)
                    }
                }
            } else {
                if (!isSecretValid) {
                    Output.JsonSchemaValidationFailure(SECRET)
                } else {
                    Output.JsonSchemaValidationFailure(RESOURCE)
                }
            }
        }

    private suspend fun createResource(
        resourceInput: CreateResourceModel,
        encryptedSecret: EncryptedSecretOrError.EncryptedSecret,
        passphrase: ByteArray,
    ): Output {
        val createResourceDto =
            if (SupportedContentTypes.v4Slugs.contains(resourceInput.contentType.slug)) {
                CreateV4ResourceDto(
                    name = resourceInput.metadataJsonModel.name,
                    resourceTypeId = getResourceTypeIdForSlug(resourceInput.contentType.slug),
                    // from API documentation: An array of secrets in object format - exactly one secret must be provided.
                    secrets = listOf(EncryptedSecret(encryptedSecret.userId, encryptedSecret.data)),
                    username = resourceInput.metadataJsonModel.username,
                    uri = resourceInput.metadataJsonModel.uri,
                    description = resourceInput.metadataJsonModel.description,
                    folderParentId = resourceInput.folderId,
                    expiry = getResourceExpiry(resourceInput.contentType),
                )
            } else {
                val encryptedMetadata =
                    metadataEncryptor.encryptMetadata(
                        resourceInput.metadataKeyType!!,
                        resourceInput.metadataKeyId!!,
                        resourceInput.metadataJsonModel.json!!,
                        passphrase,
                    )
                when (encryptedMetadata) {
                    is MetadataEncryptor.Output.Success ->
                        CreateV5ResourceDto(
                            resourceTypeId = getResourceTypeIdForSlug(resourceInput.contentType.slug),
                            // from API documentation: An array of secrets in object format - exactly one secret must be provided.
                            secrets = listOf(EncryptedSecret(encryptedSecret.userId, encryptedSecret.data)),
                            folderParentId = resourceInput.folderId,
                            expiry = getResourceExpiry(resourceInput.contentType),
                            metadata = encryptedMetadata.encryptedMetadata,
                            metadataKeyId = resourceInput.metadataKeyId,
                            metadataKeyType = metadataMapper.mapToDto(resourceInput.metadataKeyType),
                        )
                    is MetadataEncryptor.Output.Failure -> return Output.OpenPgpError(encryptedMetadata.error?.message)
                }
            }

        return when (
            val result = resourcesRepository.createResource(createResourceDto, resourceInput.contentType.slug)
        ) {
            is DomainResult.Incomplete -> Output.Failure(result)
            is DomainResult.Finished -> Output.Success(result.value.toUiModel())
        }
    }

    @Suppress("NestedBlockDepth")
    // https://drive.google.com/file/d/1lqiF0ajpuvx1xaZ74aSSjxiDLMGPBXVa/view?usp=drive_link
    private suspend fun getResourceExpiry(contentType: ContentType): ZonedDateTime? =
        if (SupportedContentTypes.resourcesSlugsSupportingExpiry.contains(contentType)) {
            val expirySettings = passwordExpirySettingsUseCase.execute(Unit)
            if (expirySettings.automaticUpdate) {
                if (expirySettings.defaultExpiryPeriodDays != null) {
                    ZonedDateTime
                        .now()
                        .plusDays(expirySettings.defaultExpiryPeriodDays!!.toLong())
                        .withFixedOffsetZone()
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }

    private suspend fun encryptSecret(
        secret: String,
        passphrase: ByteArray,
    ): EncryptedSecretOrError {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val userServerId = requireNotNull(getSelectedAccountDataUseCase.execute(Unit).serverId)
        val privateKey = requireNotNull(privateKeyRepository.getPrivateKey(userId)) { "Unable to restore private key." }.armoredKey

        return when (
            val encryptedSecret =
                openPgp.encryptSignMessageArmored(privateKey, passphrase, secret)
        ) {
            is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
            is OpenPgpResult.Result ->
                EncryptedSecretOrError.EncryptedSecret(
                    userServerId,
                    encryptedSecret.result,
                )
        }
    }

    private suspend fun isSecretValid(
        plainSecretJson: String?,
        type: ContentType,
    ) = plainSecretJson != null && jsonSchemaValidationRunner.isSecretValid(plainSecretJson, type.slug)

    private suspend fun isResourceValid(
        plainResourceMetadataJson: String?,
        type: ContentType,
    ) = plainResourceMetadataJson != null &&
        jsonSchemaValidationRunner.isResourceValid(plainResourceMetadataJson, type.slug)

    private suspend fun getResourceTypeIdForSlug(slug: String) =
        getResourceTypeIdToSlugMappingUseCase
            .execute(Unit)
            .idToSlugMapping
            .filterValues { it == slug }
            .keys
            .first()
            .toString()

    sealed class Output : AuthenticatedUseCaseOutput {
        override val authenticationState: AuthenticationState
            get() =
                when (this) {
                    is Failure -> incomplete.toAuthenticationState()
                    is PasswordExpired ->
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Passphrase,
                        )
                    else -> AuthenticationState.Authenticated
                }

        data class Success(
            val resource: ResourceUiModelWithAttributes,
        ) : Output()

        data class Failure(
            val incomplete: DomainResult.Incomplete,
        ) : Output() {
            val message: String?
                get() = incomplete.displayMessage()
        }

        data object PasswordExpired : Output()

        data class OpenPgpError(
            val message: String?,
        ) : Output()

        data class JsonSchemaValidationFailure(
            val entity: SchemaEntity,
        ) : Output()
    }
}
