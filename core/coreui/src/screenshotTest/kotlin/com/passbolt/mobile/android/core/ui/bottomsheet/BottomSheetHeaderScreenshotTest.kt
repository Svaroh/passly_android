package com.passbolt.mobile.android.core.ui.bottomsheet

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BottomSheetHeaderScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        BottomSheetHeader(
            title = "Production Database",
            onClose = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BottomSheetHeaderLongTitleScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        BottomSheetHeader(
            title = "A resource name that is long enough to run past two lines and get truncated with an ellipsis",
            onClose = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun BottomSheetHeaderDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        BottomSheetHeader(
            title = "Production Database",
            onClose = {},
        )
    }
}
