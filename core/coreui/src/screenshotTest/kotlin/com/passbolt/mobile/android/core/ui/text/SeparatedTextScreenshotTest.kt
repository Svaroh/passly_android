package com.passbolt.mobile.android.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SeparatedTextScreenshot() {
    ScreenshotContainer {
        SeparatedText(segments = listOf("Projects", "Mobile Apps", "Android"))
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SeparatedTextOverflowingScreenshot() {
    ScreenshotContainer {
        SeparatedText(
            segments =
                listOf(
                    "Projects",
                    "Mobile Apps",
                    "Android",
                    "Release Credentials",
                    "Play Console",
                ),
        )
    }
}
