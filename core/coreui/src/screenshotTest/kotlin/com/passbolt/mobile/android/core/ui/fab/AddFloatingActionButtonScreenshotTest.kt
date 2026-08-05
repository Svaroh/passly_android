package com.passbolt.mobile.android.core.ui.fab

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddFloatingActionButtonScreenshot() {
    ScreenshotContainer {
        AddFloatingActionButton(onClick = {})
    }
}
