package com.passbolt.mobile.android.core.ui.progresstoolbar

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ProgressToolbarEmptyScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        ProgressToolbar(
            progress = 0f,
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ProgressToolbarHalfScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        ProgressToolbar(
            progress = 0.5f,
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ProgressToolbarCompleteScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        ProgressToolbar(
            progress = 1f,
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ProgressToolbarWithEndIconScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        ProgressToolbar(
            progress = 0.5f,
            onBackClick = {},
            endIcon = R.drawable.ic_help,
            onEndIconClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ProgressToolbarDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        ProgressToolbar(
            progress = 0.5f,
            onBackClick = {},
            endIcon = R.drawable.ic_help,
            onEndIconClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ProgressToolbarRtlScreenshot() {
    ScreenshotContainer(isRtl = true, contentPadding = 0.dp) {
        ProgressToolbar(
            progress = 0.5f,
            onBackClick = {},
            endIcon = R.drawable.ic_help,
            onEndIconClick = {},
        )
    }
}
