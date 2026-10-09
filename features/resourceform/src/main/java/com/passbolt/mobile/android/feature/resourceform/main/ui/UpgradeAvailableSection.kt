/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package net.svaroh.passly.feature.resourceform.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.ui.button.PrimaryButton
import net.svaroh.passly.core.ui.section.Section
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.LearnMoreAboutUpgrade
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.UpgradeResource
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
internal fun UpgradeAvailableSection(
    onIntent: (ResourceFormIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(
        title = stringResource(LocalizationR.string.resource_details_upgrade_available_header),
        backgroundColor = colorResource(CoreUiR.color.upgrade_panel_background),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(LocalizationR.string.resource_details_upgrade_panel_message),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = stringResource(LocalizationR.string.resource_details_upgrade_action),
                onClick = { onIntent(UpgradeResource) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(CoreUiR.color.upgrade_panel_button_background),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
            )
            Text(
                text = stringResource(LocalizationR.string.resource_details_upgrade_learn_more),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
                modifier =
                    Modifier
                        .clickable { onIntent(LearnMoreAboutUpgrade) }
                        .padding(8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpgradeAvailableSectionPreview() {
    PassboltTheme {
        UpgradeAvailableSection(onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun UpgradeAvailableSectionDarkPreview() {
    PassboltTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            UpgradeAvailableSection(onIntent = {})
        }
    }
}
