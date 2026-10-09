package net.svaroh.passly.core.ui.bottomsheet

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun BottomSheetHeaderScreenshot() {
    BottomSheetHeader(
        title = "Production Database",
        onClose = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun BottomSheetHeaderLongTitleScreenshot() {
    BottomSheetHeader(
        title = "A resource name that is long enough to run past two lines and get truncated with an ellipsis",
        onClose = {},
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun BottomSheetHeaderDarkThemeScreenshot() {
    BottomSheetHeader(
        title = "Production Database",
        onClose = {},
    )
}
