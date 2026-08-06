package com.passbolt.mobile.android.core.ui.menu

import android.content.res.Configuration.UI_MODE_NIGHT_YES
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
fun SwitchableSettingsItemCheckedScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_bug),
            title = "Enable debug logs",
            isChecked = true,
            onCheckedChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SwitchableSettingsItemUncheckedScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_bug),
            title = "Enable debug logs",
            isChecked = false,
            onCheckedChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SwitchableSettingsItemDisabledScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_bug),
            title = "Enable debug logs",
            isChecked = true,
            onCheckedChange = {},
            isEnabled = false,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SwitchableSettingsItemDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        SwitchableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_bug),
            title = "Enable debug logs",
            isChecked = true,
            onCheckedChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@Composable
fun SwitchableSettingsItemLargeFontScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_bug),
            title = "Enable debug logs",
            isChecked = true,
            onCheckedChange = {},
        )
    }
}
