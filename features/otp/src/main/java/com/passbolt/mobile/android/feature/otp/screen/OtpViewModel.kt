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
import net.svaroh.passly.common.time.TimeProvider
import net.svaroh.passly.common.urimatcher.AutofillUriMatcher
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.otpcore.TotpParametersProvider
import net.svaroh.passly.core.otpcore.TotpParametersProvider.OtpParametersResult.InvalidTotpInput
import net.svaroh.passly.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.AVATAR
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.CLEAR
import net.svaroh.passly.core.ui.search.SearchInputEndIconMode.NONE
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.domain.metadata.interactor.ResourceAccessInteractor
import net.svaroh.passly.domain.resources.actions.ResourceCommonActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourceUpdateActionsInteractorFactory
import net.svaroh.passly.domain.resources.actions.SecretPropertiesActionsInteractorFactory
import net.svaroh.passly.domain.resources.actions.SecretPropertyActionResult
import net.svaroh.passly.domain.resources.actions.performCommonResourceAction
import net.svaroh.passly.domain.resources.actions.performResourceUpdateAction
import net.svaroh.passly.domain.resources.actions.performSecretPropertyAction
import net.svaroh.passly.domain.resources.mapper.toOtpItemWrapper
import net.svaroh.passly.domain.resources.usecase.EditPermissionsConfirmationInteractor
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesUseCase
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.feature.home.screen.ShowSuggestedModel
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseDeleteConfirmationDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseOtpMoreMenu
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseSwitchAccount
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseTrustNewKeyDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.CloseTrustedKeyDeletedDialog
import net.svaroh.passly.feature.otp.screen.OtpIntent.ConfirmDeleteTotp
import net.svaroh.passly.feature.otp.screen.OtpIntent.ConfirmedPermissionsResult
import net.svaroh.passly.feature.otp.screen.OtpIntent.CopyOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.CreateTotp
import net.svaroh.passly.feature.otp.screen.OtpIntent.DeleteOtp
import net.svaroh.passly.feature.otp.screen.OtpIntent.Dispose
import net.svaroh.passly.feature.otp.screen.OtpIntent.EditOtp
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
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToConfirmPermissions
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToCreateResourceForm
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToCreateTotp
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.NavigateToEditResourceForm
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.otp.screen.OtpSideEffect.ShowSuccessSnackbar
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.DECRYPTION_FAILURE
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.ERROR
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FAILED_TO_DELETE_RESOURCE
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FAILED_TO_TRUST_METADATA_KEY
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FAILED_TO_VERIFY_METADATA_KEYS
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.FETCH_FAILURE
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.INVALID_TOTP_PARAMETERS
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.NO_SHARED_KEY_ACCESS
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.RESOURCE_SCHEMA_INVALID
import net.svaroh.passly.feature.otp.screen.SnackbarErrorType.SECRET_SCHEMA_INVALID
import net.svaroh.passly.feature.otp.screen.SnackbarSuccessType.METADATA_KEY_IS_TRUSTED
import net.svaroh.passly.feature.otp.screen.SnackbarSuccessType.RESOURCE_CREATED
import net.svaroh.passly.feature.otp.screen.SnackbarSuccessType.RESOURCE_DELETED
import net.svaroh.passly.feature.otp.screen.SnackbarSuccessType.RESOURCE_EDITED
import net.svaroh.passly.jsonmodel.delegates.TotpSecret
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.RESOURCE
import net.svaroh.passly.serializers.jsonschema.SchemaEntity.SECRET
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.Totp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5DefaultWithTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5TotpStandalone
import net.svaroh.passly.supportedresourceTypes.SupportedContentTypes.totpSlugs
import net.svaroh.passly.ui.LeadingContentType.TOTP
import net.svaroh.passly.ui.NewMetadataKeyToTrustModel
import net.svaroh.passly.ui.OtpItemWrapper
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.ResourceUiModel
import net.svaroh.passly.ui.allReset
import net.svaroh.passly.ui.contentType
import net.svaroh.passly.ui.findVisible
import net.svaroh.passly.ui.isExpired
import net.svaroh.passly.ui.refreshingNone
import net.svaroh.passly.ui.refreshingOnly
import net.svaroh.passly.ui.replaceOnId
import net.svaroh.passly.ui.revealed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class OtpViewModel(
    private val showSuggestedModel: ShowSuggestedModel,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val totpParametersProvider: TotpParametersProvider,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    private val timerFactory: TimerFactory,
    private val resourceAccessInteractor: ResourceAccessInteractor,
    private val resourceUpdateActionsInteractorFactory: ResourceUpdateActionsInteractorFactory,
    private val editPermissionsConfirmationInteractor: EditPermissionsConfirmationInteractor,
    private val secretPropertiesActionsInteractorFactory: SecretPropertiesActionsInteractorFactory,
    private val autofillUriMatcher: AutofillUriMatcher,
    private val timeProvider: TimeProvider,
) : SideEffectViewModel<OtpState, OtpSideEffect>(OtpState()),
    KoinComponent {
    private var dataRefreshJob: Job? = null
    private var otpsCounterJob: Job? = null
    private var universalCountdownJob: Job? = null
    private var fetchTotpJob: Job? = null

    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadUserAvatar()
        observeSearchQuery()
        updateViewState { copy(universalCountdownSeconds = currentRemainingCountdownSeconds()) }
        dataRefreshJob?.cancel()
        dataRefreshJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                synchronizeWithDataRefresh()
            }
        otpsCounterJob?.cancel()
        otpsCounterJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                val otps = getOtpResources()
                updateViewState { copy(otps = otps, suggestedOtps = getSuggestedOtps(otps)) }
                updateOtpsCounterTime()
            }
        universalCountdownJob?.cancel()
        universalCountdownJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                updateUniversalCountdown()
            }
    }

    private fun currentRemainingCountdownSeconds(): Long =
        DEFAULT_TOTP_PERIOD - (timeProvider.getCurrentEpochSeconds() % DEFAULT_TOTP_PERIOD)

    override fun onCleared() {
        dataRefreshJob?.cancel()
        otpsCounterJob?.cancel()
        universalCountdownJob?.cancel()
        fetchTotpJob?.cancel()
    }

    private fun onCanCreateResource(function: () -> Unit) {
        viewModelScope.launch {
            if (resourceAccessInteractor.canCreateResource()) {
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
            is Search -> searchQueryChanged(intent.searchQuery)
            is RevealOtp -> {
                updateViewState { copy(showOtpMoreBottomSheet = false) }
                otpClick(intent.resource)
            }
            is OpenOtpMoreMenu -> updateViewState { copy(showOtpMoreBottomSheet = true, moreMenuResource = intent.otpItemWrapper) }
            is CloseOtpMoreMenu -> updateViewState { copy(showOtpMoreBottomSheet = false) }
            CreateTotp -> onCanCreateResource { emitSideEffect(NavigateToCreateTotp) }
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
            is ConfirmedPermissionsResult -> confirmedPermissionsReceived(intent.permissions)
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
            Dispose -> dispose()
        }
    }

    private fun dispose() {
        fetchTotpJob?.cancel()
        updateOtpLists { allReset() }
    }

    private fun updateOtpLists(transform: List<OtpItemWrapper>.() -> List<OtpItemWrapper>) {
        updateViewState { copy(otps = otps.transform(), filteredOtps = filteredOtps.transform()) }
    }

    private fun searchEndIconAction() {
        when (viewState.value.searchInputEndIconMode) {
            AVATAR -> {
                // opened straight away: the list is served from the local replica, so there is nothing to wait for
                updateViewState { copy(showAccountSwitchBottomSheet = true) }
            }
            CLEAR -> searchQueryChanged("")
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
            when (val contentType = otpResource.resource.contentType()) {
                is Totp, V5TotpStandalone ->
                    deleteStandaloneTotpResource(otpResource.resource)
                is PasswordDescriptionTotp, V5DefaultWithTotp ->
                    if (editPermissionsConfirmationInteractor.shouldConfirmPermissions(otpResource.resource.resourceId)) {
                        Timber.d("Removing totp from a shared resource - navigating to permissions confirmation")
                        updateViewState { copy(pendingPermissionsConfirmationResource = otpResource.resource) }
                        emitSideEffect(NavigateToConfirmPermissions(otpResource.resource.resourceId))
                    } else {
                        downgradeToPasswordAndDescriptionResource(otpResource.resource)
                    }
                else ->
                    error("$contentType type should not be presented on totp list")
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun deleteStandaloneTotpResource(otpResource: ResourceUiModel) {
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

    private fun confirmedPermissionsReceived(confirmedPermissions: List<PermissionModelUi>) {
        val resource = viewState.value.pendingPermissionsConfirmationResource ?: return
        updateViewState { copy(pendingPermissionsConfirmationResource = null, showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            downgradeToPasswordAndDescriptionResource(resource, confirmedPermissions)
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun downgradeToPasswordAndDescriptionResource(
        otpResource: ResourceUiModel,
        confirmedPermissions: List<PermissionModelUi>? = null,
    ) {
        val resourceUpdateActionInteractor = resourceUpdateActionsInteractorFactory.create(otpResource)
        performResourceUpdateAction(
            action = {
                if (confirmedPermissions == null) {
                    resourceUpdateActionInteractor.updateGenericResource(
                        UpdateAction.REMOVE_TOTP,
                        secretModification = { it.apply { totp = null } },
                    )
                } else {
                    resourceUpdateActionInteractor.updateGenericResourceWithConfirmedPermissions(
                        UpdateAction.REMOVE_TOTP,
                        confirmedPermissions,
                        secretModification = { it.apply { totp = null } },
                    )
                }
            },
            doOnPermissionsDrifted = { drifted ->
                updateViewState { copy(pendingPermissionsConfirmationResource = otpResource) }
                emitSideEffect(
                    NavigateToConfirmPermissions(otpResource.resourceId, driftedEntityNames = drifted.driftedEntityNames),
                )
            },
            doOnShareFailure = { emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.SHARE_FAILED)) },
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
        fetchTotp(otpItemWrapper.resource) { totp ->
            val otpParameters =
                totpParametersProvider.provideOtpParameters(
                    secretKey = totp.result.key,
                    digits = totp.result.digits,
                    period = totp.result.period,
                    algorithm = totp.result.algorithm,
                )

            when (otpParameters) {
                InvalidTotpInput -> stopRefreshingAndShowInvalidTotpError()
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
        if (searchQuery == searchQueryFlow.value) {
            return
        }
        searchQueryFlow.value = searchQuery
        updateViewState {
            copy(
                searchInputEndIconMode = if (searchQuery.isNotBlank()) CLEAR else AVATAR,
                isSearching = true,
            )
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            searchQueryFlow
                .drop(1)
                .debounce(SEARCH_DEBOUNCE)
                .collectLatest { searchQuery ->
                    Timber.d("Applying search query (length: ${searchQuery.length})")
                    try {
                        val filteredOtps = getOtpResources(searchQuery)
                        updateViewState {
                            copy(searchQuery = searchQuery, filteredOtps = filteredOtps, isSearching = false)
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to apply the search query")
                        updateViewState { copy(isSearching = false) }
                    }
                }
        }
    }

    private fun otpClick(resource: ResourceUiModel) {
        updateViewState { copy(showOtpMoreBottomSheet = false) }
        fetchTotp(resource) {
            showTotp(it, resource.resourceId)
        }
    }

    private fun fetchTotp(
        resource: ResourceUiModel,
        afterFetchAction: (SecretPropertyActionResult.Success<TotpSecret>) -> Unit,
    ) {
        fetchTotpJob?.cancel()
        fetchTotpJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                updateOtpLists { refreshingOnly(resource.resourceId) }

                val secretPropertiesActionsInteractor = secretPropertiesActionsInteractorFactory.create(resource)

                performSecretPropertyAction(
                    action = { secretPropertiesActionsInteractor.provideOtp() },
                    doOnDecryptionFailure = {
                        emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE))
                        updateOtpLists { refreshingNone() }
                    },
                    doOnFetchFailure = {
                        emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE))
                        updateOtpLists { refreshingNone() }
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
                updateOtpLists {
                    revealed(
                        resourceId,
                        otpParameters.otpValue,
                        totp.result.period,
                        otpParameters.secondsValid,
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

    private suspend fun updateUniversalCountdown() {
        timerFactory.createInfiniteTimer(tickDuration = 1.seconds).collectLatest {
            updateViewState { copy(universalCountdownSeconds = currentRemainingCountdownSeconds()) }
        }
    }

    private suspend fun updateOtpsCounterTime() {
        timerFactory.createInfiniteTimer(tickDuration = 1.seconds).collectLatest {
            val visibleTotp = viewState.value.otps.findVisible()
            if (visibleTotp != null) {
                val updated = visibleTotp.copy(remainingSecondsCounter = (visibleTotp.remainingSecondsCounter!!) - 1)

                if (updated.isExpired()) {
                    updateOtpLists { allReset() }
                    fetchTotp(updated.resource) {
                        showTotp(it, updated.resource.resourceId)
                    }
                } else {
                    updateOtpLists { replaceOnId(updated) }
                }
            }
        }
    }

    private suspend fun synchronizeWithDataRefresh() {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect {
            when (it) {
                // silent unless the user asked for it, see HomeViewModel for the reasoning
                is InProgress -> updateViewState {
                    copy(
                        isRefreshing = dataRefreshTrackingFlow.isUserInitiated,
                        refreshProgress = it.progress,
                    )
                }
                FinishedWithFailure -> {
                    if (dataRefreshTrackingFlow.isUserInitiated) {
                        emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                    }
                    updateViewState { copy(isRefreshing = false) }
                }
                FinishedWithSuccess -> {
                    val otps = getOtpResources()
                    val searchQuery = viewState.value.searchQuery
                    val filteredOtps = if (searchQuery.isNotEmpty()) getOtpResources(searchQuery) else emptyList()
                    updateViewState {
                        copy(
                            otps = otps,
                            filteredOtps = filteredOtps,
                            suggestedOtps = getSuggestedOtps(otps),
                            isRefreshing = false,
                        )
                    }
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

    private fun getSuggestedOtps(otps: List<OtpItemWrapper>): List<OtpItemWrapper> =
        when (showSuggestedModel) {
            is ShowSuggestedModel.DoNotShow -> emptyList()
            is ShowSuggestedModel.Show ->
                otps.filter { otpItem ->
                    val resourceUris =
                        otpItem.resource.metadataJsonModel.uris
                            .orEmpty() +
                            listOfNotNull(otpItem.resource.metadataJsonModel.uri)
                    autofillUriMatcher.isMatching(showSuggestedModel.suggestedUri, resourceUris)
                }
        }

    private suspend fun getOtpResources(searchQuery: String? = null): List<OtpItemWrapper> =
        getLocalResourcesUseCase
            .execute(GetLocalResourcesUseCase.Input(totpSlugs, searchQuery = searchQuery))
            .resources
            .map(ResourceUiModel::toOtpItemWrapper)

    private fun stopRefreshingAndShowError(message: String) {
        Timber.e(message)
        emitSideEffect(ShowErrorSnackbar(ERROR, message))
        updateOtpLists { refreshingNone() }
    }

    private fun stopRefreshingAndShowInvalidTotpError() {
        Timber.e("Invalid TOTP parameters")
        emitSideEffect(ShowErrorSnackbar(INVALID_TOTP_PARAMETERS))
        updateOtpLists { refreshingNone() }
    }

    companion object {
        val SEARCH_DEBOUNCE = 300.milliseconds
    }
}
