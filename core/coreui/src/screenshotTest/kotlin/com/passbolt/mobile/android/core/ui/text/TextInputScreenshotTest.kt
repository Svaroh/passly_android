package com.passbolt.mobile.android.core.ui.text

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltPreviewWrapper
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun TextInputFilledScreenshot() {
    TextInput(
        title = "Name",
        hint = "Enter resource name",
        isRequired = true,
        text = "Production Database",
        state = Default,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun TextInputEmptyScreenshot() {
    TextInput(
        title = "Description",
        hint = "Add a description",
        isRequired = false,
        text = "",
        state = Default,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun TextInputErrorScreenshot() {
    TextInput(
        title = "Name",
        hint = "Enter resource name",
        isRequired = true,
        text = "",
        state = Error("This field is required"),
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun TextInputMultilineScreenshot() {
    TextInput(
        title = "Description",
        hint = "Add a description",
        text = "A resource description long enough to wrap onto a second line inside the field.",
        minLines = 3,
        state = Default,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun TextInputDarkThemeScreenshot() {
    TextInput(
        title = "Name",
        hint = "Enter resource name",
        isRequired = true,
        text = "Production Database",
        state = Default,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun TextInputErrorDarkThemeScreenshot() {
    TextInput(
        title = "Name",
        hint = "Enter resource name",
        isRequired = true,
        text = "",
        state = Error("This field is required"),
    )
}
