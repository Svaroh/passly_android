package com.passbolt.mobile.android.core.ui.menu

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import com.passbolt.mobile.android.core.ui.screenshot.PassboltRtlPreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SettingsItemStandardScreenshot() {
    OpenableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_app_settings),
        title = "App settings",
        onClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SettingsItemWithWarningScreenshot() {
    OpenableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_app_settings),
        title = "App settings",
        onClick = {},
        hasWarningBadge = true,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SettingsItemDisabledScreenshot() {
    OpenableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_app_settings),
        title = "App settings",
        onClick = {},
        isEnabled = false,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SettingsItemListScreenshot() {
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

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SettingsItemDarkThemeScreenshot() {
    OpenableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_app_settings),
        title = "App settings",
        onClick = {},
        hasWarningBadge = true,
    )
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SettingsItemLargeFontScreenshot() {
    OpenableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_app_settings),
        title = "App settings",
        onClick = {},
        hasWarningBadge = true,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltRtlPreviewWrapper::class)
@Composable
fun SettingsItemRtlScreenshot() {
    OpenableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_app_settings),
        title = "App settings",
        onClick = {},
        hasWarningBadge = true,
    )
}
