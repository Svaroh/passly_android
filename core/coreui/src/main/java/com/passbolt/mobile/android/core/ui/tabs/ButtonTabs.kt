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

package net.svaroh.passly.core.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.ui.R

@Composable
fun ButtonTabs(
    items: List<ButtonTabItemModel>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = spacedBy(8.dp),
    ) {
        items.forEach { item ->
            ButtonTabItem(
                item = item,
                onClick = { onSelect(item.id) },
            )
        }
    }
}

@Composable
private fun ButtonTabItem(
    item: ButtonTabItemModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonModifier = modifier.defaultMinSize(minHeight = 40.dp)
    if (item.isSelected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(4.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
            modifier = buttonModifier,
        ) {
            Text(text = item.label, style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(4.dp),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = colorResource(R.color.secondary_button_background),
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
            border =
                BorderStroke(
                    width = 1.dp,
                    color = colorResource(R.color.secondary_button_border),
                ),
            modifier = buttonModifier,
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ButtonTabsPreview() {
    PassboltTheme {
        ButtonTabs(
            items =
                listOf(
                    ButtonTabItemModel("password", "Password", isSelected = true),
                    ButtonTabItemModel("passphrase", "Passphrase", isSelected = false),
                ),
            onSelect = {},
        )
    }
}
