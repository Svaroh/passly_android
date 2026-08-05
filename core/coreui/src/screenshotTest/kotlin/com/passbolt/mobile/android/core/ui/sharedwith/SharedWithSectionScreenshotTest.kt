package com.passbolt.mobile.android.core.ui.sharedwith

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer
import com.passbolt.mobile.android.core.ui.screenshot.groupPermission
import com.passbolt.mobile.android.core.ui.screenshot.userPermission
import com.passbolt.mobile.android.ui.PermissionModelUi

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
@Composable
fun SharedWithSectionFittingScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SharedWithSection(permissions = fitting)
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = CONTAINER_WIDTH_DP)
@Composable
fun SharedWithSectionOverflowingScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SharedWithSection(permissions = overflowing(15))
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = CONTAINER_WIDTH_DP)
@Composable
fun SharedWithSectionCounterCappedScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SharedWithSection(permissions = overflowing(120))
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = CONTAINER_WIDTH_DP, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SharedWithSectionDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true, contentPadding = 0.dp) {
        SharedWithSection(permissions = overflowing(15))
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = CONTAINER_WIDTH_DP, fontScale = 1.5f)
@Composable
fun SharedWithSectionLargeFontScreenshot() {
    ScreenshotContainer(contentPadding = 0.dp) {
        SharedWithSection(permissions = overflowing(15))
    }
}
