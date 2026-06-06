package net.svaroh.passly.core.ui.permissions

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltPreviewWrapper
import net.svaroh.passly.ui.ResourcePermission.OWNER
import net.svaroh.passly.ui.ResourcePermission.READ

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PermissionSelectorReadSelectedScreenshot() {
    PermissionSelector(
        selectedPermission = READ,
        onPermissionSelect = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PermissionSelectorOwnerSelectedScreenshot() {
    PermissionSelector(
        selectedPermission = OWNER,
        onPermissionSelect = {},
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun PermissionSelectorDarkThemeScreenshot() {
    PermissionSelector(
        selectedPermission = OWNER,
        onPermissionSelect = {},
    )
}
