package com.passbolt.mobile.android.core.ui.menu

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun MenuItemScreenshot() {
    MenuItem(
        title = "All items",
        iconResId = R.drawable.ic_list,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun MenuItemSelectedScreenshot() {
    MenuItem(
        title = "All items",
        iconResId = R.drawable.ic_list,
        isSelected = true,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun MenuItemListScreenshot() {
    Column {
        MenuItem(title = "All items", iconResId = R.drawable.ic_list, isSelected = true)
        MenuItem(title = "Favourites", iconResId = R.drawable.ic_star)
        MenuItem(title = "Shared with me", iconResId = R.drawable.ic_share)
        MenuItem(title = "Tags", iconResId = R.drawable.ic_tag)
    }
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun MenuItemLongTitleScreenshot() {
    MenuItem(
        title = "A filter name long enough to be truncated with an ellipsis on a phone",
        iconResId = R.drawable.ic_list,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun MenuItemDarkThemeScreenshot() {
    Column {
        MenuItem(title = "All items", iconResId = R.drawable.ic_list, isSelected = true)
        MenuItem(title = "Favourites", iconResId = R.drawable.ic_star)
    }
}
