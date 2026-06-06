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

package net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.ui.banner.WarningBanner
import net.svaroh.passly.core.ui.button.PrimaryButton
import net.svaroh.passly.core.ui.topbar.BackNavigationIcon
import net.svaroh.passly.core.ui.topbar.TitleAppBar
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.GoBack
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.PageSizeChanged
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.RestoreDefaultsClick
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.SaveClick
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeSideEffect.NavigateBack
import net.svaroh.passly.testtags.composetags.PageSize
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

private const val INACTIVE_TRACK_ALPHA = 0.38f
private val TRACK_HEIGHT = 8.dp
private val TICK_RADIUS = 2.dp
private val THUMB_GAP = 10.dp

@Composable
internal fun PageSizeScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: PageSizeViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateBack -> navigator.navigateBack()
        }
    }

    PageSizeScreen(
        modifier = modifier,
        state = state.value,
        allowedPageSizes = ALLOWED_PAGE_SIZES,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun PageSizeScreen(
    state: PageSizeState,
    allowedPageSizes: List<Int>,
    onIntent: (PageSizeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.settings_app_settings_expert_settings_fetch_page_size),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        content = { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(LocalizationR.string.settings_page_size_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                PageSizeSlider(
                    selectedIndex = state.selectedIndex,
                    recommendedLimitIndex = state.recommendedLimitIndex,
                    isOverRecommendedLimit = state.isOverRecommendedLimit,
                    allowedPageSizes = allowedPageSizes,
                    onSliderIndexChange = { onIntent(PageSizeChanged(it)) },
                )

                if (state.isOverRecommendedLimit) {
                    WarningBanner(
                        text = stringResource(LocalizationR.string.settings_page_size_exceeds_recommended_limit),
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                PrimaryButton(
                    text = stringResource(LocalizationR.string.save),
                    isEnabled = state.hasUnsavedChange,
                    onClick = { onIntent(SaveClick) },
                )

                TextButton(
                    onClick = { onIntent(RestoreDefaultsClick) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                ) {
                    Text(
                        text = stringResource(LocalizationR.string.settings_page_size_restore_default_values),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageSizeSlider(
    selectedIndex: Int,
    recommendedLimitIndex: Int,
    isOverRecommendedLimit: Boolean,
    allowedPageSizes: List<Int>,
    onSliderIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val numberFormat = remember(locale) { NumberFormat.getNumberInstance(locale) }
    val overLimitColor = colorResource(CoreUiR.color.yellow)
    val activeColor = if (isOverRecommendedLimit) overLimitColor else MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = numberFormat.format(allowedPageSizes[selectedIndex]),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(PageSize.HEADLINE),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { onSliderIndexChange(it.roundToInt()) },
            valueRange = 0f..allowedPageSizes.lastIndex.toFloat(),
            steps = allowedPageSizes.size - 2,
            colors = SliderDefaults.colors(thumbColor = activeColor),
            track = { sliderState ->
                PageSizeTrack(
                    sliderState = sliderState,
                    recommendedLimitFraction = recommendedLimitIndex.toFloat() / allowedPageSizes.lastIndex,
                    activeColor = activeColor,
                    overLimitColor = overLimitColor,
                )
            },
            modifier = Modifier.testTag(PageSize.SLIDER),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = numberFormat.format(allowedPageSizes.first()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = numberFormat.format(allowedPageSizes.last()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageSizeTrack(
    sliderState: SliderState,
    recommendedLimitFraction: Float,
    activeColor: Color,
    overLimitColor: Color,
    modifier: Modifier = Modifier,
) {
    val inactiveColor = activeColor.copy(alpha = INACTIVE_TRACK_ALPHA)
    val overLimitZoneColor = overLimitColor.copy(alpha = INACTIVE_TRACK_ALPHA)
    val tickColor = MaterialTheme.colorScheme.background

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TRACK_HEIGHT),
    ) {
        val strokeWidth = size.height
        val capRadius = strokeWidth / 2
        val trackWidth = size.width - strokeWidth
        if (trackWidth <= 0f) return@Canvas
        val gapFraction = THUMB_GAP.toPx() / trackWidth
        val valueFraction =
            with(sliderState) {
                (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            }

        fun xAt(fraction: Float): Float {
            val x = capRadius + fraction * trackWidth
            return if (layoutDirection == LayoutDirection.Rtl) size.width - x else x
        }

        fun drawTrackSegment(
            startFraction: Float,
            endFraction: Float,
            color: Color,
        ) {
            if (endFraction <= startFraction) return
            drawLine(
                color = color,
                start = Offset(xAt(startFraction), center.y),
                end = Offset(xAt(endFraction), center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }

        drawTrackSegment(0f, valueFraction - gapFraction, activeColor)
        drawTrackSegment(valueFraction + gapFraction, recommendedLimitFraction, inactiveColor)
        drawTrackSegment(max(valueFraction + gapFraction, recommendedLimitFraction), 1f, overLimitZoneColor)

        val stepCount = sliderState.steps + 1
        for (step in 0..stepCount) {
            val stepFraction = step.toFloat() / stepCount
            if (abs(stepFraction - valueFraction) <= gapFraction) continue
            drawCircle(
                color = tickColor,
                radius = TICK_RADIUS.toPx(),
                center = Offset(xAt(stepFraction), center.y),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PageSizeScreenPreview() {
    PageSizeScreen(
        state = PageSizeState(),
        allowedPageSizes = ALLOWED_PAGE_SIZES,
        onIntent = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PageSizeScreenOverLimitPreview() {
    PageSizeScreen(
        state =
            PageSizeState(
                selectedIndex = ALLOWED_PAGE_SIZES.lastIndex,
                savedIndex = 3,
                automaticDefaultIndex = 4,
                recommendedLimitIndex = 5,
            ),
        allowedPageSizes = ALLOWED_PAGE_SIZES,
        onIntent = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PageSizeScreenSmallValuePreview() {
    PageSizeScreen(
        state = PageSizeState(selectedIndex = 1),
        allowedPageSizes = ALLOWED_PAGE_SIZES,
        onIntent = {},
    )
}
