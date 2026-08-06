package com.passbolt.mobile.android.core.ui.switchwithdescription

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ThemedSwitchWithDescriptionItemTitleOnlyScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Remember passphrase",
            isChecked = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ThemedSwitchWithDescriptionItemWithDescriptionScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Remember passphrase",
            description = "Keep the passphrase in memory until the app is closed.",
            isChecked = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ThemedSwitchWithDescriptionItemWithAdditionalDescriptionScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Remember passphrase",
            description = "Keep the passphrase in memory until the app is closed.",
            additionalDescription = "Disabled by your organisation policy.",
            isChecked = false,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ThemedSwitchWithDescriptionItemDisabledScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Remember passphrase",
            description = "Keep the passphrase in memory until the app is closed.",
            isChecked = false,
            isEnabled = false,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ThemedSwitchWithDescriptionItemDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Remember passphrase",
            description = "Keep the passphrase in memory until the app is closed.",
            isChecked = true,
        )
    }
}
