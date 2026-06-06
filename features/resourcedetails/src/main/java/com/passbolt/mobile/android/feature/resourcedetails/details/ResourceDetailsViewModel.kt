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

package net.svaroh.passly.feature.resourcedetails.details

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.coroutinetimer.TimerFactory
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.common.types.ClipboardLabel
import net.svaroh.passly.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.idlingresource.ResourceDetailActionIdlingResource
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.otpcore.TotpParametersProvider
import net.svaroh.passly.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import net.svaroh.passly.core.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.core.resources.actions.ResourceCommonActionsInteractor
import net.svaroh.passly.core.resources.actions.ResourcePropertiesActionsInteractor
import net.svaroh.passly.core.resources.actions.SecretPropertiesActionsInteractor
import net.svaroh.passly.core.resources.actions.performCommonResourceAction
import net.svaroh.passly.core.resources.actions.performResourcePropertyAction
import net.svaroh.passly.core.resources.actions.performSecretPropertyAction
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceTagsUseCase
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.entity.featureflags.FeatureFlagsModel
import net.svaroh.passly.feature.resourcedetails.details.ErrorSnackbarType.CANNOT_PERFORM_ACTION
import net.svaroh.passly.feature.resourcedetails.details.ErrorSnackbarType.DECRYPTION_FAILURE
import net.svaroh.passly.feature.resourcedetails.details.ErrorSnackbarType.FETCH_FAILURE
import net.svaroh.passly.feature.resourcedetails.details.ErrorSnackbarType.GENERAL_ERROR
import net.svaroh.passly.feature.resourcedetails.details.ErrorSnackbarType.TOGGLE_FAVOURITE_FAILURE
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CloseDeleteConfirmationDialog
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CloseMoreMenu
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.ConfirmDeleteResource
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CopyCustomField
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CopyMetadataDescription
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CopyNote
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CopyPassword
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CopyTotp
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CopyUrl
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.CopyUsername
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.DeleteClick
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.Dispose
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.Edit
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.EditPermissions
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.GoBack
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.GoToLocation
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.GoToTags
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.Initialize
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.LaunchWebsite
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.OpenMoreMenu
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.ResourceEdited
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.ResourceShared
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.ToggleCustomField
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.ToggleFavourite
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.ToggleNoteVisibility
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.TogglePasswordVisibility
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.ToggleTotpVisibility
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent.ViewPermissions
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.AddToClipboard
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.CloseWithDeleteSuccess
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateBack
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateToEditResource
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateToResourceLocation
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateToResourcePermissions
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateToResourceTags
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.OpenWebsite
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.SetResourceEditedResult
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.ShowSuccessSnackbar
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect.ShowToast
import net.svaroh.passly.feature.resourcedetails.details.SuccessSnackbarType.RESOURCE_EDITED
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import net.svaroh.passly.jsonmodel.delegates.TotpSecret
import net.svaroh.passly.mappers.OtpModelMapper
import net.svaroh.passly.mappers.ResourceFormMapper
import net.svaroh.passly.metadata.usecase.CanShareResourceUseCase
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.ui.CustomFieldModel.BooleanCustomField
import net.svaroh.passly.ui.CustomFieldModel.NumberCustomField
import net.svaroh.passly.ui.CustomFieldModel.PasswordCustomField
import net.svaroh.passly.ui.CustomFieldModel.TextCustomField
import net.svaroh.passly.ui.CustomFieldModel.UriCustomField
import net.svaroh.passly.ui.PermissionsMode
import net.svaroh.passly.ui.RbacModel
import net.svaroh.passly.ui.RbacRuleModel.ALLOW
import net.svaroh.passly.ui.ResourceModel
import net.svaroh.passly.ui.ResourceMoreMenuModel
import net.svaroh.passly.ui.isExpired
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass")
class ResourceDetailsViewModel(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getLocalResourceTagsUseCase: GetLocalResourceTagsUseCase,
    private val getLocalFolderLocation: GetLocalFolderLocationUseCase,
    private val totpParametersProvider: TotpParametersProvider,
    private val otpModelMapper: OtpModelMapper,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    private val resourceDetailActionIdlingResource: ResourceDetailActionIdlingResource,
    private val canShareResourceUseCase: CanShareResourceUseCase,
    private val resourceFormMapper: ResourceFormMapper,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val timerFactory: TimerFactory,
) : SideEffectViewModel<ResourceDetailsState, ResourceDetailsSideEffect>(ResourceDetailsState()),
    KoinComponent {
    private val resourcePropertiesActionsInteractor: ResourcePropertiesActionsInteractor
        get() = get { parametersOf(viewState.value.requiredResourceModel) }
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor
        get() = get { parametersOf(viewState.value.requiredResourceModel) }
    private val resourceCommonActionsInteractor: ResourceCommonActionsInteractor
        get() = get { parametersOf(viewState.value.requiredResourceModel) }

    private val missingItemExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            if (throwable is NullPointerException) {
                emitSideEffect(ShowToast(ToastType.CONTENT_NOT_AVAILABLE))
                emitSideEffect(NavigateBack)
            }
        }

    private var dataRefreshJob: Job? = null
    private var otpTimerJob: Job? = null

    private val resource: ResourceModel
        get() = viewState.value.requiredResourceModel

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: ResourceDetailsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateBack)
            OpenMoreMenu -> openMoreMenu()
            CloseMoreMenu -> closeMoreMenu()
            CopyUsername -> copyUsername()
            CopyUrl -> copyMainUri()
            CopyPassword -> copyPassword()
            CopyMetadataDescription -> copyMetadataDescription()
            CopyNote -> copyNote()
            CopyTotp -> copyTotp()
            is CopyCustomField -> copyCustomField(intent.key)
            TogglePasswordVisibility -> togglePasswordVisibility()
            ToggleNoteVisibility -> toggleNoteVisibility()
            ToggleTotpVisibility -> toggleTotpVisibility()
            is ToggleCustomField -> toggleCustomFieldVisibility(intent.key)
            GoToTags -> goToTags()
            GoToLocation -> goToLocation()
            Edit -> edit()
            ViewPermissions -> viewPermissions()
            EditPermissions -> editPermissions()
            DeleteClick -> updateViewState { copy(showDeleteResourceConfirmationDialog = true) }
            ConfirmDeleteResource -> deleteResource()
            CloseDeleteConfirmationDialog -> updateViewState { copy(showDeleteResourceConfirmationDialog = false) }
            LaunchWebsite -> launchWebsite()
            is ToggleFavourite -> toggleFavourite(intent.option)
            is ResourceEdited -> handleResourceEdited(intent.resourceName)
            ResourceShared -> emitSideEffect(ShowSuccessSnackbar(SuccessSnackbarType.RESOURCE_SHARED))
            Dispose -> dispose()
            is Initialize -> initialize(intent.resourceModel)
        }
    }

    private fun initialize(resourceModel: ResourceModel) {
        updateViewState { copy(resourceData = resourceData.copy(resourceModel = resourceModel)) }

        viewModelScope.launch(coroutineLaunchContext.io + missingItemExceptionHandler) {
            loadResourceDetails()
        }

        dataRefreshJob?.cancel()
        dataRefreshJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                synchronizeWithDataRefresh()
            }

        otpTimerJob?.cancel()
        otpTimerJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                updateOtpCounterTime()
            }
    }

    private suspend fun loadResourceDetails() {
        // Wait for data refresh if in progress and refresh resource afterwards
        if (dataRefreshTrackingFlow.isInProgress()) {
            dataRefreshTrackingFlow.awaitIdle()
            val refreshedResource =
                getLocalResourceUseCase
                    .execute(GetLocalResourceUseCase.Input(resource.resourceId))
                    .resource
            updateViewState { copy(resourceData = resourceData.copy(resourceModel = refreshedResource)) }
        }

        val featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
        val rbac = getRbacRulesUseCase.execute(Unit).rbacModel

        loadAndDisplayResource(rbac, featureFlags)
        loadAndDisplayPermissions(rbac)
        loadAndDisplayTags(rbac, featureFlags)
        loadAndDisplayLocation(rbac)
    }

    private suspend fun loadAndDisplayResource(
        rbac: RbacModel,
        featureFlags: FeatureFlagsModel,
    ) {
        val slug =
            idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(resource.resourceTypeId),
            ]
        val contentType = ContentType.fromSlug(slug!!)

        performResourcePropertyAction(
            action = { resourcePropertiesActionsInteractor.provideMainUri() },
            doOnResult = { updateViewState { copy(metadataData = metadataData.copy(mainUri = it.result)) } },
        )

        performResourcePropertyAction(
            action = { resourcePropertiesActionsInteractor.provideAdditionalUris() },
            doOnResult = { updateViewState { copy(metadataData = metadataData.copy(additionalUris = it.result)) } },
        )

        val customFields =
            resource.metadataJsonModel.customFields?.associate {
                it.id to it.metadataKey.orEmpty()
            } ?: emptyMap()

        updateViewState {
            copy(
                passwordData =
                    passwordData.copy(
                        showPasswordItem = contentType.hasPassword(),
                        showPasswordEyeIcon =
                            featureFlags.isPreviewPasswordAvailable &&
                                rbac.passwordPreviewRule == ALLOW,
                    ),
                metadataData =
                    metadataData.copy(
                        showMetadataDescriptionItem = contentType.hasMetadataDescription(),
                    ),
                customFieldsData =
                    customFieldsData.copy(
                        showCustomFieldsSection = contentType.hasCustomFields() && customFields.isNotEmpty(),
                        customFields = customFields,
                    ),
                totpData =
                    totpData.copy(
                        showTotpSection = contentType.hasTotp(),
                        totpModel = otpModelMapper.map(resource),
                    ),
                noteData =
                    noteData.copy(
                        showNoteSection = contentType.hasNote(),
                    ),
            )
        }
    }

    private suspend fun loadAndDisplayPermissions(rbac: RbacModel) {
        val canViewPermissions = rbac.shareViewRule == ALLOW
        val permissions =
            if (canViewPermissions) {
                getLocalResourcePermissionsUseCase
                    .execute(GetLocalResourcePermissionsUseCase.Input(resource.resourceId))
                    .permissions
            } else {
                emptyList()
            }

        updateViewState {
            copy(
                sharedWithData =
                    sharedWithData.copy(
                        canViewPermissions = canViewPermissions,
                        permissions = permissions,
                    ),
            )
        }
    }

    private suspend fun loadAndDisplayTags(
        rbac: RbacModel,
        featureFlags: FeatureFlagsModel,
    ) {
        val canUseTags = featureFlags.areTagsAvailable && rbac.tagsUseRule == ALLOW
        val tags =
            if (canUseTags) {
                getLocalResourceTagsUseCase
                    .execute(GetLocalResourceTagsUseCase.Input(resource.resourceId))
                    .tags
                    .map { it.slug }
            } else {
                emptyList()
            }

        updateViewState {
            copy(
                metadataData =
                    metadataData.copy(
                        canViewTags = canUseTags,
                        tags = tags,
                    ),
            )
        }
    }

    private suspend fun loadAndDisplayLocation(rbac: RbacModel) {
        val canUseLocation = rbac.foldersUseRule == ALLOW
        val locationPath =
            if (canUseLocation) {
                resource.folderId?.let { folderId ->
                    getLocalFolderLocation
                        .execute(GetLocalFolderLocationUseCase.Input(folderId))
                        .parentFolders
                        .map { folder -> folder.name }
                } ?: emptyList()
            } else {
                emptyList()
            }

        updateViewState {
            copy(
                metadataData =
                    metadataData.copy(
                        canViewLocation = canUseLocation,
                        locationPath = locationPath,
                    ),
            )
        }
    }

    private suspend fun synchronizeWithDataRefresh() {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect { status ->
            when (status) {
                InProgress -> updateViewState { copy(isRefreshing = true) }
                FinishedWithFailure -> {
                    emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.DATA_REFRESH_ERROR))
                    updateViewState { copy(isRefreshing = false) }
                }
                FinishedWithSuccess -> {
                    val refreshedResource =
                        getLocalResourceUseCase
                            .execute(GetLocalResourceUseCase.Input(resource.resourceId))
                            .resource
                    updateViewState {
                        copy(
                            isRefreshing = false,
                            resourceData = resourceData.copy(resourceModel = refreshedResource),
                        )
                    }
                    viewModelScope.launch { loadResourceDetails() }
                }
                NotCompleted -> {
                    // do nothing
                }
            }
        }
    }

    private suspend fun updateOtpCounterTime() {
        timerFactory.createInfiniteTimer(tickDuration = 1.seconds).collectLatest {
            val otpModel = viewState.value.totpData.totpModel
            if (otpModel != null && otpModel.remainingSecondsCounter != null) {
                if (otpModel.isVisible && otpModel.isExpired()) {
                    updateViewState { copy(totpData = totpData.copy(totpModel = totpData.totpModel?.copy(isRefreshing = true))) }
                    doAfterOtpFetchAndDecrypt { _, otp, otpParameters ->
                        updateViewState {
                            copy(
                                isRefreshing = false,
                                totpData =
                                    totpData.copy(
                                        totpModel =
                                            viewState.value.totpData.totpModel?.copy(
                                                otpValue = otpParameters.otpValue,
                                                otpExpirySeconds = otp.period,
                                                remainingSecondsCounter = otpParameters.secondsValid,
                                                isRefreshing = false,
                                            ),
                                    ),
                            )
                        }
                    }
                } else {
                    updateViewState {
                        copy(
                            totpData =
                                totpData.copy(
                                    totpModel = otpModel.copy(remainingSecondsCounter = otpModel.remainingSecondsCounter!! - 1),
                                ),
                        )
                    }
                }
            }
        }
    }

    private fun openMoreMenu() {
        updateViewState {
            copy(
                totpData = totpData.copy(totpModel = otpModelMapper.map(resource)),
                showMoreMenu = true,
            )
        }
    }

    private fun closeMoreMenu() {
        updateViewState { copy(showMoreMenu = false) }
    }

    private fun copyUsername() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideUsername() },
                doOnResult = { emitSideEffect(AddToClipboard(it.label, it.result, it.isSecret)) },
            )
        }
    }

    private fun copyMainUri() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideMainUri() },
                doOnResult = { emitSideEffect(AddToClipboard(it.label, it.result, it.isSecret)) },
            )
        }
    }

    private fun copyPassword() {
        resourceDetailActionIdlingResource.setIdle(false)
        viewModelScope.launch(coroutineLaunchContext.io) {
            if (getRbacRulesUseCase.execute(Unit).rbacModel.passwordCopyRule == ALLOW) {
                performSecretPropertyAction(
                    action = { secretPropertiesActionsInteractor.providePassword() },
                    doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
                    doOnDecryptionFailure = { emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE)) },
                    doOnSuccess = { emitSideEffect(AddToClipboard(it.label, it.result.orEmpty(), it.isSecret)) },
                )
            }
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    private fun copyMetadataDescription() {
        resourceDetailActionIdlingResource.setIdle(false)
        viewModelScope.launch(coroutineLaunchContext.io) {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideMetadataDescription() },
                doOnResult = { emitSideEffect(AddToClipboard(it.label, it.result, it.isSecret)) },
            )
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    private fun copyNote() {
        resourceDetailActionIdlingResource.setIdle(false)
        viewModelScope.launch(coroutineLaunchContext.io) {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideNote() },
                doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
                doOnDecryptionFailure = { emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE)) },
                doOnSuccess = { emitSideEffect(AddToClipboard(it.label, it.result, it.isSecret)) },
            )
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    private fun copyTotp() {
        resourceDetailActionIdlingResource.setIdle(false)
        doAfterOtpFetchAndDecrypt { label, _, otpParameters ->
            emitSideEffect(AddToClipboard(label, otpParameters.otpValue, isSecret = true))
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    private fun copyCustomField(key: UUID) {
        resourceDetailActionIdlingResource.setIdle(false)
        viewModelScope.launch(coroutineLaunchContext.io) {
            val customFieldLabel =
                resource.metadataJsonModel.customFields
                    ?.find { it.id == key }
                    ?.metadataKey

            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideCustomFields() },
                doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
                doOnDecryptionFailure = { emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE)) },
                doOnSuccess = {
                    val customFields =
                        resourceFormMapper.mapToUiModel(
                            resource.metadataJsonModel.customFields,
                            it.result,
                        )
                    val field = customFields.find { field -> field.id == key }
                    val fieldValue =
                        when (field) {
                            is BooleanCustomField -> field.secretValue?.toString() ?: ""
                            is NumberCustomField -> field.secretValue?.toString() ?: ""
                            is PasswordCustomField -> field.secretValue ?: ""
                            is UriCustomField -> field.secretValue ?: ""
                            is TextCustomField -> field.secretValue ?: ""
                            null -> ""
                        }
                    emitSideEffect(AddToClipboard(customFieldLabel.orEmpty(), fieldValue, isSecret = true))
                },
            )
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    private fun togglePasswordVisibility() {
        val isCurrentlyVisible = viewState.value.passwordData.isPasswordVisible
        if (isCurrentlyVisible) {
            updateViewState {
                copy(
                    passwordData =
                        passwordData.copy(
                            isPasswordVisible = false,
                            password = "",
                        ),
                )
            }
        } else {
            resourceDetailActionIdlingResource.setIdle(false)
            viewModelScope.launch(coroutineLaunchContext.io) {
                performSecretPropertyAction(
                    action = { secretPropertiesActionsInteractor.providePassword() },
                    doOnDecryptionFailure = { emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE)) },
                    doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
                    doOnSuccess = {
                        updateViewState {
                            copy(
                                passwordData =
                                    passwordData.copy(
                                        isPasswordVisible = true,
                                        password = it.result.orEmpty(),
                                    ),
                            )
                        }
                    },
                )
                resourceDetailActionIdlingResource.setIdle(true)
            }
        }
    }

    private fun toggleNoteVisibility() {
        val isCurrentlyVisible = viewState.value.noteData.isNoteVisible
        if (isCurrentlyVisible) {
            updateViewState {
                copy(
                    noteData =
                        noteData.copy(
                            isNoteVisible = false,
                            note = "",
                        ),
                )
            }
        } else {
            resourceDetailActionIdlingResource.setIdle(false)
            viewModelScope.launch(coroutineLaunchContext.io) {
                performSecretPropertyAction(
                    action = { secretPropertiesActionsInteractor.provideNote() },
                    doOnDecryptionFailure = { emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE)) },
                    doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
                    doOnSuccess = {
                        updateViewState {
                            copy(
                                noteData =
                                    noteData.copy(
                                        isNoteVisible = true,
                                        note = it.result,
                                    ),
                            )
                        }
                    },
                )
                resourceDetailActionIdlingResource.setIdle(true)
            }
        }
    }

    private fun toggleTotpVisibility() {
        val currentOtpModel = viewState.value.totpData.totpModel
        if (currentOtpModel?.isVisible == true) {
            updateViewState { copy(totpData = totpData.copy(totpModel = otpModelMapper.map(resource))) }
        } else {
            resourceDetailActionIdlingResource.setIdle(false)
            updateViewState {
                copy(
                    totpData = totpData.copy(totpModel = otpModelMapper.map(resource).copy(isRefreshing = true)),
                )
            }

            doAfterOtpFetchAndDecrypt { _, otp, otpParameters ->
                updateViewState {
                    copy(
                        totpData =
                            totpData.copy(
                                totpModel =
                                    otpModelMapper.map(resource).copy(
                                        otpValue = otpParameters.otpValue,
                                        isVisible = true,
                                        otpExpirySeconds = otp.period,
                                        remainingSecondsCounter = otpParameters.secondsValid,
                                        isRefreshing = false,
                                    ),
                            ),
                    )
                }
                resourceDetailActionIdlingResource.setIdle(true)
            }
        }
    }

    private fun toggleCustomFieldVisibility(key: UUID) {
        val isVisible = viewState.value.customFieldsData.visibleCustomFields[key] != null
        if (isVisible) {
            updateViewState {
                copy(customFieldsData = customFieldsData.copy(visibleCustomFields = customFieldsData.visibleCustomFields - key))
            }
        } else {
            resourceDetailActionIdlingResource.setIdle(false)
            viewModelScope.launch(coroutineLaunchContext.io) {
                performSecretPropertyAction(
                    action = { secretPropertiesActionsInteractor.provideCustomFields() },
                    doOnDecryptionFailure = { emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE)) },
                    doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
                    doOnSuccess = {
                        val customFields =
                            resourceFormMapper.mapToUiModel(
                                resource.metadataJsonModel.customFields,
                                it.result,
                            )
                        val field = customFields.find { field -> field.id == key }
                        updateViewState {
                            copy(
                                customFieldsData =
                                    customFieldsData.copy(
                                        visibleCustomFields = customFieldsData.visibleCustomFields + (key to field),
                                    ),
                            )
                        }
                    },
                )
                resourceDetailActionIdlingResource.setIdle(true)
            }
        }
    }

    private fun viewPermissions() {
        emitSideEffect(NavigateToResourcePermissions(resource.resourceId, PermissionsMode.VIEW))
    }

    private fun goToTags() {
        emitSideEffect(NavigateToResourceTags(resource.resourceId))
    }

    private fun goToLocation() {
        emitSideEffect(NavigateToResourceLocation(resource.resourceId))
    }

    private fun edit() {
        emitSideEffect(NavigateToEditResource(resource))
    }

    private fun editPermissions() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            if (canShareResourceUseCase.execute(Unit).canShareResource) {
                emitSideEffect(NavigateToResourcePermissions(resource.resourceId, PermissionsMode.EDIT))
            } else {
                emitSideEffect(ShowErrorSnackbar(CANNOT_PERFORM_ACTION))
            }
        }
    }

    private fun deleteResource() {
        updateViewState { copy(showDeleteResourceConfirmationDialog = false) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState { copy(isLoading = true) }
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.deleteResource() },
                doOnFailure = { emitSideEffect(ShowErrorSnackbar(GENERAL_ERROR)) },
                doOnSuccess = { emitSideEffect(CloseWithDeleteSuccess(it.resourceName)) },
            )
            updateViewState { copy(isLoading = false) }
        }
    }

    private fun launchWebsite() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideMainUri() },
                doOnResult = { emitSideEffect(OpenWebsite(it.result)) },
            )
        }
    }

    private fun toggleFavourite(option: ResourceMoreMenuModel.FavouriteOption) {
        resourceDetailActionIdlingResource.setIdle(false)
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState { copy(isLoading = true) }
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.toggleFavourite(option) },
                doOnFailure = { emitSideEffect(ShowErrorSnackbar(TOGGLE_FAVOURITE_FAILURE)) },
                doOnSuccess = { emitSideEffect(SetResourceEditedResult(resource.metadataJsonModel.name)) },
            )
            val refreshedResource =
                getLocalResourceUseCase
                    .execute(GetLocalResourceUseCase.Input(resource.resourceId))
                    .resource
            updateViewState {
                copy(
                    resourceData = resourceData.copy(resourceModel = refreshedResource),
                    isLoading = false,
                )
            }
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    private fun handleResourceEdited(resourceName: String?) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val refreshedResource =
                getLocalResourceUseCase
                    .execute(GetLocalResourceUseCase.Input(resource.resourceId))
                    .resource
            updateViewState { copy(resourceData = resourceData.copy(resourceModel = refreshedResource)) }
            viewModelScope.launch { loadResourceDetails() }

            emitSideEffect(ShowSuccessSnackbar(RESOURCE_EDITED))
            emitSideEffect(SetResourceEditedResult(resourceName.orEmpty()))
        }
    }

    private fun dispose() {
        updateViewState {
            copy(
                passwordData = passwordData.copy(isPasswordVisible = false, password = ""),
                noteData = noteData.copy(isNoteVisible = false, note = ""),
                customFieldsData = customFieldsData.copy(visibleCustomFields = emptyMap()),
            )
        }
    }

    private fun doAfterOtpFetchAndDecrypt(action: (ClipboardLabel, TotpSecret, OtpParameters) -> Unit) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideOtp() },
                doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
                doOnDecryptionFailure = { emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE)) },
                doOnSuccess = {
                    if (it.result.key.isNotBlank()) {
                        val otpParametersResult =
                            totpParametersProvider.provideOtpParameters(
                                secretKey = it.result.key,
                                digits = it.result.digits,
                                period = it.result.period,
                                algorithm = it.result.algorithm,
                            )
                        when (otpParametersResult) {
                            is OtpParameters -> action(it.label, it.result, otpParametersResult)
                            is TotpParametersProvider.OtpParametersResult.InvalidTotpInput -> {
                                val error = "Invalid TOTP input"
                                Timber.e(error)
                                emitSideEffect(ShowErrorSnackbar(GENERAL_ERROR))
                            }
                        }
                    } else {
                        val error = "Fetched totp key is empty"
                        Timber.e(error)
                        emitSideEffect(ShowErrorSnackbar(GENERAL_ERROR))
                    }
                },
            )
        }
    }
}
