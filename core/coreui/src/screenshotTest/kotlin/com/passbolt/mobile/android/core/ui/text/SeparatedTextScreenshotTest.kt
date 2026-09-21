package com.passbolt.mobile.android.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltPreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SeparatedTextScreenshot() {
    SeparatedText(segments = listOf("Projects", "Mobile Apps", "Android"))
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SeparatedTextOverflowingScreenshot() {
    SeparatedText(
        segments =
            listOf(
                "Projects",
                "Mobile Apps",
                "Android",
                "Release Credentials",
                "Play Console",
            ),
    )
}
