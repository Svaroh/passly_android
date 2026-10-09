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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidingFeedbackPullToRefreshBox(
    isRefreshing: Boolean,
    refreshProgress: Float,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberSlidingFeedbackPullToRefreshState()
    val haptic = LocalHapticFeedback.current
    val latestIsRefreshing = rememberUpdatedState(isRefreshing)

    LaunchedEffect(state) {
        state.followPull(latestIsRefreshing)
    }

    LaunchedEffect(state) {
        state.detectThresholdCrossings(latestIsRefreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = {
            if (!isRefreshing) {
                state.onReleasedPastThreshold()
                onRefresh()
            }
        },
        state = state.pullToRefreshState,
        modifier = modifier,
        indicator = {
            if (state.isRippleVisible) {
                ThresholdRipple(
                    progress = state.rippleProgress,
                    pullRangePx = state.pullRangePx,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
            if (!isRefreshing && state.isArrowVisible) {
                PullArrow(
                    pullFraction = state.pullFraction,
                    pullRangePx = state.pullRangePx,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        },
    ) {
        RefreshableContent(
            contentOffset = { state.contentOffset },
            isRefreshing = isRefreshing,
            refreshProgress = refreshProgress,
            content = content,
        )
    }
}
