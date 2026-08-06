package com.passbolt.mobile.android.core.ui.button

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PrimaryButtonEnabledScreenshot() {
    ScreenshotContainer {
        PrimaryButton(
            text = "Sign in",
            onClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PrimaryButtonDisabledScreenshot() {
    ScreenshotContainer {
        PrimaryButton(
            text = "Sign in",
            onClick = {},
            isEnabled = false,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@Composable
fun PrimaryButtonLargeFontScreenshot() {
    ScreenshotContainer {
        PrimaryButton(
            text = "Sign in",
            onClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PrimaryButtonDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        PrimaryButton(
            text = "Sign in",
            onClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SecondaryButtonScreenshot() {
    ScreenshotContainer {
        SecondaryButton(
            onClick = {},
            text = "Sign out",
            icon = painterResource(id = R.drawable.ic_sign_out),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SecondaryButtonDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        SecondaryButton(
            onClick = {},
            text = "Sign out",
            icon = painterResource(id = R.drawable.ic_sign_out),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SecondaryIconButtonScreenshot() {
    ScreenshotContainer {
        SecondaryIconButton(
            onClick = {},
            icon = painterResource(id = R.drawable.ic_trash),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SecondaryIconButtonDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        SecondaryIconButton(
            onClick = {},
            icon = painterResource(id = R.drawable.ic_trash),
        )
    }
}
