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

package com.passbolt.mobile.android.domain.resources.actions

import com.passbolt.mobile.android.core.architecture.result.displayMessage
import com.passbolt.mobile.android.domain.permissionsconfirmation.mapper.toEditModePermissions
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import com.passbolt.mobile.android.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import com.passbolt.mobile.android.domain.resources.usecase.CreatePermissionsSnapshotInteractor.DriftOutput
import com.passbolt.mobile.android.domain.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
import com.passbolt.mobile.android.ui.contentType
import kotlinx.coroutines.flow.single
import timber.log.Timber

class ResourceShareActionsInteractor(
    private val createPermissionsSnapshotInteractor: CreatePermissionsSnapshotInteractor,
    private val getPermissionsSnapshotUseCase: GetPermissionsSnapshotUseCase,
    private val resourceShareInteractor: ResourceShareInteractor,
    private val confirmedRecipientsPublicKeysResolver: ConfirmedRecipientsPublicKeysResolver,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val resourceUpdateActionsInteractorFactory: ResourceUpdateActionsInteractorFactory,
) {
    suspend fun shareWithConfirmedPermissions(
        resourceId: String,
        confirmedPermissions: List<PermissionModelUi>,
    ): ShareActionResult =
        detectPermissionsDrift(resourceId)
            ?: reEncryptV5MetadataWithSharedKey(resourceId)
            ?: applyConfirmedShare(resourceId, confirmedPermissions)

    private suspend fun reEncryptV5MetadataWithSharedKey(resourceId: String): ShareActionResult? {
        val resource = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(resourceId)).resource
        if (!resource.contentType().isV5()) {
            return null
        }
        Timber.d("Re-encrypting the v5 resource metadata with a shared key before the share")
        val updateResult =
            resourceUpdateActionsInteractorFactory
                .create(resource)
                .reEncryptResourceMetadata()
                .single()
        return when (updateResult) {
            is ResourceUpdateActionResult.Success -> null
            is ResourceUpdateActionResult.MetadataKeyModified -> ShareActionResult.MetadataKeyModified(updateResult.keyToTrust)
            is ResourceUpdateActionResult.MetadataKeyDeleted -> ShareActionResult.MetadataKeyDeleted(updateResult.deletedKey)
            is ResourceUpdateActionResult.MetadataKeyVerificationFailure -> ShareActionResult.MetadataKeyVerificationFailure
            is ResourceUpdateActionResult.CryptoFailure -> ShareActionResult.CryptoFailure(updateResult.message)
            is ResourceUpdateActionResult.JsonSchemaValidationFailure ->
                ShareActionResult.SchemaValidationFailure(updateResult.entity)
            is ResourceUpdateActionResult.CannotUpdateWithCurrentConfig -> ShareActionResult.CannotUpdateWithCurrentConfig
            is ResourceUpdateActionResult.Unauthorized -> ShareActionResult.Unauthorized
            is ResourceUpdateActionResult.Failure -> ShareActionResult.ShareFailure(updateResult.message)
            is ResourceUpdateActionResult.FetchFailure -> ShareActionResult.ShareFailure(message = null)
            is ResourceUpdateActionResult.ShareFailure -> ShareActionResult.ShareFailure(updateResult.message)
            is ResourceUpdateActionResult.PermissionsDrifted ->
                ShareActionResult.PermissionsDrifted(updateResult.driftedEntityNames)
        }
    }

    private suspend fun detectPermissionsDrift(resourceId: String): ShareActionResult? =
        when (
            val driftOutput =
                runAuthenticatedOperation {
                    createPermissionsSnapshotInteractor.detectDriftForResource(resourceId)
                }
        ) {
            is DriftOutput.DriftDetected -> ShareActionResult.PermissionsDrifted(driftOutput.driftedEntityNames)
            is DriftOutput.SnapshotMissing -> {
                Timber.e("No stored permissions snapshot present for the drift check - reloading the confirmation")
                ShareActionResult.PermissionsDrifted(driftedEntityNames = emptyList())
            }
            is DriftOutput.Failure -> {
                Timber.e("Unable to verify permissions drift: ${driftOutput.message} - not sharing")
                ShareActionResult.ShareFailure(driftOutput.message)
            }
            is DriftOutput.NoDrift -> {
                Timber.d("No permissions drift detected - applying the confirmed share")
                null
            }
        }

    private suspend fun applyConfirmedShare(
        resourceId: String,
        confirmedPermissions: List<PermissionModelUi>,
    ): ShareActionResult {
        val snapshotPermissions =
            getPermissionsSnapshotUseCase
                .execute(Unit)
                .snapshot
                ?.toEditModePermissions()
                ?: return ShareActionResult.PermissionsDrifted(driftedEntityNames = emptyList())
        val shareOutput =
            runAuthenticatedOperation {
                resourceShareInteractor.simulateAndShareResource(
                    resourceId = resourceId,
                    recipients = confirmedPermissions,
                    recipientsPublicKeys = confirmedRecipientsPublicKeysResolver.resolve(confirmedPermissions),
                    existingPermissions = snapshotPermissions,
                )
            }
        return when (shareOutput) {
            is ResourceShareInteractor.Output.Success -> {
                Timber.d("Confirmed share applied")
                ShareActionResult.Success
            }
            is ResourceShareInteractor.Output.DriftDetected ->
                ShareActionResult.PermissionsDrifted(driftedEntityNames = emptyList())
            is ResourceShareInteractor.Output.SecretDecryptFailure -> ShareActionResult.CryptoFailure(shareOutput.message)
            is ResourceShareInteractor.Output.SecretEncryptFailure -> ShareActionResult.CryptoFailure(shareOutput.message)
            is ResourceShareInteractor.Output.SecretFetchFailure ->
                ShareActionResult.ShareFailure(shareOutput.incomplete.displayMessage())
            is ResourceShareInteractor.Output.ShareFailure -> ShareActionResult.ShareFailure(shareOutput.message)
            is ResourceShareInteractor.Output.SimulateShareFailure -> ShareActionResult.ShareFailure(shareOutput.message)
            is ResourceShareInteractor.Output.Unauthorized -> ShareActionResult.Unauthorized
        }
    }
}

sealed class ShareActionResult {
    data object Success : ShareActionResult()

    data class PermissionsDrifted(
        val driftedEntityNames: List<String>,
    ) : ShareActionResult()

    data class ShareFailure(
        val message: String?,
    ) : ShareActionResult()

    data class CryptoFailure(
        val message: String?,
    ) : ShareActionResult()

    data class SchemaValidationFailure(
        val entity: SchemaEntity,
    ) : ShareActionResult()

    data object CannotUpdateWithCurrentConfig : ShareActionResult()

    data object MetadataKeyVerificationFailure : ShareActionResult()

    data class MetadataKeyModified(
        val keyToTrust: NewMetadataKeyToTrustModel,
    ) : ShareActionResult()

    data class MetadataKeyDeleted(
        val deletedKey: TrustedKeyDeletedModel,
    ) : ShareActionResult()

    data object Unauthorized : ShareActionResult()
}
