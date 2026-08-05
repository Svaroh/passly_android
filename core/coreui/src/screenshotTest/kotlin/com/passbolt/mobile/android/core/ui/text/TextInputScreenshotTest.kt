package com.passbolt.mobile.android.core.ui.text

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TextInputFilledScreenshot() {
    ScreenshotContainer {
        TextInput(
            title = "Name",
            hint = "Enter resource name",
            isRequired = true,
            text = "Production Database",
            state = Default,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TextInputEmptyScreenshot() {
    ScreenshotContainer {
        TextInput(
            title = "Description",
            hint = "Add a description",
            isRequired = false,
            text = "",
            state = Default,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TextInputErrorScreenshot() {
    ScreenshotContainer {
        TextInput(
            title = "Name",
            hint = "Enter resource name",
            isRequired = true,
            text = "",
            state = Error("This field is required"),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TextInputMultilineScreenshot() {
    ScreenshotContainer {
        TextInput(
            title = "Description",
            hint = "Add a description",
            text = "A resource description long enough to wrap onto a second line inside the field.",
            minLines = 3,
            state = Default,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun TextInputDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        TextInput(
            title = "Name",
            hint = "Enter resource name",
            isRequired = true,
            text = "Production Database",
            state = Default,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun TextInputErrorDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        TextInput(
            title = "Name",
            hint = "Enter resource name",
            isRequired = true,
            text = "",
            state = Error("This field is required"),
        )
    }
}
