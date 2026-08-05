package com.passbolt.mobile.android.core.ui.banner

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun WarningBannerScreenshot() {
    ScreenshotContainer {
        WarningBanner(text = "Exceeds recommended limit. May cause app slowdowns.")
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun WarningBannerLongTextScreenshot() {
    ScreenshotContainer {
        WarningBanner(
            text =
                "Exceeds the recommended limit for this device tier. Fetching this many resources in one " +
                    "page may cause noticeable slowdowns while the list is rendering.",
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun WarningBannerDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        WarningBanner(text = "Exceeds recommended limit. May cause app slowdowns.")
    }
}
