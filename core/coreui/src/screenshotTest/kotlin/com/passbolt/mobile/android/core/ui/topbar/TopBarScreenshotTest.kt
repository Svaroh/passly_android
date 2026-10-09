package net.svaroh.passly.core.ui.topbar

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.R
import net.svaroh.passly.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import net.svaroh.passly.core.ui.screenshot.PassboltRtlPreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun TitleAppBarScreenshot() {
    TitleAppBar(title = "Settings")
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun TitleAppBarWithBackScreenshot() {
    TitleAppBar(
        title = "App settings",
        navigationIcon = { BackNavigationIcon(onBackClick = {}) },
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun TitleAppBarLongTitleScreenshot() {
    TitleAppBar(
        title = "A very long title that should be truncated with ellipsis",
        navigationIcon = { BackNavigationIcon(onBackClick = {}) },
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun TitleAppBarWithActionScreenshot() {
    TitleAppBar(
        title = "App settings",
        navigationIcon = { BackNavigationIcon(onBackClick = {}) },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = null,
                )
            }
        },
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun TitleAppBarWithRefreshProgressScreenshot() {
    TitleAppBar(
        title = "Passwords",
        refreshProgress = 0.4f,
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltRtlPreviewWrapper::class)
@Composable
fun TitleAppBarRtlScreenshot() {
    TitleAppBar(
        title = "App settings",
        navigationIcon = { BackNavigationIcon(onBackClick = {}) },
    )
}
