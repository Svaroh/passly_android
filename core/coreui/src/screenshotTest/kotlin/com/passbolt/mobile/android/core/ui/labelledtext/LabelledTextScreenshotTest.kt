package net.svaroh.passly.core.ui.labelledtext

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.R
import net.svaroh.passly.core.ui.screenshot.PassboltPreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun LabelledTextScreenshot() {
    LabelledText(
        label = "Server url",
        text = "https://passbolt.company.com",
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun LabelledTextMonospaceScreenshot() {
    LabelledText(
        label = "Fingerprint",
        text = "E8FE 388E 385841B1 0B15 05DC F8E0 3F13 4F1E",
        useMonospaceFont = true,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun LabelledTextWithEndActionScreenshot() {
    LabelledText(
        label = "Server url",
        text = "https://passbolt.company.com",
        endAction =
            LabelledTextEndAction(
                icon = R.drawable.ic_copy,
                action = {},
            ),
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun LabelledTextDarkThemeScreenshot() {
    LabelledText(
        label = "Server url",
        text = "https://passbolt.company.com",
        endAction =
            LabelledTextEndAction(
                icon = R.drawable.ic_copy,
                action = {},
            ),
    )
}
