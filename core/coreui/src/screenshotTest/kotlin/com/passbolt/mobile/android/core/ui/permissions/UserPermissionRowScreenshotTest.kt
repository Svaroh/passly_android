package net.svaroh.passly.core.ui.permissions

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import net.svaroh.passly.core.ui.screenshot.userPermission
import net.svaroh.passly.ui.ResourcePermission.OWNER
import net.svaroh.passly.ui.ResourcePermission.UPDATE

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun UserPermissionRowScreenshot() {
    UserPermissionRow(permission = userPermission())
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun UserPermissionRowOwnerScreenshot() {
    UserPermissionRow(permission = userPermission(permission = OWNER))
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun UserPermissionRowSuspendedScreenshot() {
    UserPermissionRow(
        permission =
            userPermission(
                permission = UPDATE,
                isDisabled = true,
            ),
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun UserPermissionRowLongNameScreenshot() {
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

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun UserPermissionRowDarkThemeScreenshot() {
    UserPermissionRow(permission = userPermission(permission = OWNER))
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun UserPermissionRowSuspendedDarkThemeScreenshot() {
    UserPermissionRow(permission = userPermission(isDisabled = true))
}
