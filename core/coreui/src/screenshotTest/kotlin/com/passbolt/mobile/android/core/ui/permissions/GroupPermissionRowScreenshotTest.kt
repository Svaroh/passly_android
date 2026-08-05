package com.passbolt.mobile.android.core.ui.permissions

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer
import com.passbolt.mobile.android.core.ui.screenshot.groupPermission
import com.passbolt.mobile.android.ui.ResourcePermission.OWNER
import com.passbolt.mobile.android.ui.ResourcePermission.READ

@PreviewTest
@Preview(showBackground = true)
@Composable
fun GroupPermissionRowScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        GroupPermissionRow(
            permission = groupPermission(groupName = "Engineering", permission = READ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun GroupPermissionRowOwnerScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        GroupPermissionRow(
            permission = groupPermission(groupName = "Engineering", permission = OWNER),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun GroupPermissionRowLongNameScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        GroupPermissionRow(
            permission =
                groupPermission(
                    groupName = "Engineering, Platform and Infrastructure (Europe)",
                    permission = OWNER,
                ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun GroupPermissionRowDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        GroupPermissionRow(
            permission = groupPermission(groupName = "Engineering", permission = OWNER),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun GroupPermissionRowRtlScreenshot() {
    ScreenshotContainer(isRtl = true, contentPadding = 0.dp) {
        GroupPermissionRow(
            permission = groupPermission(groupName = "Engineering", permission = OWNER),
        )
    }
}
