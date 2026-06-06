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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSlidingFeedbackPullToRefreshState(): SlidingFeedbackPullToRefreshState {
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val pullRangePx = with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
    return remember(pullToRefreshState, coroutineScope, pullRangePx) {
        SlidingFeedbackPullToRefreshState(
            pullToRefreshState = pullToRefreshState,
            coroutineScope = coroutineScope,
            pullRangePx = pullRangePx,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Stable
internal class SlidingFeedbackPullToRefreshState(
    val pullToRefreshState: PullToRefreshState,
    private val coroutineScope: CoroutineScope,
    val pullRangePx: Float,
) {
    private val contentOffsetAnimatable = Animatable(0f)
    private val rippleAnimatable = Animatable(RIPPLE_SETTLED)

    var isRetracting by mutableStateOf(false)
        private set

    val contentOffset: Float get() = contentOffsetAnimatable.value
    val rippleProgress: Float get() = rippleAnimatable.value
    val pullFraction: Float get() = pullToRefreshState.distanceFraction
    val isArrowVisible: Boolean get() = pullFraction > 0f && !isRetracting
    val isRippleVisible: Boolean get() = rippleProgress < RIPPLE_SETTLED

    suspend fun followPull(isRefreshing: State<Boolean>) {
        snapshotFlow { pullFraction }.collect { fraction ->
            if (!isRetracting && !isRefreshing.value) {
                contentOffsetAnimatable.snapTo(fraction.coerceIn(0f, MAX_PULL_FRACTION) * pullRangePx)
            }
        }
    }

    suspend fun detectThresholdCrossings(
        isRefreshing: State<Boolean>,
        onCrossed: () -> Unit,
    ) {
        snapshotFlow { pullFraction }
            .scan(initial = false) { isPastThreshold, fraction ->
                when {
                    !isRefreshing.value && fraction >= PULL_THRESHOLD -> true
                    fraction < THRESHOLD_REARM -> false
                    else -> isPastThreshold
                }
            }.distinctUntilChanged()
            .filter { isPastThreshold -> isPastThreshold }
            .collect {
                coroutineScope.launch { pulseRipple() }
                onCrossed()
            }
    }

    fun onReleasedPastThreshold() {
        if (isRetracting) return
        isRetracting = true
        coroutineScope.launch {
            contentOffsetAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = RETRACT_DURATION_MILLIS, easing = LinearOutSlowInEasing),
            )
            snapshotFlow { pullFraction }.first { it <= SETTLED_FRACTION }
            isRetracting = false
        }
    }

    private suspend fun pulseRipple() {
        rippleAnimatable.snapTo(RIPPLE_START)
        rippleAnimatable.animateTo(
            targetValue = RIPPLE_SETTLED,
            animationSpec =
                tween(
                    durationMillis = RIPPLE_DURATION_MILLIS,
                    easing = LinearOutSlowInEasing,
                ),
        )
    }
}

private const val MAX_PULL_FRACTION = 1.5f
private const val PULL_THRESHOLD = 1f
private const val THRESHOLD_REARM = 0.5f
private const val SETTLED_FRACTION = 0.01f
private const val RETRACT_DURATION_MILLIS = 150
private const val RIPPLE_START = 0f
private const val RIPPLE_SETTLED = 1f
private const val RIPPLE_DURATION_MILLIS = 300
