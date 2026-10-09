package net.svaroh.passly.feature.settings.screen

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper

private const val SCREEN_WIDTH_DP = 360
private const val SCREEN_HEIGHT_DP = 800

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SettingsScreenScreenshot() {
    SettingsScreen(
        state = SettingsState(),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SettingsScreenAutofillConflictScreenshot() {
    SettingsScreen(
        state = SettingsState(isAutofillConflictDetected = true),
        onIntent = {},
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SettingsScreenDarkThemeScreenshot() {
    SettingsScreen(
        state = SettingsState(),
        onIntent = {},
    )
}
