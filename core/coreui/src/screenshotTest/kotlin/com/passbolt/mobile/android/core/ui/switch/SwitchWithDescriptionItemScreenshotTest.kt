package net.svaroh.passly.core.ui.switch

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
fun SwitchWithDescriptionItemCheckedScreenshot() {
    SwitchWithDescriptionItem(
        title = "Use autofill service",
        description = "Fill passwords in other apps from the Android autofill framework.",
        isChecked = true,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SwitchWithDescriptionItemUncheckedScreenshot() {
    SwitchWithDescriptionItem(
        title = "Use autofill service",
        description = "Fill passwords in other apps from the Android autofill framework.",
        isChecked = false,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SwitchWithDescriptionItemWithAdditionalDescriptionScreenshot() {
    SwitchWithDescriptionItem(
        title = "Use autofill service",
        description = "Fill passwords in other apps from the Android autofill framework.",
        additionalDescription = "Requires the accessibility service on some devices.",
        isChecked = true,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SwitchWithDescriptionItemDisabledScreenshot() {
    SwitchWithDescriptionItem(
        title = "Use autofill service",
        description = "Fill passwords in other apps from the Android autofill framework.",
        isChecked = true,
        isEnabled = false,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SwitchWithDescriptionItemDarkThemeScreenshot() {
    SwitchWithDescriptionItem(
        title = "Use autofill service",
        description = "Fill passwords in other apps from the Android autofill framework.",
        isChecked = true,
    )
}
