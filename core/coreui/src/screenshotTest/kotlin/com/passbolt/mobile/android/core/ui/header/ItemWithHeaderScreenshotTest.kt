package com.passbolt.mobile.android.core.ui.header

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltPreviewWrapper

private const val PASSWORD = "MyP@ssw0rd!"

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderUsernameScreenshot() {
    ItemWithHeader(
        headerText = "Username",
        value = "ada@passbolt.com",
        actionIcon = ActionIcon.COPY,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderItalicValueScreenshot() {
    ItemWithHeader(
        headerText = "Username",
        value = "no username",
        actionIcon = ActionIcon.NONE,
        valueFontStyle = FontStyle.Italic,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderCustomContentScreenshot() {
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

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderSecretHiddenScreenshot() {
    ItemWithHeader(
        headerText = "Password",
        value = "",
        valueStyle = ValueStyle.Secret(),
        actionIcon = ActionIcon.VIEW,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderSecretVisibleScreenshot() {
    ItemWithHeader(
        headerText = "Password",
        value = PASSWORD,
        valueStyle = ValueStyle.Secret(isRevealed = true),
        actionIcon = ActionIcon.HIDE,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderSecretDifferentiatedScreenshot() {
    ItemWithHeader(
        headerText = "Password",
        value = PASSWORD,
        valueStyle = ValueStyle.Secret(differentiateCharacters = true, isRevealed = true),
        actionIcon = ActionIcon.HIDE,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderLinkifiedScreenshot() {
    ItemWithHeader(
        headerText = "URL",
        value = "Staging is at https://staging.passbolt.com and production at https://passbolt.com",
        valueStyle = ValueStyle.Linkified,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderConcealedScreenshot() {
    ItemWithHeader(
        headerText = "Password",
        value = PASSWORD,
        valueStyle = ValueStyle.Concealed,
        actionIcon = ActionIcon.VIEW,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderUsernameDarkThemeScreenshot() {
    ItemWithHeader(
        headerText = "Username",
        value = "ada@passbolt.com",
        actionIcon = ActionIcon.COPY,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderSecretDifferentiatedDarkThemeScreenshot() {
    ItemWithHeader(
        headerText = "Password",
        value = PASSWORD,
        valueStyle = ValueStyle.Secret(differentiateCharacters = true, isRevealed = true),
        actionIcon = ActionIcon.HIDE,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun ItemWithHeaderConcealedDarkThemeScreenshot() {
    ItemWithHeader(
        headerText = "Password",
        value = PASSWORD,
        valueStyle = ValueStyle.Concealed,
        actionIcon = ActionIcon.VIEW,
    )
}
