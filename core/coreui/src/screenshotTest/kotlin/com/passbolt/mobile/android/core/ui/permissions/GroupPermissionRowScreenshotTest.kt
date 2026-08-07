package com.passbolt.mobile.android.core.ui.permissions

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import com.passbolt.mobile.android.core.ui.screenshot.PassboltRtlPreviewWrapper
import com.passbolt.mobile.android.core.ui.screenshot.groupPermission
import com.passbolt.mobile.android.ui.ResourcePermission.OWNER
import com.passbolt.mobile.android.ui.ResourcePermission.READ

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun GroupPermissionRowScreenshot() {
    GroupPermissionRow(
        permission = groupPermission(groupName = "Engineering", permission = READ),
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun GroupPermissionRowOwnerScreenshot() {
    GroupPermissionRow(
        permission = groupPermission(groupName = "Engineering", permission = OWNER),
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun GroupPermissionRowLongNameScreenshot() {
    GroupPermissionRow(
        permission =
            groupPermission(
                groupName = "Engineering, Platform and Infrastructure (Europe)",
                permission = OWNER,
            ),
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun GroupPermissionRowDarkThemeScreenshot() {
    GroupPermissionRow(
        permission = groupPermission(groupName = "Engineering", permission = OWNER),
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltRtlPreviewWrapper::class)
@Composable
fun GroupPermissionRowRtlScreenshot() {
    GroupPermissionRow(
        permission = groupPermission(groupName = "Engineering", permission = OWNER),
    )
}
