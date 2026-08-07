package com.passbolt.mobile.android.core.ui.progressindicator

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltPreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun DataRefreshProgressIndicatorScreenshot() {
    DataRefreshProgressIndicator(progress = 0.4f)
}
