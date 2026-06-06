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

package net.svaroh.passly.permissions.permissions

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.datarefresh.DataRefreshStatus
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.common.validation.validation
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.fulldatarefresh.HomeDataInteractor
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.resources.actions.ResourceUpdateActionsInteractorFactory
import net.svaroh.passly.core.resources.actions.performResourceUpdateAction
import net.svaroh.passly.core.resources.usecase.ResourceShareInteractor
import net.svaroh.passly.core.resources.usecase.ResourceShareInteractor.Output
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.metadata.usecase.CanShareResourceUseCase
import net.svaroh.passly.permissions.permissions.PermissionsIntent.AddPermission
import net.svaroh.passly.permissions.permissions.PermissionsIntent.DismissMetadataKeyDeletedDialog
import net.svaroh.passly.permissions.permissions.PermissionsIntent.DismissMetadataKeyModifiedDialog
import net.svaroh.passly.permissions.permissions.PermissionsIntent.GoBack
import net.svaroh.passly.permissions.permissions.PermissionsIntent.GroupPermissionDeleted
import net.svaroh.passly.permissions.permissions.PermissionsIntent.GroupPermissionModified
import net.svaroh.passly.permissions.permissions.PermissionsIntent.MainButtonIntent
import net.svaroh.passly.permissions.permissions.PermissionsIntent.SeePermission
import net.svaroh.passly.permissions.permissions.PermissionsIntent.ShareRecipientsAdded
import net.svaroh.passly.permissions.permissions.PermissionsIntent.TrustNewMetadataKey
import net.svaroh.passly.permissions.permissions.PermissionsIntent.TrustedMetadataKeyDeleted
import net.svaroh.passly.permissions.permissions.PermissionsIntent.UserPermissionDeleted
import net.svaroh.passly.permissions.permissions.PermissionsIntent.UserPermissionModified
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.CloseWithShareSuccess
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateBack
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateToGroupPermissionDetails
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateToHome
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateToSelectShareRecipients
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateToSelfWithMode
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.NavigateToUserPermissionDetails
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.ShowContentNotAvailable
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.permissions.permissions.PermissionsSideEffect.ShowSuccessSnackbar
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.CANNOT_SHARE_RESOURCE
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.CANNOT_UPDATE_TOTP_WITH_CURRENT_CONFIG
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.DATA_REFRESH_ERROR
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.ENCRYPTION_ERROR
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.FAILED_TO_VERIFY_METADATA_KEY
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.GENERIC_ERROR
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.JSON_RESOURCE_SCHEMA_ERROR
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.JSON_SECRET_SCHEMA_ERROR
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.ONE_OWNER_REQUIRED
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.SECRET_DECRYPT_FAILURE
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.SECRET_ENCRYPT_FAILURE
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.SECRET_FETCH_FAILURE
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.SHARE_FAILED
import net.svaroh.passly.permissions.permissions.SnackbarErrorType.SHARE_SIMULATION_FAILED
import net.svaroh.passly.permissions.permissions.validation.HasAtLeastOneOwnerPermission
import net.svaroh.passly.serializers.jsonschema.SchemaEntity
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.RESOURCE
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.SECRET
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel
import net.svaroh.passly.ui.PermissionsItem
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.PermissionsMode.EDIT
import net.svaroh.passly.ui.PermissionsMode.VIEW
import net.svaroh.passly.ui.ResourcePermission
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

