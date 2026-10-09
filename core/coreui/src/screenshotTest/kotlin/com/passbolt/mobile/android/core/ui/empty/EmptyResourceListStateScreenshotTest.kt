package net.svaroh.passly.core.ui.empty

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper

@PreviewTest
@Preview(showBackground = true, heightDp = 400)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun EmptyResourceListStateScreenshot() {
    EmptyResourceListState(title = "No OTPs found")
}

@PreviewTest
@Preview(showBackground = true, heightDp = 400)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun EmptyResourceListStateNoTitleScreenshot() {
    EmptyResourceListState()
}

@PreviewTest
@Preview(showBackground = true, heightDp = 400, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun EmptyResourceListStateDarkThemeScreenshot() {
    EmptyResourceListState(title = "No OTPs found")
}
