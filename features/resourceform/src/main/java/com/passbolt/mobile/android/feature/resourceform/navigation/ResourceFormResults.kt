package net.svaroh.passly.feature.resourceform.navigation

import net.svaroh.passly.ui.AdditionalUrisUiModel
import net.svaroh.passly.ui.CustomFieldsUiModel
import net.svaroh.passly.ui.PasswordUiModel
import net.svaroh.passly.ui.ResourceAppearanceModel
import net.svaroh.passly.ui.TotpUiModel

data class PasswordFormResult(
    val model: PasswordUiModel,
)

data class TotpFormResult(
    val totpUiModel: TotpUiModel?,
)

data class TotpAdvancedSettingsFormResult(
    val totpModel: TotpUiModel,
)

data class NoteFormResult(
    val note: String?,
)

data class DescriptionFormResult(
    val metadataDescription: String,
)

data class AdditionalUrisFormResult(
    val model: AdditionalUrisUiModel,
)

data class AppearanceFormResult(
    val model: ResourceAppearanceModel,
)

data class CustomFieldsFormResult(
    val model: CustomFieldsUiModel,
)
