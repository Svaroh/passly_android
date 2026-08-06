package com.passbolt.mobile.android.core.ui.permissions

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer
import com.passbolt.mobile.android.ui.ResourcePermission.OWNER
import com.passbolt.mobile.android.ui.ResourcePermission.READ
import com.passbolt.mobile.android.ui.ResourcePermission.UPDATE

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PermissionLabelReadScreenshot() {
    ScreenshotContainer {
        PermissionLabel(permission = READ)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PermissionLabelUpdateScreenshot() {
    ScreenshotContainer {
        PermissionLabel(permission = UPDATE)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PermissionLabelOwnerScreenshot() {
    ScreenshotContainer {
        PermissionLabel(permission = OWNER)
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PermissionLabelDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        PermissionLabel(permission = OWNER)
    }
}
