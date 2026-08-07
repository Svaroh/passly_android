package com.passbolt.mobile.android.core.ui.dropdown

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltPreviewWrapper

private val algorithms = listOf("SHA1", "SHA256", "SHA512")

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun DropdownInputScreenshot() {
    DropdownInput(
        title = "Algorithm",
        items = algorithms,
        selectedItem = "SHA1",
        onItemSelect = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun DropdownInputRequiredScreenshot() {
    DropdownInput(
        title = "Algorithm",
        items = algorithms,
        selectedItem = "SHA256",
        onItemSelect = {},
        isRequired = true,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun DropdownInputDarkThemeScreenshot() {
    DropdownInput(
        title = "Algorithm",
        items = algorithms,
        selectedItem = "SHA1",
        onItemSelect = {},
        isRequired = true,
    )
}
