package net.svaroh.passly.core.ui.banner

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltPreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun WarningBannerScreenshot() {
    WarningBanner(text = "Exceeds recommended limit. May cause app slowdowns.")
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun WarningBannerLongTextScreenshot() {
    WarningBanner(
        text =
            "Exceeds the recommended limit for this device tier. Fetching this many resources in one " +
                "page may cause noticeable slowdowns while the list is rendering.",
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun WarningBannerDarkThemeScreenshot() {
    WarningBanner(text = "Exceeds recommended limit. May cause app slowdowns.")
}
