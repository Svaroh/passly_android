package com.passbolt.mobile.android.core.ui.dropdown

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

private val algorithms = listOf("SHA1", "SHA256", "SHA512")

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DropdownInputScreenshot() {
    ScreenshotContainer {
        DropdownInput(
            title = "Algorithm",
            items = algorithms,
            selectedItem = "SHA1",
            onItemSelect = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DropdownInputRequiredScreenshot() {
    ScreenshotContainer {
        DropdownInput(
            title = "Algorithm",
            items = algorithms,
            selectedItem = "SHA256",
            onItemSelect = {},
            isRequired = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun DropdownInputDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        DropdownInput(
            title = "Algorithm",
            items = algorithms,
            selectedItem = "SHA1",
            onItemSelect = {},
            isRequired = true,
        )
    }
}
