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

package net.svaroh.passly.core.ui.pulltorefresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.ui.R

@Composable
internal fun PullArrow(
    pullFraction: Float,
    pullRangePx: Float,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_refresh_reload),
        contentDescription = null,
        tint = colorResource(R.color.primary),
        modifier =
            modifier
                .padding(top = INDICATOR_TOP_PADDING)
                .size(INDICATOR_SIZE)
                .graphicsLayer {
                    alpha = pullFraction.coerceIn(0f, 1f)
                    rotationZ = pullFraction * FULL_ROTATION_DEGREES
                    translationY = ((pullFraction * pullRangePx - size.height) / 2f).coerceAtLeast(0f)
                },
    )
}

@Composable
internal fun ThresholdRipple(
    progress: Float,
    pullRangePx: Float,
    modifier: Modifier = Modifier,
) {
    val rippleColor = colorResource(R.color.primary)
    Box(
        modifier =
            modifier
                .padding(top = INDICATOR_TOP_PADDING)
                .size(INDICATOR_SIZE)
                .graphicsLayer { translationY = ((pullRangePx - size.height) / 2f).coerceAtLeast(0f) }
                .drawBehind {
                    drawCircle(
                        color = rippleColor,
                        radius = lerp(RIPPLE_START_RADIUS.toPx(), RIPPLE_END_RADIUS.toPx(), progress),
                        alpha = (1f - progress) * RIPPLE_MAX_ALPHA,
                        center = center,
                    )
                },
    )
}

private val INDICATOR_SIZE = 28.dp
private val INDICATOR_TOP_PADDING = 8.dp
private const val FULL_ROTATION_DEGREES = 360f
private val RIPPLE_START_RADIUS = 14.dp
private val RIPPLE_END_RADIUS = 44.dp
private const val RIPPLE_MAX_ALPHA = 0.3f

@Preview(showBackground = true)
@Composable
private fun PullArrowPreview() {
    PassboltTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp),
        ) {
            PullArrow(
                pullFraction = 1f,
                pullRangePx = 200f,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThresholdRipplePreview() {
    PassboltTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp),
        ) {
            ThresholdRipple(
                progress = 0.4f,
                pullRangePx = 200f,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
