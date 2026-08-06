package com.passbolt.mobile.android.core.ui.permissions

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer
import com.passbolt.mobile.android.ui.ResourcePermission.OWNER
import com.passbolt.mobile.android.ui.ResourcePermission.READ

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PermissionSelectorReadSelectedScreenshot() {
    ScreenshotContainer {
        PermissionSelector(
            selectedPermission = READ,
            onPermissionSelect = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PermissionSelectorOwnerSelectedScreenshot() {
    ScreenshotContainer {
        PermissionSelector(
            selectedPermission = OWNER,
            onPermissionSelect = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PermissionSelectorDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        PermissionSelector(
            selectedPermission = OWNER,
            onPermissionSelect = {},
        )
    }
}
