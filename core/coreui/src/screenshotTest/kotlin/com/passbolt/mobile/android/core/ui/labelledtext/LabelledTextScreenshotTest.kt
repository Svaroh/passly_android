package com.passbolt.mobile.android.core.ui.labelledtext

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LabelledTextScreenshot() {
    ScreenshotContainer {
        LabelledText(
            label = "Server url",
            text = "https://passbolt.company.com",
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LabelledTextMonospaceScreenshot() {
    ScreenshotContainer {
        LabelledText(
            label = "Fingerprint",
            text = "E8FE 388E 385841B1 0B15 05DC F8E0 3F13 4F1E",
            useMonospaceFont = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LabelledTextWithEndActionScreenshot() {
    ScreenshotContainer {
        LabelledText(
            label = "Server url",
            text = "https://passbolt.company.com",
            endAction =
                LabelledTextEndAction(
                    icon = R.drawable.ic_copy,
                    action = {},
                ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LabelledTextDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        LabelledText(
            label = "Server url",
            text = "https://passbolt.company.com",
            endAction =
                LabelledTextEndAction(
                    icon = R.drawable.ic_copy,
                    action = {},
                ),
        )
    }
}
