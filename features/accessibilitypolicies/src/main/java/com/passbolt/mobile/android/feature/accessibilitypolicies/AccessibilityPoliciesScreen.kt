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

package com.passbolt.mobile.android.feature.accessibilitypolicies

import PassboltTheme
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.Home
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.EncourageAccessibilityAutofill
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.feature.accessibilitypolicies.AccessibilityPoliciesIntent.Accept
import com.passbolt.mobile.android.feature.accessibilitypolicies.AccessibilityPoliciesIntent.Decline
import com.passbolt.mobile.android.feature.accessibilitypolicies.AccessibilityPoliciesSideEffect.NavigateToAcceptedScreen
import com.passbolt.mobile.android.feature.accessibilitypolicies.AccessibilityPoliciesSideEffect.NavigateToDeclinedScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun AccessibilityPoliciesScreen(
    flow: AccessibilityPoliciesFlow,
    viewModel: AccessibilityPoliciesViewModel = koinViewModel(),
    navigator: AppNavigator = koinInject(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    AccessibilityPoliciesScreen(
        onAccept = { viewModel.onIntent(Accept) },
        onDecline = { viewModel.onIntent(Decline) },
    )

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (flow) {
            AccessibilityPoliciesFlow.SETUP -> {
                navigator.startNavigationActivity(context, Home)
                activity?.finish()
            }
            AccessibilityPoliciesFlow.SETTINGS ->
                when (sideEffect) {
                    NavigateToAcceptedScreen -> {
                        navigator.navigateBack()
                        navigator.navigateToKey(EncourageAccessibilityAutofill)
                    }
                    NavigateToDeclinedScreen -> navigator.navigateBack()
                }
        }
    }
}

@Composable
private fun AccessibilityPoliciesScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.windowInsetsPadding(BottomAppBarDefaults.windowInsets),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(LocalizationR.string.accessibility_policies_accept),
                        onClick = onAccept,
                    )
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDecline,
                    ) {
                        Text(
                            text = stringResource(LocalizationR.string.accessibility_policies_decline),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
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
private fun AccessibilityPoliciesScreenPreview() {
    PassboltTheme {
        AccessibilityPoliciesScreen(onAccept = {}, onDecline = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AccessibilityPoliciesScreenDarkPreview() {
    PassboltTheme(darkTheme = true) {
        AccessibilityPoliciesScreen(onAccept = {}, onDecline = {})
    }
}
