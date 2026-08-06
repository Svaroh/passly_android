package com.passbolt.mobile.android.core.ui.slider

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 128

private val passwordLength = MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LabelledSliderMinimumScreenshot() {
    ScreenshotContainer {
        LabelledSlider(
            title = "Length",
            value = passwordLength.first,
            valueRange = passwordLength,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LabelledSliderMidRangeScreenshot() {
    ScreenshotContainer {
        LabelledSlider(
            title = "Length",
            value = 40,
            valueRange = passwordLength,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LabelledSliderMaximumScreenshot() {
    ScreenshotContainer {
        LabelledSlider(
            title = "Length",
            value = passwordLength.last,
            valueRange = passwordLength,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LabelledSliderDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        LabelledSlider(
            title = "Length",
            value = 40,
            valueRange = passwordLength,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
