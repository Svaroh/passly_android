package com.passbolt.mobile.android.core.ui.switch

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SwitchWithDescriptionItemCheckedScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Use autofill service",
            description = "Fill passwords in other apps from the Android autofill framework.",
            isChecked = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SwitchWithDescriptionItemUncheckedScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Use autofill service",
            description = "Fill passwords in other apps from the Android autofill framework.",
            isChecked = false,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SwitchWithDescriptionItemWithAdditionalDescriptionScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Use autofill service",
            description = "Fill passwords in other apps from the Android autofill framework.",
            additionalDescription = "Requires the accessibility service on some devices.",
            isChecked = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SwitchWithDescriptionItemDisabledScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Use autofill service",
            description = "Fill passwords in other apps from the Android autofill framework.",
            isChecked = true,
            isEnabled = false,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SwitchWithDescriptionItemDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        SwitchWithDescriptionItem(
            title = "Use autofill service",
            description = "Fill passwords in other apps from the Android autofill framework.",
            isChecked = true,
        )
    }
}
