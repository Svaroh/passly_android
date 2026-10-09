package net.svaroh.passly.feature.resourceform.main

import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteValidationError
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.PinCodeValidationError
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError
import net.svaroh.passly.ui.LeadingContentType
import net.svaroh.passly.ui.NewMetadataKeyToTrustModel
import net.svaroh.passly.ui.PassphraseGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PasswordGeneratorTypeUiModel
import net.svaroh.passly.ui.PasswordStrength
import net.svaroh.passly.ui.PinCodeUiModel
import net.svaroh.passly.ui.ResourceFormMode
import net.svaroh.passly.ui.ResourceFormUiModel
import net.svaroh.passly.ui.TotpUiModel
import net.svaroh.passly.ui.TrustedKeyDeletedModel

data class ResourceFormState(
    val mode: ResourceFormMode? = null,
    val name: String = "",
    val shouldShowScreenProgress: Boolean = true,
    val shouldShowDialogProgress: Boolean = false,
    val leadingContentType: LeadingContentType? = null,
    val isPrimaryButtonVisible: Boolean = false,
    val areAdvancedSettingsExpanded: Boolean = false,
    val areAdvancedSettingsVisible: Boolean = true,
    val supportedAdditionalSecrets: List<ResourceFormUiModel.Secret> = emptyList(),
    val supportedMetadata: List<ResourceFormUiModel.Metadata> = emptyList(),
    val passwordData: PasswordData = PasswordData(),
    val totpData: TotpData = TotpData(),
    val noteData: NoteData = NoteData(),
    val pinCodeData: PinCodeData = PinCodeData(),
    val showUpgradePanel: Boolean = false,
    val metadataKeyModifiedDialog: NewMetadataKeyToTrustModel? = null,
    val metadataKeyDeletedDialog: TrustedKeyDeletedModel? = null,
    val showPasswordWarningDialog: Boolean = false,
    val passwordWarningType: PasswordWarningType? = null,
    val generatorType: PasswordGeneratorTypeUiModel? = null,
    val passwordGeneratorSettings: PasswordGeneratorSettingsUiModel? = null,
    val passphraseGeneratorSettings: PassphraseGeneratorSettingsUiModel? = null,
    val isUnableToGeneratePasswordDialogVisible: Boolean = false,
    val minimumEntropyBits: Int = 0,
)

data class PasswordData(
    val password: String = "",
    val passwordStrength: PasswordStrength = PasswordStrength.Empty,
    val passwordEntropyBits: Double = 0.0,
    val mainUri: String = "",
    val username: String = "",
)

data class TotpData(
    val totpSecret: String = "",
    val totpIssuer: String = "",
    val totpUiModel: TotpUiModel? = null,
    val totpSecretError: TotpSecretValidationError? = null,
)

data class NoteData(
    val note: String = "",
    val noteError: NoteValidationError? = null,
)

data class PinCodeData(
    val pinCode: String = "",
    val length: Int = PinCodeUiModel.DEFAULT_LENGTH,
    val pinCodeError: PinCodeValidationError? = null,
)

enum class PasswordWarningType {
    DATA_BREACH,
    LOW_ENTROPY,
}
