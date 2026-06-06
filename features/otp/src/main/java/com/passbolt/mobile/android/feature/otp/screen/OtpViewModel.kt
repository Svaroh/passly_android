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

package net.svaroh.passly.feature.otp.screen

import androidx.lifecycle.viewModelScope
import net.svaroh.passly.common.coroutinetimer.TimerFactory
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.otpcore.TotpParametersProvider
import net.svaroh.passly.core.otpcore.TotpParametersProvider.OtpParametersResult.InvalidTotpInput
import net.svaroh.passly.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import net.svaroh.passly.core.resources.actions.ResourceCommonActionsInteractor
import net.svaroh.passly.core.resources.actions.ResourceUpdateActionsInteractorFactory
import net.svaroh.passly.core.resources.actions.SecretPropertiesActionsInteractorFactory
import net.svaroh.passly.core.resources.actions.SecretPropertyActionResult
import net.svaroh.passly.core.resources.actions.performCommonResourceAction
import net.svaroh.passly.core.resources.actions.performResourceUpdateAction
import net.svaroh.passly.core.resources.actions.performSecretPropertyAction
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourcesUseCase
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction
import net.svaroh.passly.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.AVATAR
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.CLEAR
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.NONE
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseCreateResourceMenu
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseDeleteConfirmationDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseOtpMoreMenu
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseSwitchAccount
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseTrustNewKeyDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseTrustedKeyDeletedDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.ConfirmDeleteTotp
import net.svaroh.passly.feature.otp.screen.OtpIntent.CopyOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.CreateNote
import net.svaroh.passly.feature.otp.screen.OtpIntent.CreatePassword
import net.svaroh.passly.feature.otp.screen.OtpIntent.CreateTotp
import net.svaroh.passly.feature.otp.screen.OtpIntent.DeleteOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.EditOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.OpenCreateResourceMenu
import net.svaroh.passly.feature.otp.screen.OtpIntent.OpenOtpMoreMenu
import net.svaroh.passly.feature.otp.screen.OtpIntent.OtpQRScanReturned
import net.svaroh.passly.feature.otp.screen.OtpIntent.ResourceFormReturned
import net.svaroh.passly.feature.otp.screen.OtpIntent.RevealOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.Search
import net.svaroh.passly.feature.otp.screen.OtpIntent.SearchEndIconAction
import net.svaroh.passly.feature.otp.screen.OtpIntent.TrustMetadataKeyDeletion
import net.svaroh.passly.feature.otp.screen.OtpIntent.TrustNewMetadataKey
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.CopyToClipboard
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.InitiateDataRefresh
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToCreateResourceForm
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToCreateTotp
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToEditResourceForm
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.ShowSuccessSnackbar
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.ShowToast
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.DECRYPTION_FAILURE
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.ERROR
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FAILED_TO_DELETE_RESOURCE
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FAILED_TO_TRUST_METADATA_KEY
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FAILED_TO_VERIFY_METADATA_KEYS
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FETCH_FAILURE
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.NO_SHARED_KEY_ACCESS
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.RESOURCE_SCHEMA_INVALID
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.SECRET_SCHEMA_INVALID
import net.svaroh.passly.feature.otp.screen.SnackbarSuccessType.METADATA_KEY_IS_TRUSTED
import net.svaroh.passly.feature.otp.screen.SnackbarSuccessType.RESOURCE_CREATED
import net.svaroh.passly.feature.otp.screen.SnackbarSuccessType.RESOURCE_DELETED
import net.svaroh.passly.feature.otp.screen.SnackbarSuccessType.RESOURCE_EDITED
import net.svaroh.passly.feature.otp.screen.ToastType.WAIT_FOR_DATA_REFRESH_FINISH
import net.svaroh.passly.jsonmodel.delegates.TotpSecret
import net.svaroh.passly.mappers.OtpModelMapper
import net.svaroh.passly.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.metadata.usecase.CanCreateResourceUseCase
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.RESOURCE
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.SECRET
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.Totp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5DefaultWithTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5TotpStandalone
import net.svaroh.passly.supportedresourceTypes.SupportedContentTypes.totpSlugs
import net.svaroh.passly.ui.LeadingContentType.PASSWORD
import net.svaroh.passly.ui.LeadingContentType.STANDALONE_NOTE
import net.svaroh.passly.ui.LeadingContentType.TOTP
import net.svaroh.passly.ui.NewMetadataKeyToTrustModel
import net.svaroh.passly.ui.OtpItemWrapper
import net.svaroh.passly.ui.ResourceModel
import net.svaroh.passly.ui.allReset
import net.svaroh.passly.ui.findVisible
import net.svaroh.passly.ui.isExpired
import net.svaroh.passly.ui.refreshingNone
import net.svaroh.passly.ui.refreshingOnly
import net.svaroh.passly.ui.replaceOnId
import net.svaroh.passly.ui.revealed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

