package net.svaroh.passly.core.ui.progresstoolbar

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.R
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import net.svaroh.passly.core.ui.screenshot.PassboltRtlPreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ProgressToolbarEmptyScreenshot() {
    ProgressToolbar(
        progress = 0f,
        onBackClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ProgressToolbarHalfScreenshot() {
    ProgressToolbar(
        progress = 0.5f,
        onBackClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ProgressToolbarCompleteScreenshot() {
    ProgressToolbar(
        progress = 1f,
        onBackClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ProgressToolbarWithEndIconScreenshot() {
    ProgressToolbar(
        progress = 0.5f,
        onBackClick = {},
        endIcon = R.drawable.ic_help,
        onEndIconClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ProgressToolbarDarkThemeScreenshot() {
    ProgressToolbar(
        progress = 0.5f,
        onBackClick = {},
        endIcon = R.drawable.ic_help,
        onEndIconClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltRtlPreviewWrapper::class)
@Composable
fun ProgressToolbarRtlScreenshot() {
    ProgressToolbar(
        progress = 0.5f,
        onBackClick = {},
        endIcon = R.drawable.ic_help,
        onEndIconClick = {},
    )
}
