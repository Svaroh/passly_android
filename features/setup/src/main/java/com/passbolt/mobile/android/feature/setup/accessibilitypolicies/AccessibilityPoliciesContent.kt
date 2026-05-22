package com.passbolt.mobile.android.feature.setup.accessibilitypolicies

import PassboltTheme
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun AccessibilityPoliciesContent(
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                PrimaryButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(LocalizationR.string.consent),
                    onClick = onAcknowledge,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(CoreUiR.drawable.logo_text_icon),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(top = 32.dp)
                        .size(116.dp, 48.dp),
            )

            Text(
                text = stringResource(LocalizationR.string.dialog_accessibility_consent_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp),
            )

            Text(
                text = stringResource(LocalizationR.string.dialog_accessibility_consent_intro),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            DisclosureSection(
                iconRes = CoreUiR.drawable.ic_eye_visible,
                title = stringResource(LocalizationR.string.dialog_accessibility_consent_section_what_title),
            ) {
                Text(
                    text = stringResource(LocalizationR.string.dialog_accessibility_consent_section_what_lead),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletRow(text = stringResource(LocalizationR.string.dialog_accessibility_consent_section_what_bullet_1))
                BulletRow(text = stringResource(LocalizationR.string.dialog_accessibility_consent_section_what_bullet_2))
                BulletRow(text = stringResource(LocalizationR.string.dialog_accessibility_consent_section_what_bullet_3))
            }

            Spacer(modifier = Modifier.height(12.dp))

            DisclosureSection(
                iconRes = CoreUiR.drawable.ic_lock,
                title = stringResource(LocalizationR.string.dialog_accessibility_consent_section_how_title),
            ) {
                BulletRow(text = stringResource(LocalizationR.string.dialog_accessibility_consent_section_how_bullet_1))
                BulletRow(text = stringResource(LocalizationR.string.dialog_accessibility_consent_section_how_bullet_2))
                BulletRow(text = stringResource(LocalizationR.string.dialog_accessibility_consent_section_how_bullet_3))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DisclosureSection(
    @DrawableRes iconRes: Int,
    title: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colorResource(CoreUiR.color.section_background))
                .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BulletRow(text: String) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccessibilityPoliciesContentPreview() {
    PassboltTheme {
        AccessibilityPoliciesContent(onAcknowledge = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AccessibilityPoliciesContentDarkPreview() {
    PassboltTheme(darkTheme = true) {
        AccessibilityPoliciesContent(onAcknowledge = {})
    }
}
