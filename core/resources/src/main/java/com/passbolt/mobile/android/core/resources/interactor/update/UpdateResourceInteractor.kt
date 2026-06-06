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

package net.svaroh.passly.core.resources.interactor.update

import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.privatekey.GetPrivateKeyUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.mvp.authentication.AuthenticatedUseCaseOutput
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.core.networking.MfaTypeProvider
import net.svaroh.passly.core.networking.NetworkResult
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.core.policies.usecase.GetPasswordExpirySettingsUseCase
import net.svaroh.passly.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import net.svaroh.passly.core.secrets.usecase.decrypt.SecretInput
import net.svaroh.passly.core.users.usecase.FetchUsersUseCase
import net.svaroh.passly.dto.request.CreateV4ResourceDto
import net.svaroh.passly.dto.request.CreateV5ResourceDto
import net.svaroh.passly.dto.request.EncryptedSecret
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult
import net.svaroh.passly.mappers.MetadataMapper
import net.svaroh.passly.mappers.ResourceModelMapper
import net.svaroh.passly.passboltapi.resource.ResourceRepository
import net.svaroh.passly.serializers.gson.MetadataEncryptor
import net.svaroh.passly.serializers.gson.validation.JsonSchemaValidationRunner
import net.svaroh.passly.serializers.jsonschema.SchemaEntity
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.RESOURCE
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.SECRET
import net.svaroh.passly.serializers.validationwrapper.PlainSecretValidationWrapper
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.supportedresourceTypes.SupportedContentTypes
import net.svaroh.passly.ui.EncryptedSecretOrError
import net.svaroh.passly.ui.ResourceModel
import net.svaroh.passly.ui.UpdateResourceModel
import net.svaroh.passly.ui.UserModel
import java.time.ZonedDateTime

