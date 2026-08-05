package com.passbolt.mobile.android.core.ui.section

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SectionScreenshot() {
    ScreenshotContainer {
        Section(title = "Shared with") {
            Text(
                text = "Content inside the section",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SectionWithoutTitleScreenshot() {
    ScreenshotContainer {
        Section {
            Text(
                text = "Content inside the section",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SectionDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        Section(title = "Shared with") {
            Text(
                text = "Content inside the section",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
