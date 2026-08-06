package com.passbolt.mobile.android.core.ui.menu

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun MenuItemScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        MenuItem(
            title = "All items",
            iconResId = R.drawable.ic_list,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun MenuItemSelectedScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        MenuItem(
            title = "All items",
            iconResId = R.drawable.ic_list,
            isSelected = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun MenuItemListScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        Column {
            MenuItem(title = "All items", iconResId = R.drawable.ic_list, isSelected = true)
            MenuItem(title = "Favourites", iconResId = R.drawable.ic_star)
            MenuItem(title = "Shared with me", iconResId = R.drawable.ic_share)
            MenuItem(title = "Tags", iconResId = R.drawable.ic_tag)
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun MenuItemLongTitleScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        MenuItem(
            title = "A filter name long enough to be truncated with an ellipsis on a phone",
            iconResId = R.drawable.ic_list,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun MenuItemDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        Column {
            MenuItem(title = "All items", iconResId = R.drawable.ic_list, isSelected = true)
            MenuItem(title = "Favourites", iconResId = R.drawable.ic_star)
        }
    }
}
