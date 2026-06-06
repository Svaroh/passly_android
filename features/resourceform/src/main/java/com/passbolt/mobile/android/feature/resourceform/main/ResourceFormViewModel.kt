package net.svaroh.passly.feature.resourceform.main

import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.common.validation.StringIsBase32
import net.svaroh.passly.common.validation.StringMaxLength
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.idlingresource.CreateResourceIdlingResource
import net.svaroh.passly.core.idlingresource.UpdateResourceIdlingResource
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.passwordgenerator.PinCodeGenerator
import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy
import net.svaroh.passly.core.passwordgenerator.SecretGenerator.SecretGenerationResult.Success
import net.svaroh.passly.core.passwordgenerator.codepoints.toCodepoints
import net.svaroh.passly.core.passwordgenerator.entropy.EntropyCalculator
import net.svaroh.passly.core.passwordgenerator.entropy.toPasswordStrength
import net.svaroh.passly.core.passwordgenerator.usecase.CheckPasswordPropertiesUseCase
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.ADD_METADATA_DESCRIPTION
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.ADD_NOTE
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.ADD_PASSWORD
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.ADD_PIN_CODE
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.ADD_TOTP
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.EDIT_ADDITIONAL_URIS
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.EDIT_APPEARANCE
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.EDIT_METADATA
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_METADATA_DESCRIPTION
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_NOTE
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_PASSWORD
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_PIN_CODE
import net.svaroh.passly.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_TOTP
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import net.svaroh.passly.domain.metadata.usecase.GetMetadataTypesSettingsUseCase
import net.svaroh.passly.domain.passwordexpiry.usecase.PasswordExpiryPoliciesInteractor
import net.svaroh.passly.domain.passwordpolicies.usecase.GetPasswordPoliciesUseCase
import net.svaroh.passly.domain.passwordpolicies.usecase.PasswordPoliciesInteractor
import net.svaroh.passly.domain.resources.actions.ResourceCreateActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourceUpdateActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourceUpdateActionsInteractorFactory
import net.svaroh.passly.domain.resources.actions.performResourceCreateAction
import net.svaroh.passly.domain.resources.actions.performResourceUpdateAction
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsConfirmationInteractor
import net.svaroh.passly.domain.resources.usecase.EditPermissionsConfirmationInteractor
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteFormViewModel
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteValidationError.MaxLengthExceeded
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeValidationError
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustBeBase32
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustNotBeEmpty
import net.svaroh.passly.feature.resourceform.main.GetOrLoadGeneratorSettingsUseCase.Input
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.AdditionalUrisResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.AdvancedSecretGenerationResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.AppearanceResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.ConfirmedPermissionsResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.CreateResource
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.CustomFieldsResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.DescriptionResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.DismissMetadataKeyDialog
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.DismissPasswordWarning
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.DismissUnableToGeneratePassword
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.ExpandAdvancedSettings
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GeneratePassword
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GeneratePinCode
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoBack
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToAdditionalNote
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToAdditionalPassword
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToAdditionalPinCode
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToAdditionalTotp
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToAdditionalUris
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToAppearance
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToCustomFields
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToMetadataDescription
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToPinCodeAdvancedGeneration
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToTotpMoreSettings
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.LearnMoreAboutUpgrade
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.NameTextChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.NoteChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.NoteResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.OpenAdvancedSecretGeneration
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PasswordMainUriTextChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PasswordResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PasswordTextChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PasswordUsernameTextChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PinCodeAdvancedGenerationResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PinCodeChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PinCodeResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.ProceedWithPasswordWarning
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.ScanOtpResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.ScanTotp
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.TotpAdvancedSettingsResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.TotpResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.TotpSecretChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.TotpUrlChanged
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.TrustNewMetadataKey
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.TrustedMetadataKeyDeleted
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.UpdateResource
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.UpgradeResource
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateBack
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithCreateSuccess
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithEditSuccess
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToAdditionalUris
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToAdvancedSecretGeneration
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToAppearance
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToConfirmPermissions
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToCustomFields
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToDescription
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToNote
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToPassword
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToPinCode
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToPinCodeAdvancedGeneration
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToScanOtp
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotp
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotpAdvancedSettings
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.OpenWebsite
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.ShowSnackbar
import net.svaroh.passly.feature.resourceform.main.ResourceFormSideEffect.ShowToast
import net.svaroh.passly.feature.resourceform.navigation.AdvancedSecretGenerationFormResult
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import net.svaroh.passly.jsonmodel.delegates.TotpSecret
import net.svaroh.passly.mappers.ResourceFormMapper
import net.svaroh.passly.serializers.jsonschema.SchemaEntity
import net.svaroh.passly.ui.AdditionalUrisUiModel
import net.svaroh.passly.ui.ConfirmPermissionsMode
import net.svaroh.passly.ui.Entropy
import net.svaroh.passly.ui.LeadingContentType
import net.svaroh.passly.ui.LeadingContentType.CUSTOM_FIELDS
import net.svaroh.passly.ui.LeadingContentType.PASSWORD
import net.svaroh.passly.ui.LeadingContentType.PIN_CODE
import net.svaroh.passly.ui.LeadingContentType.STANDALONE_NOTE
import net.svaroh.passly.ui.LeadingContentType.TOTP
import net.svaroh.passly.ui.MetadataIconModel
import net.svaroh.passly.ui.NewMetadataKeyToTrustModel
import net.svaroh.passly.ui.OtpParseResult
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel
import net.svaroh.passly.ui.PasswordUiModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PinCodeUiModel
import net.svaroh.passly.ui.ResourceAppearanceModel
import net.svaroh.passly.ui.ResourceAppearanceModel.Companion.DEFAULT_BACKGROUND_COLOR_HEX_STRING
import net.svaroh.passly.ui.ResourceAppearanceModel.Companion.ICON_TYPE_KEEPASS
import net.svaroh.passly.ui.ResourceAppearanceModel.Companion.ICON_TYPE_PASSBOLT
import net.svaroh.passly.ui.ResourceFormMode
import net.svaroh.passly.ui.ResourceFormMode.Create
import net.svaroh.passly.ui.ResourceFormMode.Edit
import net.svaroh.passly.ui.ResourceFormUiModel
import net.svaroh.passly.ui.TotpUiModel
import net.svaroh.passly.ui.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber

