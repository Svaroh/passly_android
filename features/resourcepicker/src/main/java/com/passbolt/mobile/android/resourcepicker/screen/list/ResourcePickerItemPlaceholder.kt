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

package net.svaroh.passly.resourcepicker.screen.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val PRIMARY_LINE_WIDTH_FRACTION = 0.5f
private const val SECONDARY_LINE_WIDTH_FRACTION = 0.3f

internal val RESOURCE_PICKER_ITEM_PLACEHOLDER_HEIGHT = 64.dp

@Composable
fun ResourcePickerItemPlaceholder(modifier: Modifier = Modifier) {
    val skeletonColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(RESOURCE_PICKER_ITEM_PLACEHOLDER_HEIGHT)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(46.dp, 52.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(skeletonColor),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(PRIMARY_LINE_WIDTH_FRACTION)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(skeletonColor),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(SECONDARY_LINE_WIDTH_FRACTION)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(skeletonColor),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResourcePickerItemPlaceholderPreview() {
    ResourcePickerItemPlaceholder()
}
