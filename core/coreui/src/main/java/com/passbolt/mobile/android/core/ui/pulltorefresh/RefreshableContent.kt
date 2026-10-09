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

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import net.svaroh.passly.core.ui.progressindicator.DataRefreshProgressIndicator

@Composable
internal fun RefreshableContent(
    contentOffset: () -> Float,
    isRefreshing: Boolean,
    refreshProgress: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val initialRamp by animateFloatAsState(
        targetValue = if (isRefreshing) INITIAL_FAKE_PROGRESS_CEILING else 0f,
        animationSpec = tween(INITIAL_FAKE_RAMP_DURATION_MILLIS, easing = LinearOutSlowInEasing),
        label = "refreshProgressInitialRamp",
    )
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = contentOffset() },
            content = content,
        )
        if (isRefreshing) {
            DataRefreshProgressIndicator(
                progress = maxOf(refreshProgress, initialRamp),
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

private const val INITIAL_FAKE_PROGRESS_CEILING = 0.1f
private const val INITIAL_FAKE_RAMP_DURATION_MILLIS = 900