@Suppress("TooManyFunctions", "LargeClass")
class ResourceFormViewModel(
    private val mode: ResourceFormMode,
    private val getPasswordPoliciesUseCase: GetPasswordPoliciesUseCase,
    private val getOrLoadGeneratorSettingsUseCase: GetOrLoadGeneratorSettingsUseCase,
    private val passwordPoliciesInteractor: PasswordPoliciesInteractor,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val passwordExpiryPoliciesInteractor: PasswordExpiryPoliciesInteractor,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val secretGenerator: SecretGenerator,
    private val pinCodeGenerator: PinCodeGenerator,
    private val entropyCalculator: EntropyCalculator,
    private val resourceFormMapper: ResourceFormMapper,
    private val resourceModelHandler: ResourceModelHandler,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    private val createResourceIdlingResource: CreateResourceIdlingResource,
    private val updateResourceIdlingResource: UpdateResourceIdlingResource,
    private val resourceUpdateActionsInteractorFactory: ResourceUpdateActionsInteractorFactory,
    private val checkPasswordPropertiesUseCase: CheckPasswordPropertiesUseCase,
    private val getMetadataTypesSettingsUseCase: GetMetadataTypesSettingsUseCase,
    private val editPermissionsConfirmationInteractor: EditPermissionsConfirmationInteractor,
    private val createPermissionsConfirmationInteractor: CreatePermissionsConfirmationInteractor,
) : SideEffectViewModel<ResourceFormState, ResourceFormSideEffect>(ResourceFormState(mode = mode)),
    KoinComponent {
    private val uiModel: ResourceFormUiModel by lazy {
        resourceModelHandler.getUiModel(mode)
    }
    private var parentFolderId: String? = null
    private var isUpgradePendingPermissionsConfirmation = false

    init {
        initialize()
    }

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: ResourceFormIntent) {
        when (intent) {
            is NameTextChanged -> nameTextChanged(intent.name)
            ExpandAdvancedSettings -> expandAdvancedSettings()
            CreateResource -> createResource()
            is ConfirmedPermissionsResult -> onPermissionsConfirmed(intent.permissions)
            UpdateResource -> updateResource()
            is PasswordTextChanged -> passwordTextChanged(intent.password)
            GeneratePassword -> generatePassword()
            DismissUnableToGeneratePassword ->
                updateViewState { copy(isUnableToGeneratePasswordDialogVisible = false) }
            OpenAdvancedSecretGeneration -> openAdvancedSecretGeneration()
            is AdvancedSecretGenerationResult -> advancedSecretGenerationResult(intent.result)
            is PasswordMainUriTextChanged -> passwordMainUriTextChanged(intent.mainUri)
            is PasswordUsernameTextChanged -> passwordUsernameTextChanged(intent.username)
            is TotpSecretChanged -> totpSecretChanged(intent.totpSecret)
            is TotpUrlChanged -> totpUrlChanged(intent.url)
            GoToTotpMoreSettings -> goToTotpMoreSettings()
            ScanTotp -> scanTotp()
            is NoteChanged -> noteChanged(intent.note)
            is PinCodeChanged -> pinCodeChanged(intent.pinCode)
            GeneratePinCode -> generatePinCode(viewState.value.pinCodeData.length)
            GoToPinCodeAdvancedGeneration -> goToPinCodeAdvancedGeneration()
            is PinCodeAdvancedGenerationResult -> generatePinCode(intent.pinCodeUiModel.length)
            GoToAdditionalNote -> goToAdditionalNote()
            GoToAdditionalPassword -> goToAdditionalPassword()
            GoToAdditionalPinCode -> goToAdditionalPinCode()
            is GoToAdditionalTotp -> goToAdditionalTotp()
            GoToAdditionalTotp -> goToAdditionalTotp()
            GoToCustomFields -> goToCustomFields()
            GoToMetadataDescription -> goToMetadataDescription()
            GoToAppearance -> goToAppearance()
            GoToAdditionalUris -> goToAdditionalUris()
            is PasswordResult -> passwordResult(intent.passwordUiModel)
            is TotpResult -> totpResult(intent.totpUiModel)
            is TotpAdvancedSettingsResult -> totpAdvancedSettingsResult(intent.totpAdvancedSettings)
            is NoteResult -> noteResult(intent.note)
            is PinCodeResult -> pinCodeResult(intent.pinCodeUiModel)
            is DescriptionResult -> descriptionResult(intent.metadataDescription)
            is AppearanceResult -> appearanceResult(intent.model)
            is AdditionalUrisResult -> additionalUrisResult(intent.urisUiModel)
            is CustomFieldsResult -> customFieldsResult()
            is ScanOtpResult -> scanOtpResult(intent.isManualCreationChosen, intent.scannedTotp)
            is TrustNewMetadataKey -> trustNewMetadataKey(intent.model)
            is TrustedMetadataKeyDeleted -> trustedMetadataKeyDeleted()
            DismissMetadataKeyDialog -> dismissMetadataKeyDialog()
            ProceedWithPasswordWarning -> proceedWithPasswordWarning()
            DismissPasswordWarning -> dismissPasswordWarning()
            UpgradeResource -> upgradeResource()
            LearnMoreAboutUpgrade -> emitSideEffect(OpenWebsite(LEARN_MORE_UPGRADE_URL))
            GoBack -> goBack()
        }
    }

    private fun scanTotp() {
        emitSideEffect(NavigateToScanOtp)
    }

    private fun dismissMetadataKeyDialog() {
        updateViewState {
            copy(metadataKeyModifiedDialog = null, metadataKeyDeletedDialog = null)
        }
    }

    private fun goBack() {
        emitSideEffect(NavigateBack)
    }

    private fun initialize() {
        launch {
            updateViewState { copy(shouldShowScreenProgress = true) }
            awaitAll(
                async(coroutineLaunchContext.io) { fetchPasswordPolicies() },
                async(coroutineLaunchContext.io) { fetchPasswordExpiry() },
            )
            dataRefreshTrackingFlow.awaitIdle()
            when (mode) {
                is Create -> {
                    try {
                        Timber.d("Initializing model with leading content type: ${mode.leadingContentType}")
                        parentFolderId = mode.parentFolderId
                        resourceModelHandler.initializeModelForCreation(mode.leadingContentType)
                    } catch (_: Exception) {
                        emitSideEffect(ShowToast(ToastMessage.CREATE_INITIALIZATION_ERROR))
                        emitSideEffect(NavigateBack)
                        return@launch
                    }
                }
                is Edit -> {
                    Timber.d("Initializing model for edition")
                    try {
                        resourceModelHandler.initializeModelForEdition(mode.resourceId)
                    } catch (_: Exception) {
                        emitSideEffect(ShowToast(ToastMessage.EDIT_INITIALIZATION_ERROR))
                        emitSideEffect(NavigateBack)
                        return@launch
                    }
                }
            }

            setupState()
            updateViewState { copy(shouldShowScreenProgress = false) }
        }
    }

    private suspend fun fetchPasswordPolicies() {
        if (getFeatureFlagsUseCase.execute(Unit).featureFlags.arePasswordPoliciesAvailable) {
            Timber.d("Password policies available, fetching password policies settings")
            when (val output = runAuthenticatedOperation { passwordPoliciesInteractor.fetchAndSavePasswordPolicies() }) {
                is PasswordPoliciesInteractor.Output.Success ->
                    Timber.d("Password policies fetched")
                is PasswordPoliciesInteractor.Output.Failure -> {
                    Timber.e("Failed to fetch password policies, using default values")
                    emitSideEffect(ShowSnackbar(SnackbarMessage.PASSWORD_POLICIES_FETCH_FAILED))
                }
            }
        } else {
            Timber.d("Password policies not available")
        }
    }

    private suspend fun fetchPasswordExpiry() {
        if (getFeatureFlagsUseCase.execute(Unit).featureFlags.isPasswordExpiryAvailable) {
            Timber.d("Password expiry available, fetching password expiry settings")
            when (val output = runAuthenticatedOperation { passwordExpiryPoliciesInteractor.fetchAndSavePasswordExpiryPolicies() }) {
                is PasswordExpiryPoliciesInteractor.Output.Success ->
                    Timber.d("Password expiry fetched")
                is PasswordExpiryPoliciesInteractor.Output.Failure -> {
                    Timber.e("Failed to fetch password expiry, using default values")
                    emitSideEffect(ShowSnackbar(SnackbarMessage.PASSWORD_EXPIRY_FETCH_FAILED))
                }
            }
        } else {
            Timber.d("Password expiry not available")
        }
    }

    private suspend fun setupState() {
        val leadingContentType = uiModel.leadingContentType
        val areAdvancedSettingsExpanded = viewState.value.areAdvancedSettingsExpanded
        val showUpgradePanel = computeShowUpgradePanel()

        updateViewState {
            copy(
                name = resourceModelHandler.resourceMetadata.name,
                leadingContentType = leadingContentType,
                isPrimaryButtonVisible = true,
                showUpgradePanel = showUpgradePanel,
            )
        }

        setupLeadingContentType(leadingContentType)

        if (areAdvancedSettingsExpanded) {
            updateViewState {
                copy(
                    supportedAdditionalSecrets = uiModel.supportedAdditionalSecrets,
                    supportedMetadata = uiModel.supportedMetadata,
                    areAdvancedSettingsVisible = false,
                )
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun setupLeadingContentType(leadingContentType: LeadingContentType) {
        val resourceMetadata = resourceModelHandler.resourceMetadata
        val resourceSecret = resourceModelHandler.resourceSecret
        val contentType = resourceModelHandler.contentType
        when (leadingContentType) {
            TOTP -> {
                val totpUiModel =
                    resourceFormMapper.mapToUiModel(resourceSecret.totp, resourceMetadata.name)
                updateViewState {
                    copy(
                        totpData =
                            totpData.copy(
                                totpUiModel = totpUiModel,
                                totpIssuer = resourceMetadata.getMainUri(contentType),
                                totpSecret = resourceSecret.totp?.key.orEmpty(),
                            ),
                    )
                }
            }
            PASSWORD -> {
                val password = resourceSecret.getPassword(contentType).orEmpty()
                val entropy = entropyCalculator.getSecretEntropy(password)
                val passwordStrength = Entropy.parse(entropy).toPasswordStrength()
                updateViewState {
                    copy(
                        passwordData =
                            passwordData.copy(
                                mainUri = resourceMetadata.getMainUri(contentType),
                                username = resourceMetadata.username.orEmpty(),
                                password =
                                    password
                                        .toCodepoints()
                                        .map { Character.toChars(it.value) }
                                        .joinToString("") { String(it) },
                                passwordStrength = passwordStrength,
                                passwordEntropyBits = entropy,
                            ),
                    )
                }
            }
            CUSTOM_FIELDS -> {
                // no leading form to setup
            }
            STANDALONE_NOTE -> {
                updateViewState {
                    copy(noteData = noteData.copy(note = resourceSecret.description.orEmpty()))
                }
            }
            PIN_CODE -> {
                updateViewState {
                    copy(
                        pinCodeData =
                            pinCodeData.copy(
                                pinCode = resourceSecret.pinCode.orEmpty(),
                                length = pinCodeData.length,
                            ),
                    )
                }
            }
        }
    }

    private fun expandAdvancedSettings() {
        updateViewState {
            copy(
                supportedAdditionalSecrets = uiModel.supportedAdditionalSecrets,
                supportedMetadata = uiModel.supportedMetadata,
                areAdvancedSettingsVisible = false,
                areAdvancedSettingsExpanded = true,
            )
        }
    }

    private fun nameTextChanged(name: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.name = name
        }
        updateViewState { copy(name = name) }
    }

    private fun passwordTextChanged(password: String) {
        resourceModelHandler.applyModelChange(
            if (password.isBlank()) REMOVE_PASSWORD else ADD_PASSWORD,
        ) { _, secret ->
            secret.setPassword(resourceModelHandler.contentType, password)
        }
        updateViewState { copy(passwordData = passwordData.copy(password = password)) }
        launch {
            val entropy = entropyCalculator.getSecretEntropy(password)
            updateViewState {
                copy(
                    passwordData =
                        passwordData.copy(
                            passwordStrength = Entropy.parse(entropy).toPasswordStrength(),
                            passwordEntropyBits = entropy,
                        ),
                )
            }
        }
    }

    private fun generatePassword() {
        launch {
            val (type, passwordSettings, passphraseSettings) = getOrLoadGeneratorSettings()
            val secretGenerationResult =
                when (type) {
                    PasswordGeneratorTypeUiModel.PASSWORD ->
                        secretGenerator.generatePassword(passwordSettings)
                    PasswordGeneratorTypeUiModel.PASSPHRASE ->
                        secretGenerator.generatePassphrase(passphraseSettings)
                }
            when (secretGenerationResult) {
                is FailedToGenerateLowEntropy ->
                    updateViewState {
                        copy(
                            isUnableToGeneratePasswordDialogVisible = true,
                            minimumEntropyBits = secretGenerationResult.minimumEntropyBits,
                        )
                    }
                is Success -> {
                    val password = secretGenerationResult.password
                    val entropy = secretGenerationResult.entropy
                    val passwordStr = password.map { Character.toChars(it.value) }.joinToString("") { String(it) }
                    applyGeneratedPassword(passwordStr, entropy)
                }
            }
        }
    }

    private fun openAdvancedSecretGeneration() {
        launch {
            val (type, passwordSettings, passphraseSettings) = getOrLoadGeneratorSettings()
            emitSideEffect(
                NavigateToAdvancedSecretGeneration(
                    selectedTab = type,
                    passwordSettings = passwordSettings,
                    passphraseSettings = passphraseSettings,
                ),
            )
        }
    }

    private fun advancedSecretGenerationResult(result: AdvancedSecretGenerationFormResult) {
        updateViewState {
            copy(
                generatorType = result.selectedTab,
                passwordGeneratorSettings = result.passwordSettings,
                passphraseGeneratorSettings = result.passphraseSettings,
            )
        }
        launch {
            val entropy = entropyCalculator.getSecretEntropy(result.generatedSecret)
            applyGeneratedPassword(result.generatedSecret, entropy)
        }
    }

    private fun applyGeneratedPassword(
        passwordStr: String,
        entropy: Double,
    ) {
        resourceModelHandler.applyModelChange(ADD_PASSWORD) { _, secret ->
            secret.setPassword(resourceModelHandler.contentType, passwordStr)
        }
        updateViewState {
            copy(
                passwordData =
                    passwordData.copy(
                        password = passwordStr,
                        passwordStrength = Entropy.parse(entropy).toPasswordStrength(),
                        passwordEntropyBits = entropy,
                    ),
            )
        }
    }

    private suspend fun getOrLoadGeneratorSettings(): GeneratorSettings {
        val state = viewState.value
        val (settings, wasLoaded) =
            getOrLoadGeneratorSettingsUseCase.execute(
                Input(
                    type = state.generatorType,
                    passwordSettings = state.passwordGeneratorSettings,
                    passphraseSettings = state.passphraseGeneratorSettings,
                ),
            )
        if (wasLoaded) {
            updateViewState {
                copy(
                    generatorType = settings.type,
                    passwordGeneratorSettings = settings.passwordSettings,
                    passphraseGeneratorSettings = settings.passphraseSettings,
                )
            }
        }
        return settings
    }

    private fun passwordMainUriTextChanged(mainUri: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.setMainUri(resourceModelHandler.contentType, mainUri)
        }
        updateViewState { copy(passwordData = passwordData.copy(mainUri = mainUri)) }
    }

    private fun passwordUsernameTextChanged(username: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.username = username
        }
        updateViewState { copy(passwordData = passwordData.copy(username = username)) }
    }

    private fun totpSecretChanged(totpSecret: String) {
        resourceModelHandler.applyModelChange(
            if (totpSecret.isBlank()) REMOVE_TOTP else ADD_TOTP,
        ) { _, secret ->
            secret.totp = requireNotNull(secret.totp).copy(key = totpSecret)
        }
        updateViewState { copy(totpData = totpData.copy(totpSecret = totpSecret, totpSecretError = null)) }
    }

    private fun totpUrlChanged(url: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.setMainUri(resourceModelHandler.contentType, url)
        }
        updateViewState { copy(totpData = totpData.copy(totpIssuer = url)) }
    }

    private fun goToTotpMoreSettings() {
        val totpUiModel =
            resourceFormMapper.mapToUiModel(
                resourceModelHandler.resourceSecret.totp,
                resourceModelHandler.resourceMetadata.getMainUri(resourceModelHandler.contentType),
            )
        emitSideEffect(NavigateToTotpAdvancedSettings(mode, totpUiModel))
    }

    private fun noteChanged(note: String) {
        resourceModelHandler.applyModelChange(
            if (note.isBlank()) REMOVE_NOTE else ADD_NOTE,
        ) { _, secret ->
            secret.description = note
        }
        updateViewState { copy(noteData = noteData.copy(note = note, noteError = null)) }
    }

    private fun pinCodeChanged(pinCode: String) {
        val sanitized = pinCode.take(PinCodeUiModel.MAX_LENGTH).filter(Char::isDigit)
        resourceModelHandler.applyModelChange(
            if (sanitized.isBlank()) REMOVE_PIN_CODE else ADD_PIN_CODE,
        ) { _, secret ->
            secret.pinCode = sanitized
        }
        updateViewState {
            copy(pinCodeData = pinCodeData.copy(pinCode = sanitized, pinCodeError = null))
        }
    }

    private fun generatePinCode(length: Int) {
        Timber.d("Generating PIN code (length=$length)")
        val generated = pinCodeGenerator.generate(length)
        resourceModelHandler.applyModelChange(ADD_PIN_CODE) { _, secret ->
            secret.pinCode = generated
        }
        updateViewState {
            copy(pinCodeData = pinCodeData.copy(pinCode = generated, length = length, pinCodeError = null))
        }
    }

    private fun goToPinCodeAdvancedGeneration() {
        val pinCode = resourceModelHandler.resourceSecret.pinCode.orEmpty()
        val length = viewState.value.pinCodeData.length
        emitSideEffect(NavigateToPinCodeAdvancedGeneration(mode, PinCodeUiModel(pinCode = pinCode, length = length)))
    }

    private fun goToAdditionalNote() {
        emitSideEffect(NavigateToNote(mode, resourceModelHandler.resourceSecret.description.orEmpty()))
    }

    private fun goToAdditionalTotp() {
        emitSideEffect(
            NavigateToTotp(
                mode,
                resourceFormMapper.mapToUiModel(
                    resourceModelHandler.resourceSecret.totp,
                    resourceModelHandler.resourceMetadata.getMainUri(resourceModelHandler.contentType),
                ),
            ),
        )
    }

    private fun goToAdditionalPassword() {
        val resourceMetadata = resourceModelHandler.resourceMetadata
        val contentType = resourceModelHandler.contentType
        emitSideEffect(
            NavigateToPassword(
                mode,
                resourceFormMapper.mapToUiModel(
                    resourceModelHandler.resourceSecret.getPassword(contentType).orEmpty(),
                    resourceMetadata.getMainUri(contentType),
                    resourceMetadata.username.orEmpty(),
                ),
            ),
        )
    }

    private fun goToAdditionalPinCode() {
        val pinCode = resourceModelHandler.resourceSecret.pinCode.orEmpty()
        val length = viewState.value.pinCodeData.length
        emitSideEffect(NavigateToPinCode(mode, PinCodeUiModel(pinCode = pinCode, length = length)))
    }

    private fun goToCustomFields() {
        val customFieldsModel =
            resourceFormMapper.mapToUiModel(
                resourceModelHandler.resourceMetadata.customFields,
                resourceModelHandler.resourceSecret.customFields,
            )
        emitSideEffect(
            NavigateToCustomFields(
                mode,
                resourceFormMapper.mapToCustomFieldsUiModel(customFieldsModel),
            ),
        )
    }

    private fun goToMetadataDescription() {
        emitSideEffect(NavigateToDescription(mode, resourceModelHandler.resourceMetadata.description.orEmpty()))
    }

    private fun goToAppearance() {
        val appearanceModel = resourceFormMapper.mapToUiModel(resourceModelHandler.resourceMetadata.icon)
        emitSideEffect(NavigateToAppearance(mode, appearanceModel))
    }

    private fun goToAdditionalUris() {
        val resourceMetadata = resourceModelHandler.resourceMetadata
        val mainUri = resourceMetadata.getMainUri(resourceModelHandler.contentType)
        val additionalUris = resourceMetadata.uris.orEmpty().filter { it != mainUri }
        emitSideEffect(
            NavigateToAdditionalUris(
                mode,
                AdditionalUrisUiModel(mainUri = mainUri, additionalUris = additionalUris),
            ),
        )
    }

    private fun passwordResult(passwordUiModel: PasswordUiModel?) {
        val contentType = resourceModelHandler.contentType
        passwordUiModel?.let {
            val passwordEvent = if (passwordUiModel.password.isBlank()) REMOVE_PASSWORD else ADD_PASSWORD
            resourceModelHandler.applyModelChange(passwordEvent) { _, secret ->
                secret.setPassword(contentType, passwordUiModel.password)
            }
            resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
                metadata.username = passwordUiModel.username
                metadata.setMainUri(contentType, passwordUiModel.mainUri)
            }
        }
    }

    private fun totpResult(totpUiModel: TotpUiModel?) {
        val totpAction = if (totpUiModel == null || totpUiModel.secret.isBlank()) REMOVE_TOTP else ADD_TOTP
        resourceModelHandler.applyModelChange(totpAction) { _, secret ->
            secret.totp = resourceFormMapper.mapToJsonModel(totpUiModel)
        }
        if (totpUiModel != null) {
            resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
                metadata.setMainUri(resourceModelHandler.contentType, totpUiModel.issuer)
            }
        }
    }

    private fun totpAdvancedSettingsResult(totpAdvancedSettings: TotpUiModel?) {
        resourceModelHandler.applyModelChange(ADD_TOTP) { _, secret ->
            val settings =
                totpAdvancedSettings ?: TotpUiModel.emptyWithDefaults(
                    resourceModelHandler.resourceMetadata.getMainUri(resourceModelHandler.contentType),
                )
            secret.totp =
                requireNotNull(resourceModelHandler.resourceSecret.totp).copy(
                    algorithm = settings.algorithm,
                    digits = settings.length.toInt(),
                    period = settings.expiry.toLong(),
                )
        }
    }

    private fun noteResult(note: String?) {
        resourceModelHandler.applyModelChange(
            if (note.isNullOrBlank()) REMOVE_NOTE else ADD_NOTE,
        ) { _, secret ->
            secret.description = note
        }
    }

    private fun pinCodeResult(pinCodeUiModel: PinCodeUiModel?) {
        val action = if (pinCodeUiModel == null || pinCodeUiModel.pinCode.isBlank()) REMOVE_PIN_CODE else ADD_PIN_CODE
        resourceModelHandler.applyModelChange(action) { _, secret ->
            secret.pinCode = pinCodeUiModel?.pinCode
        }
        updateViewState {
            copy(
                pinCodeData =
                    pinCodeData.copy(
                        pinCode = pinCodeUiModel?.pinCode.orEmpty(),
                        length = pinCodeUiModel?.length ?: pinCodeData.length,
                        pinCodeError = null,
                    ),
            )
        }
    }

    private fun descriptionResult(metadataDescription: String?) {
        resourceModelHandler.applyModelChange(
            if (metadataDescription.isNullOrBlank()) REMOVE_METADATA_DESCRIPTION else ADD_METADATA_DESCRIPTION,
        ) { metadata, _ ->
            metadata.description = metadataDescription
        }
    }

    private fun appearanceResult(model: ResourceAppearanceModel?) {
        resourceModelHandler.applyModelChange(EDIT_APPEARANCE) { metadata, _ ->
            val iconType = model?.iconType ?: ICON_TYPE_PASSBOLT
            val iconValue =
                when (iconType) {
                    ICON_TYPE_KEEPASS -> model?.iconValue
                    else -> null
                }
            val iconBackgroundColorHex = model?.iconBackgroundHexColor ?: DEFAULT_BACKGROUND_COLOR_HEX_STRING
            metadata.icon =
                MetadataIconModel(
                    type = iconType,
                    value = iconValue,
                    backgroundColorHexString = iconBackgroundColorHex,
                )
        }
    }

    private fun additionalUrisResult(urisUiModel: AdditionalUrisUiModel?) {
        urisUiModel?.let {
            val uris = listOf(urisUiModel.mainUri) + urisUiModel.additionalUris
            resourceModelHandler.applyModelChange(EDIT_ADDITIONAL_URIS) { metadata, _ ->
                metadata.uris =
                    if (uris.all { it.isBlank() }) {
                        emptyList()
                    } else {
                        uris.map { it.trim() }.filter { it.isNotBlank() }
                    }
            }
        }
    }

    private fun customFieldsResult() {
        // TODO not supported for now
    }

    private fun scanOtpResult(
        isManualCreationChosen: Boolean,
        scannedTotp: OtpParseResult.OtpQr.TotpQr?,
    ) {
        if (isManualCreationChosen) return

        scannedTotp?.let {
            resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
                metadata.setMainUri(resourceModelHandler.contentType, it.issuer.orEmpty())
                metadata.name = it.label
            }
            resourceModelHandler.applyModelChange(ADD_TOTP) { _, secret ->
                secret.totp =
                    TotpSecret(
                        key = it.secret,
                        algorithm = it.algorithm.name,
                        digits = it.digits,
                        period = it.period,
                    )
            }
            updateViewState {
                copy(
                    name = it.label,
                    totpData =
                        totpData.copy(
                            totpSecret = it.secret,
                            totpIssuer = it.issuer.orEmpty(),
                        ),
                )
            }
        }
    }

    private fun createResource() {
        onValid {
            checkPasswordAndProceed { proceedWithCreate() }
        }
    }

    private fun proceedWithCreate() {
        launch {
            isUpgradePendingPermissionsConfirmation = false
            if (shouldConfirmPermissions()) {
                Timber.d("Creating inside a shared folder - navigating to permissions confirmation")
                emitSideEffect(
                    NavigateToConfirmPermissions(ConfirmPermissionsMode.Create(requireNotNull(parentFolderId))),
                )
            } else {
                performCreate()
            }
        }
    }

    private suspend fun shouldConfirmPermissions(): Boolean =
        createPermissionsConfirmationInteractor.shouldConfirmPermissions(parentFolderId)

    private fun updateResource() {
        onValid {
            checkPasswordAndProceed { proceedWithUpdate() }
        }
    }

    private fun proceedWithUpdate() {
        launch {
            isUpgradePendingPermissionsConfirmation = false
            if (shouldConfirmEditPermissions()) {
                Timber.d("Editing a shared resource - navigating to permissions confirmation")
                emitSideEffect(
                    NavigateToConfirmPermissions(ConfirmPermissionsMode.Edit((mode as Edit).resourceId)),
                )
            } else {
                performUpdate()
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun shouldConfirmEditPermissions(): Boolean {
        val resourceId = (mode as? Edit)?.resourceId ?: return false
        if (!resourceModelHandler.isSecretModified()) {
            Timber.d("Secret not modified - updating without permissions confirmation")
            return false
        }
        return editPermissionsConfirmationInteractor.shouldConfirmPermissions(resourceId)
    }

    private fun checkPasswordAndProceed(onProceed: () -> Unit) {
        launch {
            val password = viewState.value.passwordData.password
            if (!resourceModelHandler.contentType.hasPassword() || password.isBlank()) {
                onProceed()
                return@launch
            }

            val passwordPolicies = getPasswordPoliciesUseCase.execute(Unit)
            if (!passwordPolicies.isExternalDictionaryCheckEnabled) {
                onProceed()
                return@launch
            }

            when (checkPasswordPropertiesUseCase.execute(CheckPasswordPropertiesUseCase.Input(password))) {
                is CheckPasswordPropertiesUseCase.Output.Fine -> onProceed()
                is CheckPasswordPropertiesUseCase.Output.Pwned ->
                    updateViewState { copy(showPasswordWarningDialog = true, passwordWarningType = PasswordWarningType.DATA_BREACH) }
                is CheckPasswordPropertiesUseCase.Output.Weak ->
                    updateViewState { copy(showPasswordWarningDialog = true, passwordWarningType = PasswordWarningType.LOW_ENTROPY) }
                is CheckPasswordPropertiesUseCase.Output.Failure -> {
                    Timber.d("Failed to check password status")
                    onProceed()
                }
            }
        }
    }

    private fun proceedWithPasswordWarning() {
        updateViewState { copy(showPasswordWarningDialog = false, passwordWarningType = null) }
        when (mode) {
            is Create -> proceedWithCreate()
            is Edit -> proceedWithUpdate()
        }
    }

    private fun dismissPasswordWarning() {
        updateViewState { copy(showPasswordWarningDialog = false, passwordWarningType = null) }
    }

    private fun performCreate() {
        launch {
            createResourceIdlingResource.setIdle(false)
            updateViewState { copy(shouldShowDialogProgress = true) }
            val resourceCreateActionsInteractor = get<ResourceCreateActionsInteractor>()
            performResourceCreateAction(
                action = {
                    resourceCreateActionsInteractor.createGenericResource(
                        resourceModelHandler.contentType,
                        parentFolderId,
                        resourceModelHandler.getResourceMetadataWithRequiredFields(),
                        resourceModelHandler.getResourceSecretWithRequiredFields(),
                    )
                },
                doOnFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.COMMON_FAILURE)) },
                doOnCryptoFailure = {
                    emitSideEffect(ShowSnackbar(SnackbarMessage.ENCRYPTION_FAILURE))
                },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = {
                    emitSideEffect(NavigateBackWithCreateSuccess(it.resourceName, it.resourceId))
                },
                doOnCannotCreateWithCurrentConfig = {
                    emitSideEffect(
                        ShowSnackbar(SnackbarMessage.CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG),
                    )
                },
                doOnMetadataKeyModified = {
                    updateViewState { copy(metadataKeyModifiedDialog = it) }
                },
                doOnMetadataKeyDeleted = {
                    updateViewState { copy(metadataKeyDeletedDialog = it) }
                },
                doOnMetadataKeyVerificationFailure = {
                    emitSideEffect(
                        ShowSnackbar(SnackbarMessage.METADATA_KEY_VERIFICATION_FAILURE),
                    )
                },
            )
            updateViewState { copy(shouldShowDialogProgress = false) }
            createResourceIdlingResource.setIdle(true)
        }
    }

    private fun onPermissionsConfirmed(confirmedPermissions: List<PermissionModelUi>) {
        when {
            isUpgradePendingPermissionsConfirmation -> {
                isUpgradePendingPermissionsConfirmation = false
                performUpgradeWithConfirmedPermissions(confirmedPermissions)
            }
            mode is Edit -> performUpdateWithConfirmedPermissions(confirmedPermissions)
            mode is Create -> performCreateWithConfirmedPermissions(confirmedPermissions)
        }
    }

    private fun performCreateWithConfirmedPermissions(confirmedPermissions: List<PermissionModelUi>) {
        launch {
            createResourceIdlingResource.setIdle(false)
            updateViewState { copy(shouldShowDialogProgress = true) }
            val resourceCreateActionsInteractor = get<ResourceCreateActionsInteractor>()
            performResourceCreateAction(
                action = {
                    resourceCreateActionsInteractor.createGenericResourceWithConfirmedPermissions(
                        resourceModelHandler.contentType,
                        parentFolderId,
                        resourceModelHandler.getResourceMetadataWithRequiredFields(),
                        resourceModelHandler.getResourceSecretWithRequiredFields(),
                        confirmedPermissions,
                    )
                },
                doOnFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.COMMON_FAILURE)) },
                doOnCryptoFailure = {
                    emitSideEffect(ShowSnackbar(SnackbarMessage.ENCRYPTION_FAILURE))
                },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = {
                    emitSideEffect(NavigateBackWithCreateSuccess(it.resourceName, it.resourceId))
                },
                doOnShareFailure = { navigateBackWithCreatedButNotShared(ToastMessage.RESOURCE_CREATED_SHARE_FAILED) },
                doOnFetchFailure = { navigateBackWithCreatedButNotShared(ToastMessage.RESOURCE_CREATED_SHARE_FAILED) },
                doOnPermissionsDrifted = {
                    navigateBackWithCreatedButNotShared(ToastMessage.RESOURCE_CREATED_PERMISSIONS_CHANGED)
                },
                doOnCannotCreateWithCurrentConfig = {
                    emitSideEffect(
                        ShowSnackbar(SnackbarMessage.CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG),
                    )
                },
                doOnMetadataKeyModified = {
                    updateViewState { copy(metadataKeyModifiedDialog = it) }
                },
                doOnMetadataKeyDeleted = {
                    updateViewState { copy(metadataKeyDeletedDialog = it) }
                },
                doOnMetadataKeyVerificationFailure = {
                    emitSideEffect(
                        ShowSnackbar(SnackbarMessage.METADATA_KEY_VERIFICATION_FAILURE),
                    )
                },
            )
            updateViewState { copy(shouldShowDialogProgress = false) }
            createResourceIdlingResource.setIdle(true)
        }
    }

    private fun navigateBackWithCreatedButNotShared(message: ToastMessage) {
        Timber.e("Resource created but sharing to the confirmed recipients did not complete.")
        emitSideEffect(ShowToast(message))
        emitSideEffect(
            NavigateBackWithCreateSuccess(
                resourceModelHandler.resourceMetadata.name,
                resourceId = "",
            ),
        )
    }

    private fun performUpdateWithConfirmedPermissions(confirmedPermissions: List<PermissionModelUi>) {
        launch {
            updateResourceIdlingResource.setIdle(false)
            updateViewState { copy(shouldShowDialogProgress = true) }
            val editedResource =
                getLocalResourceUseCase
                    .execute(
                        GetLocalResourceUseCase.Input((mode as Edit).resourceId),
                    ).resource
            val resourceUpdateActionsInteractor = resourceUpdateActionsInteractorFactory.create(editedResource)
            performResourceUpdateAction(
                action = {
                    resourceUpdateActionsInteractor.updateGenericResourceWithConfirmedPermissions(
                        resourceModelHandler.contentType,
                        confirmedPermissions,
                        { resourceModelHandler.getResourceMetadataWithRequiredFields() },
                        { resourceModelHandler.getResourceSecretWithRequiredFields() },
                    )
                },
                doOnFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.COMMON_FAILURE)) },
                doOnCryptoFailure = {
                    emitSideEffect(ShowSnackbar(SnackbarMessage.ENCRYPTION_FAILURE))
                },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = { emitSideEffect(NavigateBackWithEditSuccess(resourceModelHandler.resourceMetadata.name)) },
                doOnShareFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.RESOURCE_EDITED_SHARE_FAILED)) },
                doOnFetchFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.RESOURCE_EDITED_SHARE_FAILED)) },
                doOnPermissionsDrifted = { drifted ->
                    Timber.d("Permissions drifted before saving the edit - reopening the confirmation")
                    emitSideEffect(
                        NavigateToConfirmPermissions(
                            ConfirmPermissionsMode.Edit((mode as Edit).resourceId),
                            driftedEntityNames = drifted.driftedEntityNames,
                        ),
                    )
                },
                doOnCannotEditWithCurrentConfig = {
                    emitSideEffect(
                        ShowSnackbar(SnackbarMessage.CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG),
                    )
                },
                doOnMetadataKeyModified = {
                    updateViewState { copy(metadataKeyModifiedDialog = it) }
                },
                doOnMetadataKeyDeleted = {
                    updateViewState { copy(metadataKeyDeletedDialog = it) }
                },
                doOnMetadataKeyVerificationFailure = {
                    emitSideEffect(
                        ShowSnackbar(SnackbarMessage.METADATA_KEY_VERIFICATION_FAILURE),
                    )
                },
            )
            updateViewState { copy(shouldShowDialogProgress = false) }
            updateResourceIdlingResource.setIdle(true)
        }
    }

    private fun performUpdate() {
        launch {
            updateResourceIdlingResource.setIdle(false)
            updateViewState { copy(shouldShowDialogProgress = true) }
            val editedResource =
                getLocalResourceUseCase
                    .execute(
                        GetLocalResourceUseCase.Input((mode as Edit).resourceId),
                    ).resource
            val resourceUpdateActionsInteractor = resourceUpdateActionsInteractorFactory.create(editedResource)
            performResourceUpdateAction(
                action = {
                    resourceUpdateActionsInteractor.updateGenericResource(
                        resourceModelHandler.contentType,
                        { resourceModelHandler.getResourceMetadataWithRequiredFields() },
                        { resourceModelHandler.getResourceSecretWithRequiredFields() },
                    )
                },
                doOnFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.COMMON_FAILURE)) },
                doOnCryptoFailure = {
                    emitSideEffect(ShowSnackbar(SnackbarMessage.ENCRYPTION_FAILURE))
                },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = { emitSideEffect(NavigateBackWithEditSuccess(resourceModelHandler.resourceMetadata.name)) },
                doOnCannotEditWithCurrentConfig = {
                    emitSideEffect(
                        ShowSnackbar(SnackbarMessage.CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG),
                    )
                },
                doOnMetadataKeyModified = {
                    updateViewState { copy(metadataKeyModifiedDialog = it) }
                },
                doOnMetadataKeyDeleted = {
                    updateViewState { copy(metadataKeyDeletedDialog = it) }
                },
                doOnMetadataKeyVerificationFailure = {
                    emitSideEffect(
                        ShowSnackbar(SnackbarMessage.METADATA_KEY_VERIFICATION_FAILURE),
                    )
                },
            )
            updateViewState { copy(shouldShowDialogProgress = false) }
            updateResourceIdlingResource.setIdle(true)
        }
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE ->
                emitSideEffect(ShowSnackbar(SnackbarMessage.JSON_SCHEMA_RESOURCE_VALIDATION_ERROR))
            SchemaEntity.SECRET ->
                emitSideEffect(ShowSnackbar(SnackbarMessage.JSON_SCHEMA_SECRET_VALIDATION_ERROR))
        }
    }

    @Suppress("ReturnCount")
    private fun onValid(action: () -> Unit) {
        val resourceSecret = resourceModelHandler.resourceSecret
        when (uiModel.leadingContentType) {
            TOTP -> {
                val totpKey = resourceSecret.totp?.key
                if (totpKey.isNullOrBlank()) {
                    updateViewState { copy(totpData = totpData.copy(totpSecretError = MustNotBeEmpty)) }
                    return
                }
                if (!StringIsBase32.condition(totpKey)) {
                    updateViewState { copy(totpData = totpData.copy(totpSecretError = MustBeBase32)) }
                    return
                }
                action()
            }
            STANDALONE_NOTE -> {
                if (!StringMaxLength(NoteFormViewModel.NOTE_MAX_LENGTH)
                        .condition(resourceSecret.description.orEmpty())
                ) {
                    updateViewState {
                        copy(noteData = noteData.copy(noteError = MaxLengthExceeded(NoteFormViewModel.NOTE_MAX_LENGTH)))
                    }
                    return
                }
                action()
            }
            PIN_CODE -> {
                val pinCode = resourceSecret.pinCode.orEmpty()
                val error =
                    when {
                        pinCode.length < PinCodeUiModel.MIN_LENGTH ->
                            PinCodeValidationError.TooShort(PinCodeUiModel.MIN_LENGTH)
                        pinCode.length > PinCodeUiModel.MAX_LENGTH ->
                            PinCodeValidationError.TooLong(PinCodeUiModel.MAX_LENGTH)
                        else -> null
                    }
                if (error != null) {
                    Timber.w("PIN code validation failed: ${error::class.simpleName} (actualLength=${pinCode.length})")
                    updateViewState { copy(pinCodeData = pinCodeData.copy(pinCodeError = error)) }
                    return
                }
                action()
            }
            else -> action()
        }
    }

    private fun trustedMetadataKeyDeleted() {
        updateViewState { copy(metadataKeyDeletedDialog = null) }
        launch {
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
        }
    }

    private fun trustNewMetadataKey(model: NewMetadataKeyToTrustModel) {
        updateViewState { copy(metadataKeyModifiedDialog = null) }
        launch {
            updateViewState { copy(shouldShowDialogProgress = true) }
            when (
                val output =
                    runAuthenticatedOperation {
                        metadataPrivateKeysHelperInteractor.trustNewKey(model)
                    }
            ) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    emitSideEffect(ShowSnackbar(SnackbarMessage.METADATA_KEY_IS_TRUSTED))
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    emitSideEffect(ShowSnackbar(SnackbarMessage.METADATA_KEY_TRUST_FAILED))
                }
            }
            updateViewState { copy(shouldShowDialogProgress = false) }
        }
    }

    private suspend fun computeShowUpgradePanel(): Boolean {
        if (mode !is Edit) {
            return false
        }

        val resource =
            getLocalResourceUseCase
                .execute(GetLocalResourceUseCase.Input(mode.resourceId))
                .resource
        val featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
        val metadataTypesSettings = getMetadataTypesSettingsUseCase.execute(Unit).metadataTypesSettingsModel

        return featureFlags.isV5MetadataAvailable &&
            metadataTypesSettings.allowV4V5Upgrade &&
            metadataTypesSettings.allowCreationOfV5Resources &&
            !resource.contentType().isV5()
    }

    private fun upgradeResource() {
        if (mode !is Edit) return
        launch {
            updateViewState { copy(shouldShowDialogProgress = true) }
            val resourceUpdateActionsInteractor = upgradedResourceUpdateActionsInteractor()
            if (resourceUpdateActionsInteractor.doesUpgradeToV5ReEncryptSecret() &&
                editPermissionsConfirmationInteractor.shouldConfirmPermissions(mode.resourceId)
            ) {
                Timber.d("Upgrading a shared resource - navigating to permissions confirmation")
                isUpgradePendingPermissionsConfirmation = true
                emitSideEffect(NavigateToConfirmPermissions(ConfirmPermissionsMode.Edit(mode.resourceId)))
            } else {
                performUpgrade(resourceUpdateActionsInteractor, confirmedPermissions = null)
            }
            updateViewState { copy(shouldShowDialogProgress = false) }
        }
    }

    private fun performUpgradeWithConfirmedPermissions(confirmedPermissions: List<PermissionModelUi>) {
        if (mode !is Edit) return
        launch {
            updateViewState { copy(shouldShowDialogProgress = true) }
            performUpgrade(upgradedResourceUpdateActionsInteractor(), confirmedPermissions)
            updateViewState { copy(shouldShowDialogProgress = false) }
        }
    }

    private suspend fun upgradedResourceUpdateActionsInteractor(): ResourceUpdateActionsInteractor {
        val resource =
            getLocalResourceUseCase
                .execute(GetLocalResourceUseCase.Input((mode as Edit).resourceId))
                .resource
        return resourceUpdateActionsInteractorFactory.create(resource)
    }

    private suspend fun performUpgrade(
        resourceUpdateActionsInteractor: ResourceUpdateActionsInteractor,
        confirmedPermissions: List<PermissionModelUi>?,
    ) {
        val onUpgradeFailure: () -> Unit = { emitSideEffect(ShowSnackbar(SnackbarMessage.UPGRADE_FAILURE)) }
        performResourceUpdateAction(
            action = {
                if (confirmedPermissions == null) {
                    resourceUpdateActionsInteractor.upgradeToV5()
                } else {
                    resourceUpdateActionsInteractor.upgradeToV5WithConfirmedPermissions(confirmedPermissions)
                }
            },
            doOnSuccess = {
                launch {
                    resourceModelHandler.initializeModelForEdition((mode as Edit).resourceId)
                    setupState()
                    emitSideEffect(ShowSnackbar(SnackbarMessage.RESOURCE_UPGRADED))
                }
            },
            doOnFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.COMMON_FAILURE)) },
            doOnCryptoFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.ENCRYPTION_FAILURE)) },
            doOnFetchFailure = onUpgradeFailure,
            doOnUnauthorized = onUpgradeFailure,
            doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
            doOnPermissionsDrifted = { drifted ->
                Timber.d("Permissions drifted before the upgrade - reopening the confirmation")
                isUpgradePendingPermissionsConfirmation = true
                emitSideEffect(
                    NavigateToConfirmPermissions(
                        ConfirmPermissionsMode.Edit((mode as Edit).resourceId),
                        driftedEntityNames = drifted.driftedEntityNames,
                    ),
                )
            },
            doOnCannotEditWithCurrentConfig = {
                emitSideEffect(
                    ShowSnackbar(SnackbarMessage.CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG),
                )
            },
            doOnMetadataKeyModified = {
                updateViewState { copy(metadataKeyModifiedDialog = it) }
            },
            doOnMetadataKeyDeleted = {
                updateViewState { copy(metadataKeyDeletedDialog = it) }
            },
            doOnMetadataKeyVerificationFailure = {
                emitSideEffect(
                    ShowSnackbar(SnackbarMessage.METADATA_KEY_VERIFICATION_FAILURE),
                )
            },
        )
    }

    companion object {
        private const val LEARN_MORE_UPGRADE_URL =
            "https://www.passbolt.com/blog/the-road-to-passbolt-v5-encrypted-metadata-and-other-core-security-changes-2"
    }
}
