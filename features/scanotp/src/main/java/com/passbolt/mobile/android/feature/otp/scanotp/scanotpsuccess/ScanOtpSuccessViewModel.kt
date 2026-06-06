package net.svaroh.passly.feature.otp.scanotp.scanotpsuccess

import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.resources.actions.ResourceCreateActionsInteractor
import net.svaroh.passly.core.resources.actions.ResourceUpdateActionResult
import net.svaroh.passly.core.resources.actions.ResourceUpdateActionsInteractorFactory
import net.svaroh.passly.core.resources.actions.performResourceCreateAction
import net.svaroh.passly.core.resources.actions.performResourceUpdateAction
import net.svaroh.passly.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction
import net.svaroh.passly.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.core.secrets.usecase.decrypt.parser.SecretJsonModel
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.CreateStandaloneOtpClick
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.DismissNewMetadataTrustDialog
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.DismissTrustedMetadataKeyDeletedDialog
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkToResourceClick
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkedResourceReceived
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.TrustNewMetadataKey
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.TrustedMetadataKeyDeleted
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.NavigateToOtpList
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.NavigateToResourcePicker
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.ShowSuccessSnackbar
import net.svaroh.passly.jsonmodel.delegates.TotpSecret
import net.svaroh.passly.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.serializers.jsonschema.SchemaEntity
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordAndDescription
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5Default
import net.svaroh.passly.supportedresourceTypes.ContentType.V5DefaultWithTotp
import net.svaroh.passly.ui.LeadingContentType
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.NewMetadataKeyToTrustModel
import net.svaroh.passly.ui.OtpParseResult
import net.svaroh.passly.ui.ResourceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber
import java.util.UUID

