package net.svaroh.passly.core.ui.sharedwith

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import net.svaroh.passly.core.ui.screenshot.groupPermission
import net.svaroh.passly.core.ui.screenshot.userPermission
import net.svaroh.passly.ui.PermissionModelUi

private const val CONTAINER_WIDTH_DP = 240

private val fitting: List<PermissionModelUi> =
    listOf(
        groupPermission(id = "g1", groupName = "Engineering"),
        userPermission(id = "u1"),
        userPermission(id = "u2", firstName = "Grace", lastName = "Hopper"),
        userPermission(id = "u3", firstName = "Alan", lastName = "Turing"),
    )

private fun overflowing(count: Int): List<PermissionModelUi> =
    listOf(groupPermission(id = "g1", groupName = "Engineering")) +
        List(count - 1) { userPermission(id = "u$it") }

@PreviewTest
@Preview(showBackground = true, widthDp = CONTAINER_WIDTH_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SharedWithSectionFittingScreenshot() {
    SharedWithSection(permissions = fitting)
}

@PreviewTest
@Preview(showBackground = true, widthDp = CONTAINER_WIDTH_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SharedWithSectionOverflowingScreenshot() {
    SharedWithSection(permissions = overflowing(15))
}

@PreviewTest
@Preview(showBackground = true, widthDp = CONTAINER_WIDTH_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SharedWithSectionCounterCappedScreenshot() {
    SharedWithSection(permissions = overflowing(120))
}

@PreviewTest
@Preview(showBackground = true, widthDp = CONTAINER_WIDTH_DP, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SharedWithSectionDarkThemeScreenshot() {
    SharedWithSection(permissions = overflowing(15))
}

@PreviewTest
@Preview(showBackground = true, widthDp = CONTAINER_WIDTH_DP, fontScale = 1.5f)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun SharedWithSectionLargeFontScreenshot() {
    SharedWithSection(permissions = overflowing(15))
}
