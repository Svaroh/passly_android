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

package net.svaroh.passly.domain.resources.actions

import net.svaroh.passly.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction
import net.svaroh.passly.core.secrets.usecase.db.RemoveLocalSecretUseCase
import net.svaroh.passly.core.secrets.usecase.db.UpsertLocalSecretsUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysInteractor.Output.TrustedKeyDeleted
import net.svaroh.passly.domain.metadata.model.MetadataKeyPurpose.ENCRYPT
import net.svaroh.passly.domain.metadata.usecase.GetMetadataKeysSettingsUseCase
import net.svaroh.passly.domain.metadata.usecase.db.GetLocalMetadataKeysUseCase
import net.svaroh.passly.domain.permissionsconfirmation.mapper.toEditModePermissions
import net.svaroh.passly.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import net.svaroh.passly.domain.resources.interactor.update.UpdateResourceInteractor
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsSnapshotInteractor.DriftOutput
import net.svaroh.passly.domain.resources.usecase.ResourceShareInteractor
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.domain.resources.usecase.db.UpdateLocalResourceUseCase
import net.svaroh.passly.domain.resourcetypes.usecase.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.domain.secrets.model.SecretJsonModel
import net.svaroh.passly.domain.secrets.usecase.decrypt.SecretInput
import net.svaroh.passly.domain.users.usecase.GetLocalCurrentUserUseCase
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.mappers.SharePermissionsModelMapper
import net.svaroh.passly.serializers.jsonschema.SchemaEntity
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordAndDescription
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordString
import net.svaroh.passly.supportedresourceTypes.ContentType.Totp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5Default
import net.svaroh.passly.supportedresourceTypes.ContentType.V5DefaultWithTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5PasswordString
import net.svaroh.passly.supportedresourceTypes.ContentType.V5TotpStandalone
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.MetadataKeyParamsModel
import net.svaroh.passly.ui.MetadataKeyTypeModel
import net.svaroh.passly.ui.NewMetadataKeyToTrustModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel
import net.svaroh.passly.ui.ResourceUiModel
import net.svaroh.passly.ui.TrustedKeyDeletedModel
import net.svaroh.passly.ui.UpdateResourceModel
import net.svaroh.passly.ui.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import timber.log.Timber

