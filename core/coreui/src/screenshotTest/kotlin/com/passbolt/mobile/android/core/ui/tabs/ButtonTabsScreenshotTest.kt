package com.passbolt.mobile.android.core.ui.tabs

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltPreviewWrapper

private val twoTabs =
    listOf(
        ButtonTabItemModel("password", "Password", isSelected = true),
        ButtonTabItemModel("passphrase", "Passphrase", isSelected = false),
    )

private val threeTabs =
    listOf(
        ButtonTabItemModel("password", "Password", isSelected = false),
        ButtonTabItemModel("passphrase", "Passphrase", isSelected = true),
        ButtonTabItemModel("pin", "PIN", isSelected = false),
    )

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ButtonTabsScreenshot() {
    ButtonTabs(
        items = twoTabs,
        onSelect = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ButtonTabsThreeItemsScreenshot() {
    ButtonTabs(
        items = threeTabs,
        onSelect = {},
    )
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ButtonTabsLargeFontScreenshot() {
    ButtonTabs(
        items = threeTabs,
        onSelect = {},
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ButtonTabsDarkThemeScreenshot() {
    ButtonTabs(
        items = twoTabs,
        onSelect = {},
    )
}
