package com.passbolt.mobile.android.core.ui.menu

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsItemStandardScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_app_settings),
            title = "App settings",
            onClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsItemWithWarningScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_app_settings),
            title = "App settings",
            onClick = {},
            hasWarningBadge = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsItemDisabledScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_app_settings),
            title = "App settings",
            onClick = {},
            isEnabled = false,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsItemListScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        Column {
            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_app_settings),
                title = "App settings",
                onClick = {},
                hasWarningBadge = true,
            )
            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_manage_accounts),
                title = "Accounts",
                onClick = {},
            )
            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_terms),
                title = "Terms and licenses",
                onClick = {},
            )
            OpenableSettingsItem(
                iconPainter = painterResource(R.drawable.ic_sign_out),
                title = "Sign out",
                onClick = {},
                opensInternally = false,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SettingsItemDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_app_settings),
            title = "App settings",
            onClick = {},
            hasWarningBadge = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@Composable
fun SettingsItemLargeFontScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_app_settings),
            title = "App settings",
            onClick = {},
            hasWarningBadge = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsItemRtlScreenshot() {
    ScreenshotContainer(isRtl = true, contentPadding = 0.dp) {
        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_app_settings),
            title = "App settings",
            onClick = {},
            hasWarningBadge = true,
        )
    }
}