class ResourceUpdateActionsInteractor(
    private val existingResource: ResourceUiModel,
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor,
    private val updateResourceInteractor: UpdateResourceInteractor,
    private val resourceTypesUpdateGraph: ResourceTypesUpdatesAdjacencyGraph,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase,
    private val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase,
    private val metadataPrivateKeysInteractor: MetadataPrivateKeysInteractor,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getMetadataKeysSettingsUseCase: GetMetadataKeysSettingsUseCase,
    private val getMetadataKeysUseCase: GetLocalMetadataKeysUseCase,
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val createPermissionsSnapshotInteractor: CreatePermissionsSnapshotInteractor,
    private val getPermissionsSnapshotUseCase: GetPermissionsSnapshotUseCase,
    private val resourceShareInteractor: ResourceShareInteractor,
    private val confirmedRecipientsPublicKeysResolver: ConfirmedRecipientsPublicKeysResolver,
    private val upsertLocalSecretsUseCase: UpsertLocalSecretsUseCase,
    private val removeLocalSecretUseCase: RemoveLocalSecretUseCase,
) {
    suspend fun updateGenericResource(
        newContentType: ContentType,
        metadataModification: (MetadataJsonModel) -> MetadataJsonModel = { it },
        secretModification: (SecretJsonModel) -> SecretJsonModel = { it },
        forceMetadataSharedKey: Boolean = false,
        confirmedRecipientsPublicKeys: Map<String, String> = emptyMap(),
    ): Flow<ResourceUpdateActionResult> =
        if (!isSupported(newContentType)) {
            flowOf(ResourceUpdateActionResult.CannotUpdateWithCurrentConfig)
        } else {
            when (val metadataKeyParams = getMetadataKeysParams(existingResource.folderId, forceMetadataSharedKey)) {
                is MetadataKeyParamsModel.ErrorDuringVerification -> {
                    flowOf(ResourceUpdateActionResult.MetadataKeyVerificationFailure)
                }
                is MetadataKeyParamsModel.NewMetadataKeyToTrust -> {
                    flowOf(ResourceUpdateActionResult.MetadataKeyModified(metadataKeyParams.newMetadataKeyToTrust))
                }
                is MetadataKeyParamsModel.TrustedKeyDeleted -> {
                    flowOf(ResourceUpdateActionResult.MetadataKeyDeleted(metadataKeyParams.trustedKeyDeleted))
                }
                is MetadataKeyParamsModel.ParamsModel -> {
                    updateResource(
                        updateResource = {
                            UpdateResourceModel(
                                contentType = newContentType,
                                resourceId = existingResource.resourceId,
                                folderId = existingResource.folderId,
                                expiry = existingResource.expiry,
                                metadataKeyId = metadataKeyParams.metadataKeyId,
                                metadataKeyType = metadataKeyParams.metadataKeyType,
                                metadataJsonModel = metadataModification(existingResource.metadataJsonModel),
                            )
                        },
                        updateSecret = { decryptedSecret ->
                            val existingResourceContentType = existingResource.contentType()
                            val originalSecretJson = decryptedSecret.json
                            val modifiedSecret = secretModification(decryptedSecret)
                            val passwordChanged =
                                decryptedSecret.getPassword(existingResourceContentType) !=
                                    modifiedSecret.getPassword(
                                        newContentType,
                                    )
                            SecretInput(
                                secretJsonModel = modifiedSecret,
                                passwordChanged = passwordChanged,
                                secretChanged = modifiedSecret.json != originalSecretJson,
                            )
                        },
                        confirmedRecipientsPublicKeys = confirmedRecipientsPublicKeys,
                    )
                }
            }
        }

    suspend fun updateGenericResourceWithConfirmedPermissions(
        updateAction: UpdateAction,
        confirmedPermissions: List<PermissionModelUi>,
        metadataModification: (MetadataJsonModel) -> MetadataJsonModel = { it },
        secretModification: (SecretJsonModel) -> SecretJsonModel = { it },
    ): Flow<ResourceUpdateActionResult> {
        val newContentType =
            resourceTypesUpdateGraph.getResourceTypeSlugAfterUpdate(
                existingResource.slug,
                updateAction,
            )
        return updateGenericResourceWithConfirmedPermissions(
            newContentType,
            confirmedPermissions,
            metadataModification,
            secretModification,
        )
    }

    suspend fun updateGenericResourceWithConfirmedPermissions(
        newContentType: ContentType,
        confirmedPermissions: List<PermissionModelUi>,
        metadataModification: (MetadataJsonModel) -> MetadataJsonModel = { it },
        secretModification: (SecretJsonModel) -> SecretJsonModel = { it },
    ): Flow<ResourceUpdateActionResult> =
        flowOf(
            detectPermissionsDrift()
                ?: confirmedPermissionsDelta(confirmedPermissions)
                    ?.let { delta ->
                        applySafeOrderedConfirmedPermissions(delta, newContentType, metadataModification, secretModification)
                    }
                ?: ResourceUpdateActionResult.PermissionsDrifted(driftedEntityNames = emptyList()),
        )

    private suspend fun applySafeOrderedConfirmedPermissions(
        delta: ConfirmedPermissionsDelta,
        newContentType: ContentType,
        metadataModification: (MetadataJsonModel) -> MetadataJsonModel,
        secretModification: (SecretJsonModel) -> SecretJsonModel,
    ): ResourceUpdateActionResult {
        val confirmedRecipientsPublicKeys = confirmedRecipientsPublicKeysResolver.resolve(delta.confirmedPermissions)
        return revokeUnconfirmedAccess(delta, confirmedRecipientsPublicKeys)
            ?: updateAndGrantConfirmedAdditions(
                delta,
                confirmedRecipientsPublicKeys,
                newContentType,
                metadataModification,
                secretModification,
            )
    }

    private suspend fun updateAndGrantConfirmedAdditions(
        delta: ConfirmedPermissionsDelta,
        confirmedRecipientsPublicKeys: Map<String, String>,
        newContentType: ContentType,
        metadataModification: (MetadataJsonModel) -> MetadataJsonModel,
        secretModification: (SecretJsonModel) -> SecretJsonModel,
    ): ResourceUpdateActionResult {
        val updateResult =
            updateGenericResource(
                newContentType = newContentType,
                metadataModification = metadataModification,
                secretModification = secretModification,
                confirmedRecipientsPublicKeys = confirmedRecipientsPublicKeys,
            ).single()
        return if (updateResult is ResourceUpdateActionResult.Success) {
            grantConfirmedAdditions(delta, confirmedRecipientsPublicKeys) ?: updateResult
        } else {
            updateResult
        }
    }

    private suspend fun detectPermissionsDrift(): ResourceUpdateActionResult? =
        when (
            val driftOutput =
                runAuthenticatedOperation {
                    createPermissionsSnapshotInteractor.detectDriftForResource(existingResource.resourceId)
                }
        ) {
            is DriftOutput.DriftDetected -> ResourceUpdateActionResult.PermissionsDrifted(driftOutput.driftedEntityNames)
            is DriftOutput.SnapshotMissing -> {
                Timber.e("No stored permissions snapshot present for the drift check - reopening the confirmation")
                ResourceUpdateActionResult.PermissionsDrifted(driftedEntityNames = emptyList())
            }
            is DriftOutput.Failure -> {
                Timber.e("Unable to verify permissions drift: ${driftOutput.message} - not updating")
                ResourceUpdateActionResult.ShareFailure(driftOutput.message)
            }
            is DriftOutput.NoDrift -> {
                Timber.d("No permissions drift detected - applying the confirmed permissions")
                null
            }
        }

    private suspend fun confirmedPermissionsDelta(confirmedPermissions: List<PermissionModelUi>): ConfirmedPermissionsDelta? =
        getPermissionsSnapshotUseCase
            .execute(Unit)
            .snapshot
            ?.toEditModePermissions()
            ?.let { snapshotPermissions ->
                val operatorUserId =
                    getLocalCurrentUserUseCase
                        .execute(Unit)
                        .user.id
                ConfirmedPermissionsDelta(
                    snapshotPermissions = snapshotPermissions,
                    retainedPermissions =
                        confirmedPermissions.filterNot {
                            it.permissionId == SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID
                        },
                    confirmedPermissions = confirmedPermissions,
                    operatorSnapshotPermission =
                        snapshotPermissions
                            .filterIsInstance<UserPermissionModel>()
                            .find { it.user.userId == operatorUserId },
                )
            }

    private suspend fun revokeUnconfirmedAccess(
        delta: ConfirmedPermissionsDelta,
        confirmedRecipientsPublicKeys: Map<String, String>,
    ): ResourceUpdateActionResult? =
        if (delta.hasRevocationsOrModifications) {
            Timber.d("Applying the confirmed permission revocations and modifications before the update")
            applyConfirmedPermissionsDelta(
                delta.operatorSafeRetainedPermissions,
                delta.snapshotPermissions,
                confirmedRecipientsPublicKeys,
            )
        } else {
            null
        }

    private suspend fun grantConfirmedAdditions(
        delta: ConfirmedPermissionsDelta,
        confirmedRecipientsPublicKeys: Map<String, String>,
    ): ResourceUpdateActionResult? =
        if (delta.hasNewRecipients || delta.hasOperatorOwnChange) {
            Timber.d("Applying the confirmed additions and the operator's own change after the update")
            applyConfirmedPermissionsDelta(
                delta.confirmedPermissions,
                delta.operatorSafeRetainedPermissions,
                confirmedRecipientsPublicKeys,
            )
        } else {
            null
        }

    private data class ConfirmedPermissionsDelta(
        val snapshotPermissions: List<PermissionModelUi>,
        val retainedPermissions: List<PermissionModelUi>,
        val confirmedPermissions: List<PermissionModelUi>,
        val operatorSnapshotPermission: UserPermissionModel?,
    ) {
        val operatorSafeRetainedPermissions: List<PermissionModelUi>
            get() =
                if (operatorSnapshotPermission == null) {
                    retainedPermissions
                } else {
                    retainedPermissions.filterNot {
                        it is UserPermissionModel && it.user.userId == operatorSnapshotPermission.user.userId
                    } + operatorSnapshotPermission
                }

        val hasOperatorOwnChange: Boolean
            get() =
                operatorSnapshotPermission != null &&
                    confirmedPermissions.none {
                        it.permissionId == operatorSnapshotPermission.permissionId &&
                            it.permission == operatorSnapshotPermission.permission
                    }

        val hasNewRecipients: Boolean
            get() = retainedPermissions.size != confirmedPermissions.size

        val hasRevocationsOrModifications: Boolean
            get() =
                snapshotPermissions.any { snapshotPermission ->
                    operatorSafeRetainedPermissions.none {
                        it.permissionId == snapshotPermission.permissionId && it.permission == snapshotPermission.permission
                    }
                }
    }

    private suspend fun applyConfirmedPermissionsDelta(
        recipients: List<PermissionModelUi>,
        existingPermissions: List<PermissionModelUi>,
        confirmedRecipientsPublicKeys: Map<String, String>,
    ): ResourceUpdateActionResult? =
        when (
            val shareResult =
                runAuthenticatedOperation {
                    resourceShareInteractor.simulateAndShareResource(
                        resourceId = existingResource.resourceId,
                        recipients = recipients,
                        recipientsPublicKeys = confirmedRecipientsPublicKeys,
                        existingPermissions = existingPermissions,
                    )
                }
        ) {
            is ResourceShareInteractor.Output.Success -> null
            is ResourceShareInteractor.Output.DriftDetected ->
                ResourceUpdateActionResult.PermissionsDrifted(driftedEntityNames = emptyList())
            is ResourceShareInteractor.Output.SecretDecryptFailure -> ResourceUpdateActionResult.CryptoFailure(shareResult.message)
            is ResourceShareInteractor.Output.SecretEncryptFailure -> ResourceUpdateActionResult.CryptoFailure(shareResult.message)
            is ResourceShareInteractor.Output.SecretFetchFailure -> ResourceUpdateActionResult.FetchFailure
            is ResourceShareInteractor.Output.ShareFailure -> ResourceUpdateActionResult.ShareFailure(shareResult.message)
            is ResourceShareInteractor.Output.SimulateShareFailure -> ResourceUpdateActionResult.ShareFailure(shareResult.message)
            is ResourceShareInteractor.Output.Unauthorized -> ResourceUpdateActionResult.Unauthorized
        }

    suspend fun upgradeToV5(): Flow<ResourceUpdateActionResult> {
        val currentContentType = existingResource.contentType()
        val targetContentType =
            v5TargetFor(currentContentType)
                ?: return flowOf(ResourceUpdateActionResult.CannotUpdateWithCurrentConfig)
        val targetTypeId =
            findResourceTypeId(targetContentType)
                ?: return flowOf(ResourceUpdateActionResult.CannotUpdateWithCurrentConfig)

        return updateGenericResource(
            newContentType = targetContentType,
            metadataModification = upgradeMetadata(currentContentType, targetContentType, targetTypeId),
            secretModification = upgradeSecret(targetContentType, targetTypeId),
        )
    }

    suspend fun upgradeToV5WithConfirmedPermissions(confirmedPermissions: List<PermissionModelUi>): Flow<ResourceUpdateActionResult> {
        val currentContentType = existingResource.contentType()
        val targetContentType =
            v5TargetFor(currentContentType)
                ?: return flowOf(ResourceUpdateActionResult.CannotUpdateWithCurrentConfig)
        val targetTypeId =
            findResourceTypeId(targetContentType)
                ?: return flowOf(ResourceUpdateActionResult.CannotUpdateWithCurrentConfig)

        return updateGenericResourceWithConfirmedPermissions(
            newContentType = targetContentType,
            confirmedPermissions = confirmedPermissions,
            metadataModification = upgradeMetadata(currentContentType, targetContentType, targetTypeId),
            secretModification = upgradeSecret(targetContentType, targetTypeId),
        )
    }

    fun doesUpgradeToV5ReEncryptSecret(): Boolean =
        v5TargetFor(existingResource.contentType())
            ?.let { it != V5PasswordString } == true

    private fun v5TargetFor(contentType: ContentType): ContentType? =
        when (contentType) {
            PasswordString -> V5PasswordString
            PasswordAndDescription -> V5Default
            PasswordDescriptionTotp -> V5DefaultWithTotp
            Totp -> V5TotpStandalone
            else -> null
        }

    private suspend fun findResourceTypeId(contentType: ContentType): String? =
        resourceTypeIdToSlugMappingProvider
            .provideMappingForSelectedAccount()
            .entries
            .firstOrNull { it.value == contentType.slug }
            ?.key
            ?.toString()

    private fun upgradeMetadata(
        currentContentType: ContentType,
        targetContentType: ContentType,
        targetTypeId: String,
    ): (MetadataJsonModel) -> MetadataJsonModel =
        { metadata ->
            metadata.objectType = MetadataJsonModel.OBJECT_TYPE
            metadata.resourceTypeId = targetTypeId
            val mainUri = metadata.getMainUri(currentContentType)
            if (mainUri.isNotBlank()) {
                metadata.setMainUri(targetContentType, mainUri)
            }
            metadata
        }

    private fun upgradeSecret(
        targetContentType: ContentType,
        targetTypeId: String,
    ): (SecretJsonModel) -> SecretJsonModel =
        { secret ->
            if (targetContentType != V5PasswordString) {
                secret.objectType = SecretJsonModel.OBJECT_TYPE
                secret.resourceTypeId = targetTypeId
            }
            secret
        }

    suspend fun updateGenericResource(
        updateAction: UpdateAction,
        metadataModification: (MetadataJsonModel) -> MetadataJsonModel = { it },
        secretModification: (SecretJsonModel) -> SecretJsonModel = { it },
    ): Flow<ResourceUpdateActionResult> {
        val newContentType =
            resourceTypesUpdateGraph.getResourceTypeSlugAfterUpdate(
                existingResource.slug,
                updateAction,
            )
        return updateGenericResource(newContentType, metadataModification, secretModification)
    }

    suspend fun reEncryptResourceMetadata(): Flow<ResourceUpdateActionResult> =
        updateGenericResource(existingResource.contentType(), forceMetadataSharedKey = true)

    private suspend fun isSupported(contentType: ContentType) =
        resourceTypeIdToSlugMappingProvider
            .provideMappingForSelectedAccount()
            .values
            .contains(contentType.slug)

    private suspend fun shouldPersonalKeyBeUsed(
        parentFolderId: String?,
        forceMetadataSharedKey: Boolean,
    ): Boolean {
        val isPersonalKeyAllowed =
            getMetadataKeysSettingsUseCase
                .execute(Unit)
                .metadataKeysSettingsModel.allowUsageOfPersonalKeys
        val isParentFolderShared =
            parentFolderId?.let {
                getLocalFolderPermissionsUseCase
                    .execute(
                        GetLocalFolderPermissionsUseCase.Input(parentFolderId),
                    ).permissions.size > 1
            } ?: false

        val resourcePermissions =
            getLocalResourcePermissionsUseCase
                .execute(
                    GetLocalResourcePermissionsUseCase.Input(existingResource.resourceId),
                ).permissions
        val isResourceShared =
            resourcePermissions.any { it is GroupPermissionModel } ||
                resourcePermissions
                    .filterIsInstance<UserPermissionModel>()
                    .any { it.user.userId != getLocalCurrentUserUseCase.execute(Unit).user.id }

        return !forceMetadataSharedKey && isPersonalKeyAllowed && !isParentFolderShared && !isResourceShared
    }

    @Suppress("LongMethod")
    private suspend fun getMetadataKeysParams(
        parentFolderId: String?,
        forceMetadataSharedKey: Boolean,
    ): MetadataKeyParamsModel {
        val metadataKeyType =
            if (shouldPersonalKeyBeUsed(parentFolderId, forceMetadataSharedKey)) {
                MetadataKeyTypeModel.PERSONAL
            } else {
                MetadataKeyTypeModel.SHARED
            }

        return when (metadataKeyType) {
            MetadataKeyTypeModel.SHARED -> {
                val verifyOutput =
                    runAuthenticatedOperation {
                        metadataPrivateKeysInteractor.verifyMetadataPrivateKey()
                    }

                when (verifyOutput) {
                    is MetadataPrivateKeysInteractor.Output.Failure -> {
                        MetadataKeyParamsModel.ErrorDuringVerification
                    }
                    is MetadataPrivateKeysInteractor.Output.NewKeyToTrust -> {
                        MetadataKeyParamsModel.NewMetadataKeyToTrust(
                            NewMetadataKeyToTrustModel(
                                id = verifyOutput.metadataPrivateKey.id,
                                signedUsername = verifyOutput.signedUsername,
                                signedName = verifyOutput.signedName,
                                signatureCreationTimestampSeconds = verifyOutput.signatureCreationTimestampSeconds,
                                signatureKeyFingerprint = verifyOutput.signatureKeyFingerprint,
                                metadataPrivateKey = verifyOutput.metadataPrivateKey,
                                modificationKind = verifyOutput.modificationKind,
                            ),
                        )
                    }
                    is TrustedKeyDeleted -> {
                        MetadataKeyParamsModel.TrustedKeyDeleted(
                            TrustedKeyDeletedModel(
                                keyFingerprint = verifyOutput.keyFingerprint,
                                signedUsername = verifyOutput.signedUsername,
                                signedName = verifyOutput.signedName,
                                modificationKind = verifyOutput.modificationKind,
                            ),
                        )
                    }
                    else -> {
                        // for cases when not able to verify (i.e. cannot get user, cannot validate signature)
                        // do not block the user
                        MetadataKeyParamsModel.ParamsModel(
                            metadataKeyId =
                                getMetadataKeysUseCase
                                    .execute(GetLocalMetadataKeysUseCase.Input(ENCRYPT))
                                    .firstOrNull()
                                    ?.id
                                    ?.toString(),
                            metadataKeyType = MetadataKeyTypeModel.SHARED,
                        )
                    }
                }
            }
            MetadataKeyTypeModel.PERSONAL -> {
                MetadataKeyParamsModel.ParamsModel(
                    metadataKeyId =
                        getLocalCurrentUserUseCase
                            .execute(Unit)
                            .user.gpgKey.id,
                    metadataKeyType = MetadataKeyTypeModel.PERSONAL,
                )
            }
        }
    }

    private suspend fun updateResource(
        updateResource: () -> UpdateResourceModel,
        updateSecret: suspend (SecretJsonModel) -> SecretInput,
        confirmedRecipientsPublicKeys: Map<String, String>,
    ): Flow<ResourceUpdateActionResult> =
        try {
            val decryptedSecret = secretPropertiesActionsInteractor.provideDecryptedSecret().single()
            flowOf(
                when (decryptedSecret) {
                    is SecretPropertyActionResult.Success ->
                        runUpdateOperation {
                            updateResourceInteractor.execute(
                                resourceInput = updateResource(),
                                secretInput = updateSecret(decryptedSecret.result),
                                confirmedRecipientsPublicKeys = confirmedRecipientsPublicKeys,
                            )
                        }
                    is SecretPropertyActionResult.FetchFailure ->
                        ResourceUpdateActionResult.FetchFailure
                    is SecretPropertyActionResult.DecryptionFailure ->
                        ResourceUpdateActionResult.CryptoFailure()
                    else -> ResourceUpdateActionResult.Failure()
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "Error updating resource")
            flowOf(ResourceUpdateActionResult.Failure())
        }

    private suspend fun runUpdateOperation(operation: suspend () -> UpdateResourceInteractor.Output): ResourceUpdateActionResult =
        when (
            val operationResult =
                runAuthenticatedOperation {
                    operation()
                }
        ) {
            is UpdateResourceInteractor.Output.Failure -> {
                ResourceUpdateActionResult.Failure(operationResult.message)
            }
            is UpdateResourceInteractor.Output.OpenPgpError -> {
                ResourceUpdateActionResult.CryptoFailure(operationResult.message)
            }
            is UpdateResourceInteractor.Output.PasswordExpired -> {
                ResourceUpdateActionResult.Unauthorized
            }
            is UpdateResourceInteractor.Output.Success -> {
                updateLocalResourceUseCase.execute(
                    UpdateLocalResourceUseCase.Input(operationResult.resource),
                )
                refreshLocalSecret(
                    operationResult.resource.resourceId,
                    operationResult.armoredSecretForCurrentUser,
                )
                ResourceUpdateActionResult.Success(
                    operationResult.resource.resourceId,
                    operationResult.resource.metadataJsonModel.name,
                )
            }
            is UpdateResourceInteractor.Output.JsonSchemaValidationFailure ->
                ResourceUpdateActionResult.JsonSchemaValidationFailure(operationResult.entity)
        }

    /**
     * Replaces the locally stored ciphertext with the block that was just encrypted for this account, so an offline
     * read after an edit returns the new secret rather than the previous one. If the account was somehow not among
     * the recipients, the now-stale copy is dropped instead.
     */
    private suspend fun refreshLocalSecret(
        resourceId: String,
        armoredSecret: String?,
    ) {
        try {
            if (armoredSecret == null) {
                removeLocalSecretUseCase.execute(RemoveLocalSecretUseCase.Input(resourceId))
            } else {
                upsertLocalSecretsUseCase.execute(
                    UpsertLocalSecretsUseCase.Input(
                        secrets =
                            listOf(
                                UpsertLocalSecretsUseCase.LocalSecret(
                                    resourceId = resourceId,
                                    secretId = null,
                                    armoredData = armoredSecret,
                                    modified = null,
                                ),
                            ),
                    ),
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Could not refresh the local copy of the updated secret")
        }
    }
}

@Suppress("LongParameterList")
suspend fun performResourceUpdateAction(
    action: suspend () -> Flow<ResourceUpdateActionResult>,
    doOnCryptoFailure: (String) -> Unit,
    doOnFailure: (String) -> Unit,
    doOnSuccess: (ResourceUpdateActionResult.Success) -> Unit,
    doOnSchemaValidationFailure: (SchemaEntity) -> Unit,
    doOnCannotEditWithCurrentConfig: () -> Unit,
    doOnMetadataKeyModified: (NewMetadataKeyToTrustModel) -> Unit,
    doOnMetadataKeyDeleted: (TrustedKeyDeletedModel) -> Unit,
    doOnFetchFailure: () -> Unit = {},
    doOnUnauthorized: () -> Unit = {},
    doOnMetadataKeyVerificationFailure: () -> Unit = {},
    doOnShareFailure: (String) -> Unit = {},
    doOnPermissionsDrifted: (ResourceUpdateActionResult.PermissionsDrifted) -> Unit = {},
    doOnFinish: () -> Unit = {},
) {
    action().single().let {
        doOnFinish()
        when (it) {
            is ResourceUpdateActionResult.CryptoFailure -> doOnCryptoFailure(it.message.orEmpty())
            is ResourceUpdateActionResult.Failure -> doOnFailure(it.message.orEmpty())
            is ResourceUpdateActionResult.FetchFailure -> doOnFetchFailure()
            is ResourceUpdateActionResult.Success -> doOnSuccess(it)
            is ResourceUpdateActionResult.Unauthorized -> doOnUnauthorized()
            is ResourceUpdateActionResult.JsonSchemaValidationFailure -> doOnSchemaValidationFailure(it.entity)
            is ResourceUpdateActionResult.CannotUpdateWithCurrentConfig -> doOnCannotEditWithCurrentConfig()
            is ResourceUpdateActionResult.MetadataKeyDeleted -> doOnMetadataKeyDeleted(it.deletedKey)
            is ResourceUpdateActionResult.MetadataKeyModified -> doOnMetadataKeyModified(it.keyToTrust)
            ResourceUpdateActionResult.MetadataKeyVerificationFailure -> doOnMetadataKeyVerificationFailure()
            is ResourceUpdateActionResult.ShareFailure -> doOnShareFailure(it.message.orEmpty())
            is ResourceUpdateActionResult.PermissionsDrifted -> doOnPermissionsDrifted(it)
        }
    }
}
