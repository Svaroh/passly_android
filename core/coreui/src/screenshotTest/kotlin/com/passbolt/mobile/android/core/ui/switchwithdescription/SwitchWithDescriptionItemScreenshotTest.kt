package com.passbolt.mobile.android.core.ui.switchwithdescription

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ThemedSwitchWithDescriptionItemTitleOnlyScreenshot() {
    SwitchWithDescriptionItem(
        title = "Remember passphrase",
        isChecked = true,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ThemedSwitchWithDescriptionItemWithDescriptionScreenshot() {
    SwitchWithDescriptionItem(
        title = "Remember passphrase",
        description = "Keep the passphrase in memory until the app is closed.",
        isChecked = true,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ThemedSwitchWithDescriptionItemWithAdditionalDescriptionScreenshot() {
    SwitchWithDescriptionItem(
        title = "Remember passphrase",
        description = "Keep the passphrase in memory until the app is closed.",
        additionalDescription = "Disabled by your organisation policy.",
        isChecked = false,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ThemedSwitchWithDescriptionItemDisabledScreenshot() {
    SwitchWithDescriptionItem(
        title = "Remember passphrase",
        description = "Keep the passphrase in memory until the app is closed.",
        isChecked = false,
        isEnabled = false,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun ThemedSwitchWithDescriptionItemDarkThemeScreenshot() {
    SwitchWithDescriptionItem(
        title = "Remember passphrase",
        description = "Keep the passphrase in memory until the app is closed.",
        isChecked = true,
    )
}
