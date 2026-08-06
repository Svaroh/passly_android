package com.passbolt.mobile.android.core.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.PassboltTheme

private const val DEFAULT_CONTENT_PADDING_DP = 16

@Composable
internal fun ScreenshotContainer(
    isDarkTheme: Boolean = false,
    isRtl: Boolean = false,
    contentPadding: Dp = DEFAULT_CONTENT_PADDING_DP.dp,
    content: @Composable () -> Unit,
) {
    PassboltTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.padding(contentPadding)) {
                    content()
                }
            }
        }
    }
}
