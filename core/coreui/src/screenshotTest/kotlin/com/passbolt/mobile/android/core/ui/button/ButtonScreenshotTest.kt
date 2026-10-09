package net.svaroh.passly.core.ui.button

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.R
import net.svaroh.passly.core.ui.screenshot.PassboltPreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PrimaryButtonEnabledScreenshot() {
    PrimaryButton(
        text = "Sign in",
        onClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PrimaryButtonDisabledScreenshot() {
    PrimaryButton(
        text = "Sign in",
        onClick = {},
        isEnabled = false,
    )
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PrimaryButtonLargeFontScreenshot() {
    PrimaryButton(
        text = "Sign in",
        onClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PrimaryButtonDarkThemeScreenshot() {
    PrimaryButton(
        text = "Sign in",
        onClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SecondaryButtonScreenshot() {
    SecondaryButton(
        onClick = {},
        text = "Sign out",
        icon = painterResource(id = R.drawable.ic_sign_out),
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SecondaryButtonDarkThemeScreenshot() {
    SecondaryButton(
        onClick = {},
        text = "Sign out",
        icon = painterResource(id = R.drawable.ic_sign_out),
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SecondaryIconButtonScreenshot() {
    SecondaryIconButton(
        onClick = {},
        icon = painterResource(id = R.drawable.ic_trash),
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SecondaryIconButtonDarkThemeScreenshot() {
    SecondaryIconButton(
        onClick = {},
        icon = painterResource(id = R.drawable.ic_trash),
    )
}
