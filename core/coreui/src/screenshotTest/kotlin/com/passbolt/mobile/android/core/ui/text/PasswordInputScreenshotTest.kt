package com.passbolt.mobile.android.core.ui.text

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltPreviewWrapper
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PasswordInputMaskedScreenshot() {
    PasswordInput(
        title = "Passphrase",
        text = "correct horse battery staple",
        onTextChange = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PasswordInputEmptyScreenshot() {
    PasswordInput(
        title = "Passphrase",
        text = "",
        hint = "Enter your passphrase",
        onTextChange = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PasswordInputRequiredScreenshot() {
    PasswordInput(
        title = "Passphrase",
        text = "correct horse battery staple",
        isRequired = true,
        onTextChange = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PasswordInputErrorScreenshot() {
    PasswordInput(
        title = "Passphrase",
        text = "",
        isRequired = true,
        state = Error("Passphrase is required"),
        onTextChange = {},
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PasswordInputDarkThemeScreenshot() {
    PasswordInput(
        title = "Passphrase",
        text = "correct horse battery staple",
        isRequired = true,
        onTextChange = {},
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PasswordInputErrorDarkThemeScreenshot() {
    PasswordInput(
        title = "Passphrase",
        text = "",
        isRequired = true,
        state = Error("Passphrase is required"),
        onTextChange = {},
    )
}
