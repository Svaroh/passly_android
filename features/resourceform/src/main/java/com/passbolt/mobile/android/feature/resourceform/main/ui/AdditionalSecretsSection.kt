package net.svaroh.passly.feature.resourceform.main.ui

import PassboltTheme
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.svaroh.passly.core.ui.section.Section
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToAdditionalNote
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToAdditionalPassword
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToAdditionalTotp
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.GoToCustomFields
import net.svaroh.passly.ui.ResourceFormUiModel
import net.svaroh.passly.ui.ResourceFormUiModel.Secret.CUSTOM_FIELDS
import net.svaroh.passly.ui.ResourceFormUiModel.Secret.NOTE
import net.svaroh.passly.ui.ResourceFormUiModel.Secret.PASSWORD
import net.svaroh.passly.ui.ResourceFormUiModel.Secret.TOTP
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
internal fun AdditionalSecretsSection(
    secrets: List<ResourceFormUiModel.Secret>,
    onIntent: (ResourceFormIntent) -> Unit,
) {
    val context = LocalContext.current

    Section(title = stringResource(LocalizationR.string.resource_form_view_additional_secrets)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            secrets.forEach { secret ->
                with(
                    secretToSettingRowItem(
                        context = context,
                        secret = secret,
                        onIntent = onIntent,
                    ),
                ) {
                    SettingRow(
                        leadingIconResId = iconResId,
                        text = text,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}

private fun secretToSettingRowItem(
    context: Context,
    secret: ResourceFormUiModel.Secret,
    onIntent: (ResourceFormIntent) -> Unit,
): SettingRowItem =
    when (secret) {
        PASSWORD ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_password),
                iconResId = CoreUiR.drawable.ic_key,
                onClick = { onIntent(GoToAdditionalPassword) },
            )
        NOTE ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_note),
                iconResId = CoreUiR.drawable.ic_notes,
                onClick = { onIntent(GoToAdditionalNote) },
            )
        TOTP ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_totp),
                iconResId = CoreUiR.drawable.ic_time_lock,
                onClick = { onIntent(GoToAdditionalTotp) },
            )
        CUSTOM_FIELDS ->
            SettingRowItem(
                text = context.getString(LocalizationR.string.resource_form_custom_fields),
                iconResId = CoreUiR.drawable.ic_custom_fields,
                onClick = { onIntent(GoToCustomFields) },
            )
    }

@Preview(showBackground = true)
@Composable
private fun AdditionalSecretsSectionPreview() {
    PassboltTheme {
        AdditionalSecretsSection(
            secrets =
                listOf(
                    PASSWORD,
                    NOTE,
                    TOTP,
                    CUSTOM_FIELDS,
                ),
            onIntent = {},
        )
    }
}
