package com.passbolt.mobile.android.core.ui.tabs

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

private val twoTabs =
    listOf(
        ButtonTabItemModel("password", "Password", isSelected = true),
        ButtonTabItemModel("passphrase", "Passphrase", isSelected = false),
    )

private val threeTabs =
    listOf(
        ButtonTabItemModel("password", "Password", isSelected = false),
        ButtonTabItemModel("passphrase", "Passphrase", isSelected = true),
        ButtonTabItemModel("pin", "PIN", isSelected = false),
    )

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ButtonTabsScreenshot() {
    ScreenshotContainer {
        ButtonTabs(
            items = twoTabs,
            onSelect = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ButtonTabsThreeItemsScreenshot() {
    ScreenshotContainer {
        ButtonTabs(
            items = threeTabs,
            onSelect = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@Composable
fun ButtonTabsLargeFontScreenshot() {
    ScreenshotContainer {
        ButtonTabs(
            items = threeTabs,
            onSelect = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ButtonTabsDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        ButtonTabs(
            items = twoTabs,
            onSelect = {},
        )
    }
}
