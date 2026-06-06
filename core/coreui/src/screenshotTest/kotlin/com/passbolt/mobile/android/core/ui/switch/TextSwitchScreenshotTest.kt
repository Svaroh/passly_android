package net.svaroh.passly.core.ui.switch

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun TextSwitchCheckedScreenshot() {
    TextSwitch(
        text = "Include symbols",
        isChecked = true,
        onCheckedChange = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun TextSwitchUncheckedScreenshot() {
    TextSwitch(
        text = "Include symbols",
        isChecked = false,
        onCheckedChange = {},
    )
}
