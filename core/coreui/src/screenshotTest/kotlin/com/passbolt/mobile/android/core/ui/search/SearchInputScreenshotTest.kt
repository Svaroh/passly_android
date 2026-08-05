package com.passbolt.mobile.android.core.ui.search

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@Composable
private fun SearchLeadingIcon() {
    Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SearchInputEmptyScreenshot() {
    ScreenshotContainer {
        SearchInput(
            onValueChange = {},
            placeholder = "Search passwords",
            endIconMode = SearchInputEndIconMode.AVATAR,
            leadingIcon = { SearchLeadingIcon() },
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SearchInputFilledScreenshot() {
    ScreenshotContainer {
        SearchInput(
            onValueChange = {},
            placeholder = "Search passwords",
            endIconMode = SearchInputEndIconMode.AVATAR,
            initialValue = "database",
            leadingIcon = { SearchLeadingIcon() },
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SearchInputLongPlaceholderScreenshot() {
    ScreenshotContainer {
        SearchInput(
            onValueChange = {},
            placeholder = "Search for passwords, usernames, folders, tags and other items",
            endIconMode = SearchInputEndIconMode.AVATAR,
            leadingIcon = { SearchLeadingIcon() },
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SearchInputClearIconScreenshot() {
    ScreenshotContainer {
        SearchInput(
            onValueChange = {},
            placeholder = "Search passwords",
            endIconMode = SearchInputEndIconMode.CLEAR,
            initialValue = "database",
            leadingIcon = { SearchLeadingIcon() },
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SearchInputNoEndIconScreenshot() {
    ScreenshotContainer {
        SearchInput(
            onValueChange = {},
            placeholder = "Search passwords",
            endIconMode = SearchInputEndIconMode.NONE,
            initialValue = "database",
            leadingIcon = { SearchLeadingIcon() },
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SearchInputDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        SearchInput(
            onValueChange = {},
            placeholder = "Search passwords",
            endIconMode = SearchInputEndIconMode.CLEAR,
            initialValue = "database",
            leadingIcon = { SearchLeadingIcon() },
        )
    }
}
