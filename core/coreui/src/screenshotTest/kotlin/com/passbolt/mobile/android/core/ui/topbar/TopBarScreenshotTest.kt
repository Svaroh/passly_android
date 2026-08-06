package com.passbolt.mobile.android.core.ui.topbar

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TitleAppBarScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        TitleAppBar(title = "Settings")
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TitleAppBarWithBackScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        TitleAppBar(
            title = "App settings",
            navigationIcon = { BackNavigationIcon(onBackClick = {}) },
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TitleAppBarLongTitleScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        TitleAppBar(
            title = "A very long title that should be truncated with ellipsis",
            navigationIcon = { BackNavigationIcon(onBackClick = {}) },
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TitleAppBarWithActionScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        TitleAppBar(
            title = "App settings",
            navigationIcon = { BackNavigationIcon(onBackClick = {}) },
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TitleAppBarWithRefreshProgressScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        TitleAppBar(
            title = "Passwords",
            refreshProgress = 0.4f,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TitleAppBarRtlScreenshot() {
    ScreenshotContainer(isRtl = true, contentPadding = 0.dp) {
        TitleAppBar(
            title = "App settings",
            navigationIcon = { BackNavigationIcon(onBackClick = {}) },
        )
    }
}
