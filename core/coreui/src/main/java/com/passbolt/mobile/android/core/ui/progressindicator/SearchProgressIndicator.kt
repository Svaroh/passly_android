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
package net.svaroh.passly.core.ui.progressindicator

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.ui.R
import net.svaroh.passly.core.localization.R as LocalizationR

@Composable
fun SearchProgressIndicator(modifier: Modifier = Modifier) {
    val searchingDescription = stringResource(LocalizationR.string.search_in_progress)

    LinearProgressIndicator(
        color = colorResource(R.color.primary),
        trackColor = Color.Transparent,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        modifier =
            modifier
                .fillMaxWidth()
                .height(INDICATOR_HEIGHT)
                .semantics { contentDescription = searchingDescription },
    )
}

private val INDICATOR_HEIGHT = 4.dp

@Preview(showBackground = true)
@Composable
private fun SearchProgressIndicatorPreview() {
    PassboltTheme {
        SearchProgressIndicator()
    }
}
