package com.passbolt.mobile.android.core.ui.search

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltPreviewWrapper

@Composable
private fun SearchLeadingIcon() {
    Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SearchInputEmptyScreenshot() {
    SearchInput(
        onValueChange = {},
        placeholder = "Search passwords",
        endIconMode = SearchInputEndIconMode.AVATAR,
        leadingIcon = { SearchLeadingIcon() },
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SearchInputFilledScreenshot() {
    SearchInput(
        onValueChange = {},
        placeholder = "Search passwords",
        endIconMode = SearchInputEndIconMode.AVATAR,
        initialValue = "database",
        leadingIcon = { SearchLeadingIcon() },
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SearchInputLongPlaceholderScreenshot() {
    SearchInput(
        onValueChange = {},
        placeholder = "Search for passwords, usernames, folders, tags and other items",
        endIconMode = SearchInputEndIconMode.AVATAR,
        leadingIcon = { SearchLeadingIcon() },
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SearchInputClearIconScreenshot() {
    SearchInput(
        onValueChange = {},
        placeholder = "Search passwords",
        endIconMode = SearchInputEndIconMode.CLEAR,
        initialValue = "database",
        leadingIcon = { SearchLeadingIcon() },
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SearchInputNoEndIconScreenshot() {
    SearchInput(
        onValueChange = {},
        placeholder = "Search passwords",
        endIconMode = SearchInputEndIconMode.NONE,
        initialValue = "database",
        leadingIcon = { SearchLeadingIcon() },
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SearchInputDarkThemeScreenshot() {
    SearchInput(
        onValueChange = {},
        placeholder = "Search passwords",
        endIconMode = SearchInputEndIconMode.CLEAR,
        initialValue = "database",
        leadingIcon = { SearchLeadingIcon() },
    )
}
