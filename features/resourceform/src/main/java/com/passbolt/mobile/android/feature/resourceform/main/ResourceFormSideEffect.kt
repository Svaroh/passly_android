package net.svaroh.passly.feature.resourceform.main

import net.svaroh.passly.ui.AdditionalUrisUiModel
import net.svaroh.passly.ui.ConfirmPermissionsMode
import net.svaroh.passly.ui.CustomFieldsUiModel
import net.svaroh.passly.ui.PassphraseGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel
import net.svaroh.passly.ui.PasswordUiModel
import net.svaroh.passly.ui.PinCodeUiModel
import net.svaroh.passly.ui.ResourceAppearanceModel
import net.svaroh.passly.ui.ResourceFormMode
import net.svaroh.passly.ui.TotpUiModel

sealed interface ResourceFormSideEffect {
    data class NavigateToPassword(
        val mode: ResourceFormMode,
        val passwordUiModel: PasswordUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToTotp(
        val mode: ResourceFormMode,
        val totpUiModel: TotpUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToTotpAdvancedSettings(
        val mode: ResourceFormMode,
        val totpUiModel: TotpUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToAdvancedSecretGeneration(
        val selectedTab: PasswordGeneratorTypeUiModel,
        val passwordSettings: PasswordGeneratorSettingsUiModel,
        val passphraseSettings: PassphraseGeneratorSettingsUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToNote(
        val mode: ResourceFormMode,
        val note: String,
    ) : ResourceFormSideEffect

    data class NavigateToPinCode(
        val mode: ResourceFormMode,
        val pinCodeUiModel: PinCodeUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToPinCodeAdvancedGeneration(
        val mode: ResourceFormMode,
        val pinCodeUiModel: PinCodeUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToDescription(
        val mode: ResourceFormMode,
        val metadataDescription: String,
    ) : ResourceFormSideEffect

    data class NavigateToAdditionalUris(
        val mode: ResourceFormMode,
        val model: AdditionalUrisUiModel,
    ) : ResourceFormSideEffect

    data class NavigateToAppearance(
        val mode: ResourceFormMode,
        val appearanceModel: ResourceAppearanceModel,
    ) : ResourceFormSideEffect

    data class NavigateToCustomFields(
        val mode: ResourceFormMode,
        val model: CustomFieldsUiModel,
    ) : ResourceFormSideEffect

    data object NavigateToScanOtp : ResourceFormSideEffect

    data class NavigateToConfirmPermissions(
        val confirmMode: ConfirmPermissionsMode,
        val driftedEntityNames: List<String>? = null,
    ) : ResourceFormSideEffect

    data class NavigateBackWithCreateSuccess(
        val name: String,
        val resourceId: String,
    ) : ResourceFormSideEffect

    data class NavigateBackWithEditSuccess(
        val name: String,
    ) : ResourceFormSideEffect

    data object NavigateBack : ResourceFormSideEffect

    data class ShowSnackbar(
        val type: SnackbarMessage,
    ) : ResourceFormSideEffect

    data class ShowToast(
        val type: ToastMessage,
        val args: List<Any> = emptyList(),
    ) : ResourceFormSideEffect

    data class OpenWebsite(
        val url: String,
    ) : ResourceFormSideEffect
}

enum class SnackbarMessage {
    COMMON_FAILURE,
    CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG,
    METADATA_KEY_VERIFICATION_FAILURE,
    JSON_SCHEMA_RESOURCE_VALIDATION_ERROR,
    JSON_SCHEMA_SECRET_VALIDATION_ERROR,
    METADATA_KEY_TRUST_FAILED,
    ENCRYPTION_FAILURE,
    METADATA_KEY_IS_TRUSTED,
    RESOURCE_UPGRADED,
    UPGRADE_FAILURE,
    PASSWORD_POLICIES_FETCH_FAILED,
    PASSWORD_EXPIRY_FETCH_FAILED,
    RESOURCE_EDITED_SHARE_FAILED,
}

enum class ToastMessage {
    UNABLE_TO_GENERATE_PASSWORD,
    CREATE_INITIALIZATION_ERROR,
    EDIT_INITIALIZATION_ERROR,
    RESOURCE_CREATED_SHARE_FAILED,
    RESOURCE_CREATED_PERMISSIONS_CHANGED,
}
