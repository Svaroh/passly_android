package net.svaroh.passly.core.ui.menu

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.R
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SwitchableSettingsItemCheckedScreenshot() {
    SwitchableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_bug),
        title = "Enable debug logs",
        isChecked = true,
        onCheckedChange = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SwitchableSettingsItemUncheckedScreenshot() {
    SwitchableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_bug),
        title = "Enable debug logs",
        isChecked = false,
        onCheckedChange = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SwitchableSettingsItemDisabledScreenshot() {
    SwitchableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_bug),
        title = "Enable debug logs",
        isChecked = true,
        onCheckedChange = {},
        isEnabled = false,
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SwitchableSettingsItemDarkThemeScreenshot() {
    SwitchableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_bug),
        title = "Enable debug logs",
        isChecked = true,
        onCheckedChange = {},
    )
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SwitchableSettingsItemLargeFontScreenshot() {
    SwitchableSettingsItem(
        iconPainter = painterResource(R.drawable.ic_bug),
        title = "Enable debug logs",
        isChecked = true,
        onCheckedChange = {},
    )
}
