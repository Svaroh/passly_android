package com.passbolt.mobile.android.core.ui.header

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

private const val PASSWORD = "MyP@ssw0rd!"

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ItemWithHeaderUsernameScreenshot() {
    ScreenshotContainer {
        ItemWithHeader(
            headerText = "Username",
            value = "ada@passbolt.com",
            actionIcon = ActionIcon.COPY,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ItemWithHeaderItalicValueScreenshot() {
    ScreenshotContainer {
        ItemWithHeader(
            headerText = "Username",
            value = "no username",
            actionIcon = ActionIcon.NONE,
            valueFontStyle = FontStyle.Italic,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ItemWithHeaderCustomContentScreenshot() {
    ScreenshotContainer {
        ItemWithHeader(
            headerText = "Expiry",
            content = {
                Text(
                    text = "Never expires",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ItemWithHeaderSecretHiddenScreenshot() {
    ScreenshotContainer {
        ItemWithHeader(
            headerText = "Password",
            value = "",
            valueStyle = ValueStyle.Secret(),
            actionIcon = ActionIcon.VIEW,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ItemWithHeaderSecretVisibleScreenshot() {
    ScreenshotContainer {
        ItemWithHeader(
            headerText = "Password",
            value = PASSWORD,
            valueStyle = ValueStyle.Secret(isRevealed = true),
            actionIcon = ActionIcon.HIDE,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ItemWithHeaderSecretDifferentiatedScreenshot() {
    ScreenshotContainer {
        ItemWithHeader(
            headerText = "Password",
            value = PASSWORD,
            valueStyle = ValueStyle.Secret(differentiateCharacters = true, isRevealed = true),
            actionIcon = ActionIcon.HIDE,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ItemWithHeaderLinkifiedScreenshot() {
    ScreenshotContainer {
        ItemWithHeader(
            headerText = "URL",
            value = "Staging is at https://staging.passbolt.com and production at https://passbolt.com",
            valueStyle = ValueStyle.Linkified,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ItemWithHeaderConcealedScreenshot() {
    ScreenshotContainer {
        ItemWithHeader(
            headerText = "Password",
            value = PASSWORD,
            valueStyle = ValueStyle.Concealed,
            actionIcon = ActionIcon.VIEW,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ItemWithHeaderUsernameDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        ItemWithHeader(
            headerText = "Username",
            value = "ada@passbolt.com",
            actionIcon = ActionIcon.COPY,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ItemWithHeaderSecretDifferentiatedDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        ItemWithHeader(
            headerText = "Password",
            value = PASSWORD,
            valueStyle = ValueStyle.Secret(differentiateCharacters = true, isRevealed = true),
            actionIcon = ActionIcon.HIDE,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ItemWithHeaderConcealedDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        ItemWithHeader(
            headerText = "Password",
            value = PASSWORD,
            valueStyle = ValueStyle.Concealed,
            actionIcon = ActionIcon.VIEW,
        )
    }
}
