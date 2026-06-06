package net.svaroh.passly.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import net.svaroh.passly.ui.AdditionalUrisUiModel
import net.svaroh.passly.ui.CustomFieldsUiModel
import net.svaroh.passly.ui.PasswordUiModel
import net.svaroh.passly.ui.ResourceAppearanceModel
import net.svaroh.passly.ui.ResourceFormMode
import net.svaroh.passly.ui.TotpUiModel
import kotlinx.serialization.Serializable

sealed interface ResourceFormNavigationKey : NavKey {
    @Serializable
    data class MainResourceForm(
        val mode: ResourceFormMode,
    ) : ResourceFormNavigationKey

    @Serializable
    data class PasswordForm(
        val mode: ResourceFormMode,
        val passwordModel: PasswordUiModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class TotpForm(
        val mode: ResourceFormMode,
        val totpUiModel: TotpUiModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class TotpAdvancedSettingsForm(
        val mode: ResourceFormMode,
        val totpUiModel: TotpUiModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class NoteForm(
        val mode: ResourceFormMode,
        val note: String,
    ) : ResourceFormNavigationKey

    @Serializable
    data class DescriptionForm(
        val mode: ResourceFormMode,
        val metadataDescription: String,
    ) : ResourceFormNavigationKey

    @Serializable
    data class AdditionalUrisForm(
        val mode: ResourceFormMode,
        val additionalUris: AdditionalUrisUiModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class AppearanceForm(
        val mode: ResourceFormMode,
        val appearanceModel: ResourceAppearanceModel,
    ) : ResourceFormNavigationKey

    @Serializable
    data class CustomFieldsForm(
        val mode: ResourceFormMode,
        val customFieldsUiModel: CustomFieldsUiModel,
    ) : ResourceFormNavigationKey
}
