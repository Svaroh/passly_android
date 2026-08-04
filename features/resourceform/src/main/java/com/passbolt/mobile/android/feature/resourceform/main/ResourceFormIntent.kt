package com.passbolt.mobile.android.feature.resourceform.main

import com.passbolt.mobile.android.feature.resourceform.navigation.AdvancedSecretGenerationFormResult
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PinCodeUiModel
import com.passbolt.mobile.android.ui.ResourceAppearanceModel
import com.passbolt.mobile.android.ui.TotpUiModel

sealed interface ResourceFormIntent {
    data class NameTextChanged(
        val name: String,
    ) : ResourceFormIntent

    data object ExpandAdvancedSettings : ResourceFormIntent

    data object CreateResource : ResourceFormIntent

    data class ConfirmedPermissionsResult(
        val permissions: List<PermissionModelUi>,
    ) : ResourceFormIntent

    data object UpdateResource : ResourceFormIntent

    data class PasswordTextChanged(
        val password: String,
    ) : ResourceFormIntent

    data object GeneratePassword : ResourceFormIntent

    data object DismissUnableToGeneratePassword : ResourceFormIntent

    data object OpenAdvancedSecretGeneration : ResourceFormIntent

    data class AdvancedSecretGenerationResult(
        val result: AdvancedSecretGenerationFormResult,
    ) : ResourceFormIntent

    data class PasswordMainUriTextChanged(
        val mainUri: String,
    ) : ResourceFormIntent

    data class PasswordUsernameTextChanged(
        val username: String,
    ) : ResourceFormIntent

    data class TotpSecretChanged(
        val totpSecret: String,
    ) : ResourceFormIntent

    data class TotpUrlChanged(
        val url: String,
    ) : ResourceFormIntent

    data object GoToTotpMoreSettings : ResourceFormIntent

    data object ScanTotp : ResourceFormIntent

    data class NoteChanged(
        val note: String,
    ) : ResourceFormIntent

    data class PinCodeChanged(
        val pinCode: String,
    ) : ResourceFormIntent

    data object GeneratePinCode : ResourceFormIntent

    data object GoToPinCodeAdvancedGeneration : ResourceFormIntent

    data class PinCodeAdvancedGenerationResult(
        val pinCodeUiModel: PinCodeUiModel,
    ) : ResourceFormIntent

    data object GoToAdditionalNote : ResourceFormIntent

    data object GoToAdditionalTotp : ResourceFormIntent

    data object GoToAdditionalPassword : ResourceFormIntent

    data object GoToAdditionalPinCode : ResourceFormIntent

    data object GoToCustomFields : ResourceFormIntent

    data object GoToMetadataDescription : ResourceFormIntent

    data object GoToAppearance : ResourceFormIntent

    data object GoToAdditionalUris : ResourceFormIntent

    data class PasswordResult(
        val passwordUiModel: PasswordUiModel?,
    ) : ResourceFormIntent

    data class TotpResult(
        val totpUiModel: TotpUiModel?,
    ) : ResourceFormIntent

    data class TotpAdvancedSettingsResult(
        val totpAdvancedSettings: TotpUiModel?,
    ) : ResourceFormIntent

    data class NoteResult(
        val note: String?,
    ) : ResourceFormIntent

    data class PinCodeResult(
        val pinCodeUiModel: PinCodeUiModel?,
    ) : ResourceFormIntent

    data class DescriptionResult(
        val metadataDescription: String?,
    ) : ResourceFormIntent

    data class AppearanceResult(
        val model: ResourceAppearanceModel?,
    ) : ResourceFormIntent

    data class AdditionalUrisResult(
        val urisUiModel: AdditionalUrisUiModel?,
    ) : ResourceFormIntent

    data object CustomFieldsResult : ResourceFormIntent

    data class ScanOtpResult(
        val isManualCreationChosen: Boolean,
        val scannedTotp: OtpParseResult.OtpQr.TotpQr?,
    ) : ResourceFormIntent

    data class TrustNewMetadataKey(
        val model: NewMetadataKeyToTrustModel,
    ) : ResourceFormIntent

    data object TrustedMetadataKeyDeleted : ResourceFormIntent

    data object DismissMetadataKeyDialog : ResourceFormIntent

    data object GoBack : ResourceFormIntent

    data object ProceedWithPasswordWarning : ResourceFormIntent

    data object DismissPasswordWarning : ResourceFormIntent

    data object UpgradeResource : ResourceFormIntent

    data object LearnMoreAboutUpgrade : ResourceFormIntent
}