internal class ScanOtpSuccessViewModel(
    private val scannedTotp: OtpParseResult.OtpQr.TotpQr,
    private val parentFolderId: String?,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val getDefaultCreateContentTypeUseCase: GetDefaultCreateContentTypeUseCase,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    private val resourceUpdateActionsInteractorFactory: ResourceUpdateActionsInteractorFactory,
) : SideEffectViewModel<ScanOtpSuccessState, ScanOtpSuccessSideEffect>(ScanOtpSuccessState()),
    KoinComponent {
    fun onIntent(intent: ScanOtpSuccessIntent) {
        when (intent) {
            CreateStandaloneOtpClick -> createStandaloneOtp()
            LinkToResourceClick -> emitSideEffect(NavigateToResourcePicker(scannedTotp.issuer))
            is LinkedResourceReceived -> linkedResourceReceived(intent.resource)
            TrustNewMetadataKey -> viewState.value.metadataKeyToTrust?.let { trustNewMetadataKey(it) }
            TrustedMetadataKeyDeleted -> trustedMetadataKeyDeleted()
            DismissNewMetadataTrustDialog -> updateViewState { copy(showNewMetadataTrustDialog = false) }
            DismissTrustedMetadataKeyDeletedDialog -> updateViewState { copy(showTrustedMetadataKeyDeletedDialog = false) }
        }
    }

    private fun createStandaloneOtp() {
        launch {
            updateViewState { copy(showProgress = true) }
            val resourceCreateActionsInteractor = get<ResourceCreateActionsInteractor>()
            val defaultType =
                getDefaultCreateContentTypeUseCase.execute(
                    GetDefaultCreateContentTypeUseCase.Input(LeadingContentType.TOTP),
                )

            if (defaultType is GetDefaultCreateContentTypeUseCase.Output.CreationContentType) {
                performResourceCreateAction(
                    action = {
                        resourceCreateActionsInteractor.createGenericResource(
                            resourceParentFolderId = parentFolderId,
                            contentType = defaultType.contentType,
                            metadataJsonModel =
                                MetadataJsonModel.empty().apply {
                                    name = scannedTotp.label
                                    scannedTotp.issuer?.let {
                                        setMainUri(defaultType.contentType, it)
                                    }
                                },
                            secretJsonModel =
                                SecretJsonModel.emptyTotp().apply {
                                    totp = scannedTotp.toTotpSecret()
                                },
                        )
                    },
                    doOnFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.GENERIC_ERROR)) },
                    doOnCryptoFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.ENCRYPTION_ERROR, it)) },
                    doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                    doOnSuccess = {
                        emitSideEffect(NavigateToOtpList(scannedTotp, otpCreated = true, it.resourceId))
                    },
                    doOnCannotCreateWithCurrentConfig = {
                        emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.CANNOT_CREATE_WITH_CURRENT_CONFIG))
                    },
                    doOnMetadataKeyModified = {
                        updateViewState { copy(metadataKeyToTrust = it, showNewMetadataTrustDialog = true) }
                    },
                    doOnMetadataKeyDeleted = {
                        updateViewState { copy(metadataKeyDeleted = it, showTrustedMetadataKeyDeletedDialog = true) }
                    },
                    doOnMetadataKeyVerificationFailure = {
                        emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.FAILED_TO_VERIFY_METADATA_KEY))
                    },
                )
            } else {
                Timber.e("Could not determine default content type for TOTP")
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE ->
                emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.JSON_RESOURCE_SCHEMA_VALIDATION_ERROR))
            SchemaEntity.SECRET ->
                emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.JSON_SECRET_SCHEMA_VALIDATION_ERROR))
        }
    }

    private fun linkedResourceReceived(resource: ResourceModel) {
        launch {
            updateViewState { copy(showProgress = true) }
            val updateOperation = createLinkTotpOperation(resource)
            performLinkTotpUpdate(updateOperation)
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun createLinkTotpOperation(resource: ResourceModel): suspend () -> Flow<ResourceUpdateActionResult> {
        val slug =
            idToSlugMappingProvider.provideMappingForSelectedAccount()[UUID.fromString(resource.resourceTypeId)]
                ?: return { emptyFlow() }

        val resourceUpdateActionsInteractor = resourceUpdateActionsInteractorFactory.create(resource)

        return when (ContentType.fromSlug(slug)) {
            is PasswordAndDescription, V5Default, is PasswordDescriptionTotp, V5DefaultWithTotp ->
                suspend {
                    resourceUpdateActionsInteractor.updateGenericResource(
                        updateAction = UpdateAction.ADD_TOTP,
                        secretModification = {
                            it.apply { totp = scannedTotp.toTotpSecret() }
                        },
                    )
                }
            else -> throw IllegalArgumentException("$slug resource type is not possible to link")
        }
    }

    private suspend fun performLinkTotpUpdate(updateOperation: suspend () -> Flow<ResourceUpdateActionResult>) {
        performResourceUpdateAction(
            action = updateOperation,
            doOnFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.GENERIC_ERROR)) },
            doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.GENERIC_ERROR)) },
            doOnCryptoFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.ENCRYPTION_ERROR, it)) },
            doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
            doOnSuccess = {
                emitSideEffect(NavigateToOtpList(totp = scannedTotp, otpCreated = true, resourceId = it.resourceId))
            },
            doOnCannotEditWithCurrentConfig = {
                emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.CANNOT_CREATE_WITH_CURRENT_CONFIG))
            },
            doOnMetadataKeyModified = {
                updateViewState { copy(metadataKeyToTrust = it, showNewMetadataTrustDialog = true) }
            },
            doOnMetadataKeyDeleted = {
                updateViewState { copy(metadataKeyDeleted = it, showTrustedMetadataKeyDeletedDialog = true) }
            },
            doOnMetadataKeyVerificationFailure = {
                emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.FAILED_TO_VERIFY_METADATA_KEY))
            },
        )
    }

    private fun trustNewMetadataKey(model: NewMetadataKeyToTrustModel) {
        launch {
            updateViewState { copy(showProgress = true, showNewMetadataTrustDialog = false, metadataKeyToTrust = null) }
            when (
                val output =
                    runAuthenticatedOperation {
                        metadataPrivateKeysHelperInteractor.trustNewKey(model)
                    }
            ) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    emitSideEffect(ShowSuccessSnackbar(SuccessSnackbarType.NEW_METADATA_KEY_IS_TRUSTED))
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.FAILED_TO_TRUST_METADATA_KEY))
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun trustedMetadataKeyDeleted() {
        launch {
            updateViewState {
                copy(metadataKeyDeleted = null, showTrustedMetadataKeyDeletedDialog = false)
            }
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
        }
    }

    private fun OtpParseResult.OtpQr.TotpQr.toTotpSecret() =
        TotpSecret(
            algorithm = algorithm.name,
            key = secret,
            period = period,
            digits = digits,
        )
}
