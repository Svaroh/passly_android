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

package com.passbolt.mobile.android.domain.resources.interactor.update

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.displayMessage
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.toAuthenticationState
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.passwordexpiry.usecase.GetPasswordExpirySettingsUseCase
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.domain.resources.ResourcesRepository
import com.passbolt.mobile.android.domain.resources.mapper.toUiModel
import com.passbolt.mobile.android.domain.resourcetypes.usecase.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.decrypt.SecretInput
import com.passbolt.mobile.android.domain.users.usecase.FetchUsersUseCase
import com.passbolt.mobile.android.dto.request.CreateV4ResourceDto
import com.passbolt.mobile.android.dto.request.CreateV5ResourceDto
import com.passbolt.mobile.android.dto.request.EncryptedSecret
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.MetadataMapper
import com.passbolt.mobile.android.serializers.gson.MetadataEncryptor
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.RESOURCE
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.SECRET
import com.passbolt.mobile.android.serializers.validationwrapper.PlainSecretValidationWrapper
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.ResourceUiModel
import com.passbolt.mobile.android.ui.UpdateResourceModel
import com.passbolt.mobile.android.ui.UserUiModel
import timber.log.Timber
import java.time.ZonedDateTime

class UpdateResourceInteractor(
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val resourcesRepository: ResourcesRepository,
    private val fetchUsersUseCase: FetchUsersUseCase,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val jsonSchemaValidationRunner: JsonSchemaValidationRunner,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val openPgp: OpenPgp,
    private val passwordExpirySettingsUseCase: GetPasswordExpirySettingsUseCase,
    private val metadataMapper: MetadataMapper,
    private val metadataEncryptor: MetadataEncryptor,
) {
    @Suppress("ReturnCount")
    suspend fun execute(
        resourceInput: UpdateResourceModel,
        secretInput: SecretInput,
        confirmedRecipientsPublicKeys: Map<String, String> = emptyMap(),
    ): Output {
        val passphrase =
            when (val result = passphraseMemoryCache.get()) {
                is PotentialPassphrase.Passphrase -> result.passphrase
                is PotentialPassphrase.PassphraseNotPresent -> return Output.PasswordExpired
            }

        if (!isSecretValid(
                PlainSecretValidationWrapper(secretInput.secretJsonModel.json, resourceInput.contentType)
                    .validationPlainSecret,
                resourceInput.contentType,
            )
        ) {
            return Output.JsonSchemaValidationFailure(SECRET)
        }
        if (!isResourceValid(resourceInput.metadataJsonModel.json, resourceInput.contentType)) {
            return Output.JsonSchemaValidationFailure(RESOURCE)
        }

        return if (secretInput.secretChanged) {
            updateWithChangedSecret(resourceInput, secretInput, passphrase, confirmedRecipientsPublicKeys)
        } else {
            updateWithUnchangedSecret(resourceInput, secretInput, passphrase)
        }
    }

    private suspend fun updateWithUnchangedSecret(
        resourceInput: UpdateResourceModel,
        secretInput: SecretInput,
        passphrase: ByteArray,
    ): Output {
        Timber.d("Secret not changed - updating the resource without the secrets payload")
        return updateResource(secretInput, passphrase, resourceInput, secrets = null)
    }

    private suspend fun updateWithChangedSecret(
        resourceInput: UpdateResourceModel,
        secretInput: SecretInput,
        passphrase: ByteArray,
        confirmedRecipientsPublicKeys: Map<String, String>,
    ): Output =
        when (
            val usersWhoHaveAccess =
                fetchUsersUseCase.execute(FetchUsersUseCase.Input(listOf(resourceInput.resourceId)))
        ) {
            is FetchUsersUseCase.Output.Failure -> Output.Failure(usersWhoHaveAccess.incomplete)
            is FetchUsersUseCase.Output.Success -> {
                val hasUnconfirmedRecipients =
                    confirmedRecipientsPublicKeys.isNotEmpty() &&
                        usersWhoHaveAccess.users.any { it.id !in confirmedRecipientsPublicKeys }
                if (hasUnconfirmedRecipients) {
                    Timber.e("Resource has recipients that were not confirmed - aborting the update")
                    Output.OpenPgpError("Resource has recipients that were not confirmed")
                } else {
                    encryptSecretsAndUpdateResource(
                        secretInput,
                        passphrase,
                        usersWhoHaveAccess.users,
                        resourceInput,
                        confirmedRecipientsPublicKeys,
                    )
                }
            }
        }

    private suspend fun encryptSecretsAndUpdateResource(
        secretInput: SecretInput,
        passphrase: ByteArray,
        usersWhoHaveAccess: List<UserUiModel>,
        resourceInput: UpdateResourceModel,
        confirmedRecipientsPublicKeys: Map<String, String>,
    ): Output {
        val encryptedSecrets =
            encrypt(secretInput.secretJsonModel.json!!, passphrase, usersWhoHaveAccess, confirmedRecipientsPublicKeys)
        return if (encryptedSecrets.any { it is EncryptedSecretOrError.Error }) {
            Output.OpenPgpError(
                encryptedSecrets.filterIsInstance<EncryptedSecretOrError.Error>().first().message,
            )
        } else {
            updateResource(
                secretInput,
                passphrase,
                resourceInput,
                secrets =
                    encryptedSecrets
                        .filterIsInstance<EncryptedSecretOrError.EncryptedSecret>()
                        .map { EncryptedSecret(it.userId, it.data) },
            )
        }
    }

    @Suppress("LongMethod")
    private suspend fun updateResource(
        secretInput: SecretInput,
        passphrase: ByteArray,
        resourceInput: UpdateResourceModel,
        secrets: List<EncryptedSecret>?,
    ): Output {
        val createResourceDto =
            if (SupportedContentTypes.v4Slugs.contains(resourceInput.contentType.slug)) {
                CreateV4ResourceDto(
                    name = resourceInput.metadataJsonModel.name,
                    resourceTypeId = getResourceTypeIdForSlug(resourceInput.contentType.slug),
                    secrets = secrets,
                    username = resourceInput.metadataJsonModel.username,
                    uri = resourceInput.metadataJsonModel.uri,
                    description = resourceInput.metadataJsonModel.description,
                    folderParentId = resourceInput.folderId,
                    expiry = getResourceExpiry(resourceInput, secretInput),
                )
            } else {
                resourceInput.apply {
                    this.metadataJsonModel.objectType = MetadataJsonModel.OBJECT_TYPE
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
                            secrets = secrets,
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
            val result =
                resourcesRepository.updateResource(
                    resourceInput.resourceId,
                    createResourceDto,
                    resourceInput.contentType.slug,
                )
        ) {
            is DomainResult.Incomplete -> Output.Failure(result)
            is DomainResult.Finished -> Output.Success(result.value.toUiModel())
        }
    }

    @Suppress("NestedBlockDepth")
    // https://drive.google.com/file/d/1lqiF0ajpuvx1xaZ74aSSjxiDLMGPBXVa/view?usp=drive_link
    private suspend fun getResourceExpiry(
        resourceInput: UpdateResourceModel,
        secretInput: SecretInput,
    ): ZonedDateTime? =
        if (SupportedContentTypes.resourcesSlugsSupportingExpiry.contains(resourceInput.contentType)) {
            val expirySettings = passwordExpirySettingsUseCase.execute(Unit)
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
        usersWhoHaveAccess: List<UserUiModel>,
        confirmedRecipientsPublicKeys: Map<String, String>,
    ): List<EncryptedSecretOrError> =
        usersWhoHaveAccess.mapTo(mutableListOf()) {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val privateKey = requireNotNull(privateKeyRepository.getPrivateKey(userId)) { "Unable to restore private key." }.armoredKey
            val publicKey = confirmedRecipientsPublicKeys[it.id] ?: it.gpgKey.armoredKey

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
                when (this) {
                    is Failure -> incomplete.toAuthenticationState()
                    is PasswordExpired ->
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Passphrase,
                        )
                    else -> AuthenticationState.Authenticated
                }

        data class Success(
            val resource: ResourceUiModel,
        ) : Output()

        data class Failure(
            val incomplete: DomainResult.Incomplete,
        ) : Output() {
            val message: String?
                get() = incomplete.displayMessage()
        }

        data object PasswordExpired : Output()

        data class OpenPgpError(
            val message: String,
        ) : Output()

        data class JsonSchemaValidationFailure(
            val entity: SchemaEntity,
        ) : Output()
    }
}
