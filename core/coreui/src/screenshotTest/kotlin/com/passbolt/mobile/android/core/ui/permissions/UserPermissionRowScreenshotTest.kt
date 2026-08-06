package com.passbolt.mobile.android.core.ui.permissions

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer
import com.passbolt.mobile.android.core.ui.screenshot.userPermission
import com.passbolt.mobile.android.ui.ResourcePermission.OWNER
import com.passbolt.mobile.android.ui.ResourcePermission.UPDATE

@PreviewTest
@Preview(showBackground = true)
@Composable
fun UserPermissionRowScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        UserPermissionRow(permission = userPermission())
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun UserPermissionRowOwnerScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        UserPermissionRow(permission = userPermission(permission = OWNER))
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun UserPermissionRowSuspendedScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        UserPermissionRow(
            permission =
                userPermission(
                    permission = UPDATE,
                    isDisabled = true,
                ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun UserPermissionRowLongNameScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        UserPermissionRow(
            permission =
                userPermission(
                    firstName = "Augusta Ada",
                    lastName = "King-Noel, Countess of Lovelace",
                    userName = "augusta.ada.king.noel@passbolt.com",
                    permission = OWNER,
                ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun UserPermissionRowDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        UserPermissionRow(permission = userPermission(permission = OWNER))
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun UserPermissionRowSuspendedDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        UserPermissionRow(permission = userPermission(isDisabled = true))
    }
}
