package com.passbolt.mobile.android.core.ui.progressindicator

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DataRefreshProgressIndicatorScreenshot() {
    ScreenshotContainer {
        DataRefreshProgressIndicator(progress = 0.4f)
    }
}
