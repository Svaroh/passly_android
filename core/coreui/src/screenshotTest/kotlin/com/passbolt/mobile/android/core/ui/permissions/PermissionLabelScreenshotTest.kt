package com.passbolt.mobile.android.core.ui.permissions

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.PassboltPreviewWrapper
import com.passbolt.mobile.android.ui.ResourcePermission.OWNER
import com.passbolt.mobile.android.ui.ResourcePermission.READ
import com.passbolt.mobile.android.ui.ResourcePermission.UPDATE

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PermissionLabelReadScreenshot() {
    PermissionLabel(permission = READ)
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PermissionLabelUpdateScreenshot() {
    PermissionLabel(permission = UPDATE)
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PermissionLabelOwnerScreenshot() {
    PermissionLabel(permission = OWNER)
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PermissionLabelDarkThemeScreenshot() {
    PermissionLabel(permission = OWNER)
}