internal class OtpViewModel(
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val otpModelMapper: OtpModelMapper,
    private val totpParametersProvider: TotpParametersProvider,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    private val timerFactory: TimerFactory,
    private val canCreateResourceUse: CanCreateResourceUseCase,
    private val resourceUpdateActionsInteractorFactory: ResourceUpdateActionsInteractorFactory,
    private val secretPropertiesActionsInteractorFactory: SecretPropertiesActionsInteractorFactory,
) : SideEffectViewModel<OtpState, OtpSideEffect>(OtpState()),
    KoinComponent {
    init {
        loadUserAvatar()
        viewModelScope.launch(coroutineLaunchContext.io) {
            synchronizeWithDataRefresh()
        }
        viewModelScope.launch(coroutineLaunchContext.io) {
            val otps = getOtpResources()
            updateViewState { copy(otps = otps) }
            updateOtpsCounterTime()
        }
    }

    private fun onCanCreateResource(function: () -> Unit) {
        viewModelScope.launch {
            if (canCreateResourceUse.execute(CanCreateResourceUseCase.Input(folderId = null)).canCreateResource) {
                function()
            } else {
                emitSideEffect(ShowErrorSnackbar(NO_SHARED_KEY_ACCESS))
            }
        }
    }

    // TODO refactor after feature completion
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun onIntent(intent: OtpIntent) {
        when (intent) {
            OpenCreateResourceMenu -> updateViewState { copy(showCreateResourceBottomSheet = true) }
            CloseCreateResourceMenu -> updateViewState { copy(showCreateResourceBottomSheet = false) }
            is Search -> searchQueryChanged(intent.searchQuery)
            is RevealOtp -> {
                updateViewState { copy(showOtpMoreBottomSheet = false) }
                otpClick(intent.otpItemWrapper)
            }
            is OpenOtpMoreMenu -> updateViewState { copy(showOtpMoreBottomSheet = true, moreMenuResource = intent.otpItemWrapper) }
            is CloseOtpMoreMenu -> updateViewState { copy(showOtpMoreBottomSheet = false) }
            CreatePassword -> {
                updateViewState { copy(showCreateResourceBottomSheet = false) }
                onCanCreateResource { emitSideEffect(NavigateToCreateResourceForm(leadingContentType = PASSWORD)) }
            }
            CreateNote -> {
                updateViewState { copy(showCreateResourceBottomSheet = false) }
                onCanCreateResource { emitSideEffect(NavigateToCreateResourceForm(leadingContentType = STANDALONE_NOTE)) }
            }
            CreateTotp -> {
                updateViewState { copy(showCreateResourceBottomSheet = false) }
                onCanCreateResource { emitSideEffect(NavigateToCreateTotp) }
            }
            is OtpQRScanReturned -> processOtpScanResult(intent)
            is ResourceFormReturned -> processResourceFormResult(intent)
            is CopyOtp -> {
                updateViewState { copy(showOtpMoreBottomSheet = false) }
                copyTotp(intent.otpItemWrapper)
            }
            is DeleteOtp -> updateViewState { copy(showOtpMoreBottomSheet = false, showDeleteTotpConfirmationDialog = true) }
            is EditOtp -> {
                updateViewState { copy(showOtpMoreBottomSheet = false) }
                emitSideEffect(
                    NavigateToEditResourceForm(
                        resourceId = intent.otpItemWrapper.resource.resourceId,
                        resourceName = intent.otpItemWrapper.resource.metadataJsonModel.name,
                    ),
                )
            }
            CloseDeleteConfirmationDialog -> updateViewState { copy(showDeleteTotpConfirmationDialog = false) }
            ConfirmDeleteTotp -> {
                updateViewState { copy(showProgress = true, showDeleteTotpConfirmationDialog = false) }
                deleteTotp(viewState.value.moreMenuResource)
            }
            CloseTrustedKeyDeletedDialog ->
                updateViewState {
                    copy(
                        showMetadataTrustedKeyDeletedDialog = false,
                        metadataDeletedKeyModel = null,
                    )
                }
            is TrustMetadataKeyDeletion -> deleteTrustedMetadataKeyConfirmed()
            CloseTrustNewKeyDialog ->
                updateViewState {
                    copy(
                        showNewMetadataTrustDialog = false,
                        newMetadataKeyTrustModel = null,
                    )
                }
            is TrustNewMetadataKey -> trustNewMetadataKeyConfirmed(intent.model)
            CloseSwitchAccount -> updateViewState { copy(showAccountSwitchBottomSheet = false) }
            SearchEndIconAction -> searchEndIconAction()
        }
    }

    private fun searchEndIconAction() {
        when (viewState.value.searchInputEndIconMode) {
            AVATAR -> {
                viewModelScope.launch(coroutineLaunchContext.io) {
                    if (dataRefreshTrackingFlow.isInProgress()) {
                        emitSideEffect(ShowToast(WAIT_FOR_DATA_REFRESH_FINISH))
                        dataRefreshTrackingFlow.awaitIdle()
                    }
                    updateViewState { copy(showAccountSwitchBottomSheet = true) }
                }
            }
            CLEAR ->
                updateViewState {
                    copy(
                        searchQuery = "",
                        searchInputEndIconMode = AVATAR,
                    )
                }
            NONE -> {
                // no-op
            }
        }
    }

    private fun trustNewMetadataKeyConfirmed(model: NewMetadataKeyToTrustModel) {
        updateViewState { copy(showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            when (
                val output =
                    runAuthenticatedOperation {
                        metadataPrivateKeysHelperInteractor.trustNewKey(model)
                    }
            ) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    emitSideEffect(ShowSuccessSnackbar(METADATA_KEY_IS_TRUSTED))
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_TRUST_METADATA_KEY))
                }
            }
            updateViewState {
                copy(
                    showNewMetadataTrustDialog = false,
                    newMetadataKeyTrustModel = null,
                )
            }
        }
    }

    private fun deleteTrustedMetadataKeyConfirmed() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
            updateViewState {
                copy(
                    showMetadataTrustedKeyDeletedDialog = false,
                    metadataDeletedKeyModel = null,
                )
            }
        }
    }

    private fun deleteTotp(moreMenuResource: OtpItemWrapper?) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val otpResource = requireNotNull(moreMenuResource)
            val slug =
                idToSlugMappingProvider.provideMappingForSelectedAccount()[
                    UUID.fromString(otpResource.resource.resourceTypeId),
                ]
            when (val contentType = ContentType.fromSlug(slug!!)) {
                is Totp, V5TotpStandalone ->
                    deleteStandaloneTotpResource(otpResource.resource)
                is PasswordDescriptionTotp, V5DefaultWithTotp ->
                    downgradeToPasswordAndDescriptionResource(otpResource.resource)
                else ->
                    error("$contentType type should not be presented on totp list")
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun deleteStandaloneTotpResource(otpResource: ResourceModel) {
        val resourceCommonActionsInteractor = get<ResourceCommonActionsInteractor> { parametersOf(otpResource) }
        performCommonResourceAction(
            action = { resourceCommonActionsInteractor.deleteResource() },
            doOnFailure = { emitSideEffect(ShowErrorSnackbar(FAILED_TO_DELETE_RESOURCE)) },
            doOnSuccess = {
                emitSideEffect(ShowSuccessSnackbar(RESOURCE_DELETED))
                emitSideEffect(InitiateDataRefresh)
            },
        )
    }

    private suspend fun downgradeToPasswordAndDescriptionResource(otpResource: ResourceModel) {
        val resourceUpdateActionInteractor = resourceUpdateActionsInteractorFactory.create(otpResource)
        performResourceUpdateAction(
            action = {
                resourceUpdateActionInteractor.updateGenericResource(
                    UpdateAction.REMOVE_TOTP,
                    secretModification = { it.apply { totp = null } },
                )
            },
            doOnCryptoFailure = { emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.ENCRYPTION_FAILURE)) },
            doOnFailure = { emitSideEffect(ShowErrorSnackbar(ERROR)) },
            doOnSuccess = {
                emitSideEffect(ShowSuccessSnackbar(RESOURCE_DELETED))
                emitSideEffect(InitiateDataRefresh)
            },
            doOnSchemaValidationFailure = {
                when (it) {
                    RESOURCE -> emitSideEffect(ShowErrorSnackbar(RESOURCE_SCHEMA_INVALID))
                    SECRET -> emitSideEffect(ShowErrorSnackbar(SECRET_SCHEMA_INVALID))
                }
            },
            doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
            doOnCannotEditWithCurrentConfig = { emitSideEffect(ShowErrorSnackbar(CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION)) },
            doOnMetadataKeyModified = {
                updateViewState { copy(showNewMetadataTrustDialog = true, newMetadataKeyTrustModel = it) }
            },
            doOnMetadataKeyDeleted = {
                updateViewState {
                    copy(
                        showMetadataTrustedKeyDeletedDialog = true,
                        metadataDeletedKeyModel = it,
                    )
                }
            },
            doOnMetadataKeyVerificationFailure = { emitSideEffect(ShowErrorSnackbar(FAILED_TO_VERIFY_METADATA_KEYS)) },
        )
    }

    private fun copyTotp(otpItemWrapper: OtpItemWrapper) {
        fetchTotp(otpItemWrapper) { totp ->
            val otpParameters =
                totpParametersProvider.provideOtpParameters(
                    secretKey = totp.result.key,
                    digits = totp.result.digits,
                    period = totp.result.period,
                    algorithm = totp.result.algorithm,
                )

            when (otpParameters) {
                InvalidTotpInput -> stopRefreshingAndShowError("Failed to generate totp parameters")
                is OtpParameters -> {
                    emitSideEffect(
                        CopyToClipboard(
                            label = totp.label,
                            value = otpParameters.otpValue,
                            isSensitive = true,
                        ),
                    )
                }
            }
        }
    }

    private fun processResourceFormResult(intent: ResourceFormReturned) {
        if (intent.resourceCreated) {
            emitSideEffect(InitiateDataRefresh)
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_CREATED, intent.resourceName))
        }
        if (intent.resourceEdited) {
            emitSideEffect(InitiateDataRefresh)
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_EDITED, intent.resourceName))
        }
    }

    private fun processOtpScanResult(intent: OtpQRScanReturned) {
        if (intent.otpCreated) {
            emitSideEffect(InitiateDataRefresh)
        } else {
            if (intent.otpManualCreationChosen) {
                emitSideEffect(NavigateToCreateResourceForm(leadingContentType = TOTP))
            }
        }
    }

    private fun searchQueryChanged(searchQuery: String) {
        val searchEndIcon = if (searchQuery.isNotBlank()) CLEAR else AVATAR
        viewModelScope.launch {
            val filteredOtps = getOtpResources(searchQuery)
            updateViewState {
                copy(
                    searchInputEndIconMode = searchEndIcon,
                    searchQuery = searchQuery,
                    filteredOtps = filteredOtps,
                )
            }
        }
    }

    private fun otpClick(otpItemWrapper: OtpItemWrapper) {
        updateViewState { copy(showOtpMoreBottomSheet = false) }
        fetchTotp(otpItemWrapper) {
            showTotp(it, otpItemWrapper.resource.resourceId)
        }
    }

    private fun fetchTotp(
        otpItemWrapper: OtpItemWrapper,
        afterFetchAction: (SecretPropertyActionResult.Success<TotpSecret>) -> Unit,
    ) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState {
                copy(otps = otps.refreshingOnly(otpItemWrapper.resource.resourceId))
            }

            val secretPropertiesActionsInteractor = secretPropertiesActionsInteractorFactory.create(otpItemWrapper.resource)

            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideOtp() },
                doOnDecryptionFailure = {
                    emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE))
                    updateViewState { copy(otps = otps.refreshingNone()) }
                },
                doOnFetchFailure = {
                    emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE))
                    updateViewState { copy(otps = otps.refreshingNone()) }
                },
                doOnSuccess = { result ->
                    afterFetchAction(result)
                },
            )
        }
    }

    private fun showTotp(
        totp: SecretPropertyActionResult.Success<TotpSecret>,
        resourceId: String,
    ) {
        if (totp.result.key.isBlank()) {
            stopRefreshingAndShowError("Fetched totp key is empty")
        }

        val otpParameters =
            totpParametersProvider.provideOtpParameters(
                secretKey = totp.result.key,
                digits = totp.result.digits,
                period = totp.result.period,
                algorithm = totp.result.algorithm,
            )

        when (otpParameters) {
            InvalidTotpInput -> stopRefreshingAndShowError("Failed to generate totp parameters")
            is OtpParameters -> {
                updateViewState {
                    copy(
                        otps =
                            otps.revealed(
                                resourceId,
                                otpParameters.otpValue,
                                totp.result.period,
                                otpParameters.secondsValid,
                            ),
                    )
                }

                emitSideEffect(
                    CopyToClipboard(
                        label = totp.label,
                        value = otpParameters.otpValue,
                        isSensitive = true,
                    ),
                )
            }
        }
    }

    private suspend fun updateOtpsCounterTime() {
        timerFactory.createInfiniteTimer(tickDuration = 1.seconds).collectLatest {
            val visibleTotp = viewState.value.otps.findVisible()
            if (visibleTotp != null) {
                val updated = visibleTotp.copy(remainingSecondsCounter = (visibleTotp.remainingSecondsCounter!!) - 1)

                if (updated.isExpired()) {
                    updateViewState { copy(otps = otps.allReset()) }
                    fetchTotp(updated) {
                        showTotp(it, updated.resource.resourceId)
                    }
                } else {
                    updateViewState { copy(otps = otps.replaceOnId(updated)) }
                }
            }
        }
    }

    private suspend fun synchronizeWithDataRefresh() {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect {
            when (it) {
                InProgress -> updateViewState { copy(isRefreshing = true) }
                FinishedWithFailure -> {
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                    updateViewState { copy(isRefreshing = false) }
                }
                FinishedWithSuccess -> {
                    val otps = getOtpResources()
                    updateViewState { copy(otps = otps, isRefreshing = false) }
                }
                NotCompleted -> {
                    // do nothing
                }
            }
        }
    }

    private fun loadUserAvatar() {
        val avatarUrl =
            getSelectedAccountDataUseCase
                .execute(Unit)
                .avatarUrl

        updateViewState { copy(userAvatar = avatarUrl) }
    }

    private suspend fun getOtpResources(searchQuery: String? = null): List<OtpItemWrapper> =
        getLocalResourcesUseCase
            .execute(GetLocalResourcesUseCase.Input(totpSlugs, searchQuery = searchQuery))
            .resources
            .map(otpModelMapper::map)

    private fun stopRefreshingAndShowError(message: String) {
        Timber.e(message)
        emitSideEffect(ShowErrorSnackbar(ERROR, message))
        updateViewState { copy(otps = otps.refreshingNone()) }
    }
}
