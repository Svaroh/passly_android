package net.svaroh.passly.core.ui.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import net.svaroh.passly.core.ui.progressindicator.DataRefreshProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleAppBar(
    modifier: Modifier = Modifier,
    title: String = "",
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    refreshProgress: Float? = null,
) {
    Box(modifier = modifier) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = navigationIcon,
            actions = actions,
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
        )
        if (refreshProgress != null) {
            DataRefreshProgressIndicator(
                progress = refreshProgress,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Preview
@Composable
private fun TitleToolbarPreview() {
    TitleAppBar(
        title = "Title",
        navigationIcon = { BackNavigationIcon(onBackClick = {}) },
    )
}

@Preview
@Composable
private fun TitleToolbarWithRefreshProgressPreview() {
    TitleAppBar(
        title = "Title",
        navigationIcon = { BackNavigationIcon(onBackClick = {}) },
        refreshProgress = 0.4f,
    )
}
