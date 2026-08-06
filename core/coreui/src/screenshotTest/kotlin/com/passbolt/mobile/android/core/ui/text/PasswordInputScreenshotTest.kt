package com.passbolt.mobile.android.core.ui.text

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PasswordInputMaskedScreenshot() {
    ScreenshotContainer {
        PasswordInput(
            title = "Passphrase",
            text = "correct horse battery staple",
            onTextChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PasswordInputEmptyScreenshot() {
    ScreenshotContainer {
        PasswordInput(
            title = "Passphrase",
            text = "",
            hint = "Enter your passphrase",
            onTextChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PasswordInputRequiredScreenshot() {
    ScreenshotContainer {
        PasswordInput(
            title = "Passphrase",
            text = "correct horse battery staple",
            isRequired = true,
            onTextChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PasswordInputErrorScreenshot() {
    ScreenshotContainer {
        PasswordInput(
            title = "Passphrase",
            text = "",
            isRequired = true,
            state = Error("Passphrase is required"),
            onTextChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PasswordInputDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        PasswordInput(
            title = "Passphrase",
            text = "correct horse battery staple",
            isRequired = true,
            onTextChange = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PasswordInputErrorDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        PasswordInput(
            title = "Passphrase",
            text = "",
            isRequired = true,
            state = Error("Passphrase is required"),
            onTextChange = {},
        )
    }
}