class PermissionsViewModel(
    permissionsItem: PermissionsItem,
    id: String,
    mode: PermissionsMode,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getLocalFolderUseCase: GetLocalFolderDetailsUseCase,
    private val permissionModelUiComparator: PermissionModelUiComparator,
    private val resourceShareInteractor: ResourceShareInteractor,
    private val homeDataInteractor: HomeDataInteractor,
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    private val canShareResourceUseCase: CanShareResourceUseCase,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val resourceUpdateActionsInteractorFactory: ResourceUpdateActionsInteractorFactory,
) : SideEffectViewModel<PermissionsState, PermissionsSideEffect>(
        initialState =
            PermissionsState(
                permissionsItem = permissionsItem,
                permissionItemId = id,
                mode = mode,
            ),
    ) {
    private val missingItemHandler =
        CoroutineExceptionHandler { _, throwable ->
            if (throwable is NullPointerException) {
                emitSideEffect(ShowContentNotAvailable)
                emitSideEffect(NavigateToHome)
            }
        }

    init {
        loadInitialPermissions()
        processItemsVisibility()
        viewModelScope.launch(coroutineLaunchContext.ui) {
            synchronizeWithDataRefresh()
        }
    }

    fun onIntent(intent: PermissionsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            is SeePermission -> permissionClick(intent.permission)
            MainButtonIntent -> actionButtonClick()
            AddPermission -> addPermissionClick()
            is ShareRecipientsAdded -> shareRecipientsAdded(intent.recipients)
            is UserPermissionModified -> userPermissionModified(intent.permission)
            is UserPermissionDeleted -> userPermissionDeleted(intent.permission)
            is GroupPermissionModified -> groupPermissionModified(intent.permission)
            is GroupPermissionDeleted -> groupPermissionDeleted(intent.permission)
            TrustNewMetadataKey -> trustNewMetadataKey()
            TrustedMetadataKeyDeleted -> trustedMetadataKeyDeleted()
            DismissMetadataKeyModifiedDialog ->
                updateViewState { copy(showMetadataKeyModifiedDialog = false, newMetadataKeyToTrustModel = null) }
            DismissMetadataKeyDeletedDialog ->
                updateViewState { copy(showMetadataKeyDeletedDialog = false, trustedKeyDeletedModel = null) }
        }
    }

    private suspend fun synchronizeWithDataRefresh() {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect { status ->
            when (status) {
                DataRefreshStatus.InProgress -> { // no-op
                }
                DataRefreshStatus.Idle.FinishedWithFailure ->
                    emitSideEffect(ShowErrorSnackbar(DATA_REFRESH_ERROR))
                DataRefreshStatus.Idle.FinishedWithSuccess ->
                    viewModelScope.launch(coroutineLaunchContext.io) { reloadPermissions() }
                DataRefreshStatus.Idle.NotCompleted -> { // no-op
                }
            }
        }
    }

    private fun loadInitialPermissions() {
        viewModelScope.launch(missingItemHandler + coroutineLaunchContext.io) {
            if (viewState.value.permissions.isEmpty()) {
                val fetched = fetchPermissions()
                updatePermissions { fetched }
            } else {
                updatePermissions()
            }
        }
    }

    private suspend fun reloadPermissions() {
        val fetched = fetchPermissions()
        updatePermissions { fetched }
    }

    private suspend fun fetchPermissions(): List<PermissionModelUi> =
        when (viewState.value.permissionsItem) {
            PermissionsItem.RESOURCE ->
                getLocalResourcePermissionsUseCase
                    .execute(GetLocalResourcePermissionsUseCase.Input(viewState.value.permissionItemId))
                    .permissions
            PermissionsItem.FOLDER ->
                getLocalFolderPermissionsUseCase
                    .execute(GetLocalFolderPermissionsUseCase.Input(viewState.value.permissionItemId))
                    .permissions
        }

    private fun updatePermissions(transform: (List<PermissionModelUi>) -> List<PermissionModelUi> = { it }) {
        updateViewState {
            val sorted = transform(permissions).sortedWith(permissionModelUiComparator)
            copy(
                permissions = sorted,
                showEmptyState = sorted.isEmpty(),
            )
        }
    }

    private fun processItemsVisibility() {
        val mode = viewState.value.mode
        when (mode) {
            VIEW -> {
                viewModelScope.launch(missingItemHandler + coroutineLaunchContext.io) {
                    val isOwner =
                        when (viewState.value.permissionsItem) {
                            PermissionsItem.RESOURCE ->
                                getLocalResourceUseCase
                                    .execute(GetLocalResourceUseCase.Input(viewState.value.permissionItemId))
                                    .resource.permission == ResourcePermission.OWNER
                            PermissionsItem.FOLDER ->
                                getLocalFolderUseCase
                                    .execute(GetLocalFolderDetailsUseCase.Input(viewState.value.permissionItemId))
                                    .folder.permission == ResourcePermission.OWNER
                        }
                    if (isOwner && viewState.value.permissionsItem == PermissionsItem.RESOURCE) {
                        updateViewState { copy(showEditButton = true) }
                    }
                }
            }
            EDIT -> {
                updateViewState {
                    copy(showAddUserButton = true, showSaveButton = true)
                }
            }
        }
    }

    private fun permissionClick(permission: PermissionModelUi) {
        val mode = viewState.value.mode
        when (permission) {
            is GroupPermissionModel -> emitSideEffect(NavigateToGroupPermissionDetails(permission, mode))
            is UserPermissionModel -> emitSideEffect(NavigateToUserPermissionDetails(permission, mode))
        }
    }

    private fun actionButtonClick() {
        when (viewState.value.mode) {
            VIEW ->
                onCanShareResource {
                    emitSideEffect(NavigateToSelfWithMode(viewState.value.permissionItemId, EDIT, viewState.value.permissionsItem))
                }
            EDIT -> validatePermissions()
        }
    }

    private fun onCanShareResource(action: () -> Unit) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            if (canShareResourceUseCase.execute(Unit).canShareResource) {
                action()
            } else {
                emitSideEffect(ShowErrorSnackbar(CANNOT_SHARE_RESOURCE))
            }
        }
    }

    private fun validatePermissions() {
        validation {
            of(viewState.value.permissions) {
                withRules(HasAtLeastOneOwnerPermission) {
                    onInvalid { emitSideEffect(ShowErrorSnackbar(ONE_OWNER_REQUIRED)) }
                }
            }
            onValid {
                updateIfNeededAndShareResource()
            }
        }
    }

    private fun updateIfNeededAndShareResource() {
        updateViewState { copy(showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            val resource = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(viewState.value.permissionItemId)).resource
            val contentType =
                ContentType.fromSlug(
                    resourceTypeIdToSlugMappingProvider.provideMappingForSelectedAccount()[
                        UUID.fromString(resource.resourceTypeId),
                    ]!!,
                )
            val resourceUpdateActionsInteractor = resourceUpdateActionsInteractorFactory.create(resource)

            if (contentType.isV5()) {
                performResourceUpdateAction(
                    action = { resourceUpdateActionsInteractor.reEncryptResourceMetadata() },
                    doOnFailure = { emitSideEffect(ShowErrorSnackbar(GENERIC_ERROR)) },
                    doOnCryptoFailure = { emitSideEffect(ShowErrorSnackbar(ENCRYPTION_ERROR)) },
                    doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                    doOnSuccess = { viewModelScope.launch(coroutineLaunchContext.io) { shareResource() } },
                    doOnCannotEditWithCurrentConfig = {
                        emitSideEffect(ShowErrorSnackbar(CANNOT_UPDATE_TOTP_WITH_CURRENT_CONFIG))
                    },
                    doOnMetadataKeyModified = {
                        updateViewState {
                            copy(showMetadataKeyModifiedDialog = true, newMetadataKeyToTrustModel = it)
                        }
                    },
                    doOnMetadataKeyDeleted = {
                        updateViewState {
                            copy(showMetadataKeyDeletedDialog = true, trustedKeyDeletedModel = it)
                        }
                    },
                    doOnMetadataKeyVerificationFailure = {
                        emitSideEffect(ShowErrorSnackbar(FAILED_TO_VERIFY_METADATA_KEY))
                    },
                    doOnFinish = { updateViewState { copy(showProgress = false) } },
                )
            } else {
                updateViewState { copy(showProgress = false) }
                shareResource()
            }
        }
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            RESOURCE -> emitSideEffect(ShowErrorSnackbar(JSON_RESOURCE_SCHEMA_ERROR))
            SECRET -> emitSideEffect(ShowErrorSnackbar(JSON_SECRET_SCHEMA_ERROR))
        }
    }

    private suspend fun shareResource() {
        updateViewState { copy(showProgress = true) }
        when (
            runAuthenticatedOperation {
                resourceShareInteractor.simulateAndShareResource(viewState.value.permissionItemId, viewState.value.permissions).also {
                    updateViewState { copy(showProgress = false) }
                }
            }
        ) {
            is Output.SecretDecryptFailure -> emitSideEffect(ShowErrorSnackbar(SECRET_DECRYPT_FAILURE))
            is Output.SecretEncryptFailure -> emitSideEffect(ShowErrorSnackbar(SECRET_ENCRYPT_FAILURE))
            is Output.SecretFetchFailure -> emitSideEffect(ShowErrorSnackbar(SECRET_FETCH_FAILURE))
            is Output.ShareFailure -> emitSideEffect(ShowErrorSnackbar(SHARE_FAILED))
            is Output.SimulateShareFailure -> emitSideEffect(ShowErrorSnackbar(SHARE_SIMULATION_FAILED))
            is Output.Success -> shareSuccess()
            is Output.Unauthorized -> { // not interested
            }
        }
    }

    private suspend fun shareSuccess() {
        updateViewState { copy(showProgress = true) }

        // TODO change to service refresh?
        runAuthenticatedOperation {
            homeDataInteractor.refreshAllHomeScreenData()
        }
        updateViewState { copy(showProgress = false) }
        emitSideEffect(CloseWithShareSuccess)
    }

    private fun addPermissionClick() {
        val permissions = viewState.value.permissions
        emitSideEffect(
            NavigateToSelectShareRecipients(
                permissions.filterIsInstance<GroupPermissionModel>(),
                permissions.filterIsInstance<UserPermissionModel>(),
            ),
        )
    }

    private fun shareRecipientsAdded(shareRecipients: List<PermissionModelUi>?) {
        shareRecipients?.let { newRecipients ->
            updatePermissions { newRecipients.toList() }
        }
    }

    private fun userPermissionModified(permission: UserPermissionModel) {
        updatePermissions { current ->
            current.map { existing ->
                if (existing is UserPermissionModel &&
                    existing.user.userId == permission.user.userId
                ) {
                    UserPermissionModel(
                        permission.permission,
                        permission.permissionId,
                        existing.user.copy(),
                    )
                } else {
                    existing
                }
            }
        }
    }

    private fun userPermissionDeleted(permission: UserPermissionModel) {
        updatePermissions { current ->
            current.filterNot {
                it is UserPermissionModel && it.user.userId == permission.user.userId
            }
        }
    }

    private fun groupPermissionModified(permission: GroupPermissionModel) {
        updatePermissions { current ->
            current.map { existing ->
                if (existing is GroupPermissionModel &&
                    existing.group.groupId == permission.group.groupId
                ) {
                    GroupPermissionModel(
                        permission.permission,
                        permission.permissionId,
                        existing.group.copy(),
                    )
                } else {
                    existing
                }
            }
        }
    }

    private fun groupPermissionDeleted(permission: GroupPermissionModel) {
        updatePermissions { current ->
            current.filterNot {
                it is GroupPermissionModel && it.group.groupId == permission.group.groupId
            }
        }
    }

    private fun trustedMetadataKeyDeleted() {
        updateViewState { copy(showMetadataKeyDeletedDialog = false, trustedKeyDeletedModel = null) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
        }
    }

    private fun trustNewMetadataKey() {
        val model = viewState.value.newMetadataKeyToTrustModel ?: return
        updateViewState { copy(showMetadataKeyModifiedDialog = false, newMetadataKeyToTrustModel = null) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState { copy(showProgress = true) }
            when (
                val output =
                    runAuthenticatedOperation {
                        metadataPrivateKeysHelperInteractor.trustNewKey(model)
                    }
            ) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    emitSideEffect(ShowSuccessSnackbar(SnackbarSuccessType.METADATA_KEY_IS_TRUSTED))
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.FAILED_TO_TRUST_METADATA_KEY))
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }
}
