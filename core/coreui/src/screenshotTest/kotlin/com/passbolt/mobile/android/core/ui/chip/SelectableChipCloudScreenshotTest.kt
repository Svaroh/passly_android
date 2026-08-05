package com.passbolt.mobile.android.core.ui.chip

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

private val characterGroups =
    listOf(
        SelectableChipItemModel("upper", "A-Z", isSelected = true),
        SelectableChipItemModel("digit", "0-9", isSelected = true),
        SelectableChipItemModel("lower", "a-z", isSelected = true),
        SelectableChipItemModel("special1", "# $ % &", isSelected = true),
        SelectableChipItemModel("parenthesis", "{ [ ( | ) ] }", isSelected = false),
        SelectableChipItemModel("punctuation", "< > ? /", isSelected = false),
    )

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SelectableChipCloudScreenshot() {
    ScreenshotContainer {
        SelectableChipCloud(
            items = characterGroups,
            onToggle = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SelectableChipCloudDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        SelectableChipCloud(
            items = characterGroups,
            onToggle = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@Composable
fun SelectableChipCloudLargeFontScreenshot() {
    ScreenshotContainer {
        SelectableChipCloud(
            items = characterGroups,
            onToggle = {},
        )
    }
}
