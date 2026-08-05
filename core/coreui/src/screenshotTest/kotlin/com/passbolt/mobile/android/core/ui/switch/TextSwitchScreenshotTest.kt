package com.passbolt.mobile.android.core.ui.switch

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TextSwitchCheckedScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        TextSwitch(
            text = "Include symbols",
            isChecked = true,
            onCheckedChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TextSwitchUncheckedScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        TextSwitch(
            text = "Include symbols",
            isChecked = false,
            onCheckedChange = {},
        )
    }
}
