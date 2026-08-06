package com.passbolt.mobile.android.core.ui.empty

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true, heightDp = 400)
@Composable
fun EmptyResourceListStateScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        EmptyResourceListState(title = "No OTPs found")
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 400)
@Composable
fun EmptyResourceListStateNoTitleScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        EmptyResourceListState()
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 400, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun EmptyResourceListStateDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        EmptyResourceListState(title = "No OTPs found")
    }
}