class UpdateResourceInteractor(
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val resourceModelMapper: ResourceModelMapper,
    private val resourceRepository: ResourceRepository,
    private val fetchUsersUseCase: FetchUsersUseCase,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val jsonSchemaValidationRunner: JsonSchemaValidationRunner,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    private val passwordExpirySettingsUseCase: GetPasswordExpirySettingsUseCase,
    private val metadataMapper: MetadataMapper,
    private val metadataEncryptor: MetadataEncryptor,
) {
    suspend fun execute(
        resourceInput: UpdateResourceModel,
        secretInput: SecretInput,
    ): Output {
        val passphrase =
            when (val result = passphraseMemoryCache.get()) {
                is PotentialPassphrase.Passphrase -> result.passphrase
                is PotentialPassphrase.PassphraseNotPresent -> return Output.PasswordExpired
            }

        val isSecretValid =
            isSecretValid(
                PlainSecretValidationWrapper(secretInput.secretJsonModel.json, resourceInput.contentType)
                    .validationPlainSecret,
                resourceInput.contentType,
            )
        val isResourceValid = isResourceValid(resourceInput.metadataJsonModel.json, resourceInput.contentType)

        return when (
            val usersWhoHaveAccess =
                fetchUsersUseCase.execute(FetchUsersUseCase.Input(listOf(resourceInput.resourceId)))
        ) {
            is FetchUsersUseCase.Output.Failure<*> -> Output.Failure(usersWhoHaveAccess.response)
            is FetchUsersUseCase.Output.Success -> {
                if (isSecretValid && isResourceValid) {
                    updateResource(secretInput, passphrase, usersWhoHaveAccess.users, resourceInput)
                } else {
                    if (!isSecretValid) {
                        Output.JsonSchemaValidationFailure(SECRET)
                    } else {
                        Output.JsonSchemaValidationFailure(RESOURCE)
                    }
                }
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun updateResource(
        secretInput: SecretInput,
        passphrase: ByteArray,
        usersWhoHaveAccess: List<UserModel>,
        resourceInput: UpdateResourceModel,
    ): Output {
        val encryptedSecrets = encrypt(secretInput.secretJsonModel.json!!, passphrase, usersWhoHaveAccess)
        return if (encryptedSecrets.any { it is EncryptedSecretOrError.Error }) {
            Output.OpenPgpError(
                encryptedSecrets.filterIsInstance<EncryptedSecretOrError.Error>().first().message,
            )
        } else {
            val secrets = encryptedSecrets.filterIsInstance<EncryptedSecretOrError.EncryptedSecret>()
            val createResourceDto =
                if (SupportedContentTypes.v4Slugs.contains(resourceInput.contentType.slug)) {
                    CreateV4ResourceDto(
                        name = resourceInput.metadataJsonModel.name,
                        resourceTypeId = getResourceTypeIdForSlug(resourceInput.contentType.slug),
                        secrets = secrets.map { EncryptedSecret(it.userId, it.data) },
                        username = resourceInput.metadataJsonModel.username,
                        uri = resourceInput.metadataJsonModel.uri,
                        description = resourceInput.metadataJsonModel.description,
                        folderParentId = resourceInput.folderId,
                        expiry = getResourceExpiry(resourceInput, secretInput),
                    )
                } else {
                    resourceInput.apply {
                        this.metadataJsonModel.objectType = "PASSBOLT_RESOURCE_METADATA"
                        this.metadataJsonModel.resourceTypeId = getResourceTypeIdForSlug(contentType.slug)
                    }

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
                                secrets = secrets.map { EncryptedSecret(it.userId, it.data) },
                                folderParentId = resourceInput.folderId,
                                expiry = getResourceExpiry(resourceInput, secretInput),
                                metadata = encryptedMetadata.encryptedMetadata,
                                metadataKeyId = resourceInput.metadataKeyId,
                                metadataKeyType = metadataMapper.mapToDto(resourceInput.metadataKeyType),
                            )
                        is MetadataEncryptor.Output.Failure -> return Output.OpenPgpError(
                            encryptedMetadata.error?.message.orEmpty(),
                        )
                    }
                }

            return when (
                val response =
                    resourceRepository.updateResource(
                        resourceInput.resourceId,
                        createResourceDto,
                    )
            ) {
                is NetworkResult.Failure -> Output.Failure(response)
                is NetworkResult.Success -> Output.Success(resourceModelMapper.map(response.value.body))
            }
        }
    }

    @Suppress("NestedBlockDepth")
    // https://drive.google.com/file/d/1lqiF0ajpuvx1xaZ74aSSjxiDLMGPBXVa/view?usp=drive_link
    private suspend fun getResourceExpiry(
        resourceInput: UpdateResourceModel,
        secretInput: SecretInput,
    ): ZonedDateTime? =
        if (SupportedContentTypes.resourcesSlugsSupportingExpiry.contains(resourceInput.contentType)) {
            val expirySettings = passwordExpirySettingsUseCase.execute(Unit).expirySettings
            if (expirySettings.automaticUpdate) {
                if (secretInput.passwordChanged) {
                    if (expirySettings.defaultExpiryPeriodDays != null) {
                        ZonedDateTime
                            .now()
                            .plusDays(expirySettings.defaultExpiryPeriodDays!!.toLong())
                            .withFixedOffsetZone()
                    } else {
                        null
                    }
                } else {
                    resourceInput.expiry
                }
            } else {
                resourceInput.expiry
            }
        } else {
            null
        }

    private suspend fun encrypt(
        plainSecret: String,
        passphrase: ByteArray,
        usersWhoHaveAccess: List<UserModel>,
    ): List<EncryptedSecretOrError> =
        usersWhoHaveAccess.mapTo(mutableListOf()) {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey
            val publicKey = it.gpgKey.armoredKey

            when (
                val encryptedSecret =
                    openPgp.encryptSignMessageArmored(publicKey, privateKey, passphrase, plainSecret)
            ) {
                is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
                is OpenPgpResult.Result -> EncryptedSecretOrError.EncryptedSecret(it.id, encryptedSecret.result)
            }
        }

    private suspend fun isSecretValid(
        plainSecret: String?,
        contenType: ContentType,
    ) = plainSecret != null && jsonSchemaValidationRunner.isSecretValid(plainSecret, contenType.slug)

    private suspend fun isResourceValid(
        plainResourceMetadataJson: String?,
        contentType: ContentType,
    ) = plainResourceMetadataJson != null &&
        jsonSchemaValidationRunner.isResourceValid(
            plainResourceMetadataJson,
            contentType.slug,
        )

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
                when {
                    this is Failure<*> && this.response.isUnauthorized -> {
                        AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                    }
                    this is Failure<*> && this.response.isMfaRequired -> {
                        val providers = MfaTypeProvider.get(this.response)
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Mfa(providers),
                        )
                    }
                    this is PasswordExpired ->
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Passphrase,
                        )
                    else -> {
                        AuthenticationState.Authenticated
                    }
                }

        data class Success(
            val resource: ResourceModel,
        ) : Output()

        data class Failure<T : Any>(
            val response: NetworkResult.Failure<T>,
        ) : Output()

        data object PasswordExpired : Output()

        data class OpenPgpError(
            val message: String,
        ) : Output()

        data class JsonSchemaValidationFailure(
            val entity: SchemaEntity,
        ) : Output()
    }
}
