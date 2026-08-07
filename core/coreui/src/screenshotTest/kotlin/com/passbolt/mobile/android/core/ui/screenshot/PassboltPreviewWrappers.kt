package com.passbolt.mobile.android.core.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.PassboltTheme

private val PREVIEW_CONTENT_PADDING = 16.dp

/**
 * Wraps a preview in [PassboltTheme], a themed background and content padding.
 *
 * Light / dark comes from the preview configuration - a night mode ui mode makes the theme resolve
 * to dark by default, so the preview body must not hardcode it.
 */
class PassboltPreviewWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        PassboltTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.padding(PREVIEW_CONTENT_PADDING)) {
                    content()
                }
            }
        }
    }
}

/** [PassboltPreviewWrapper] without the content padding, for components rendered edge to edge. */
class PassboltEdgeToEdgePreviewWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        PassboltTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box {
                    content()
                }
            }
        }
    }
}

/** [PassboltEdgeToEdgePreviewWrapper] forced into a right-to-left layout direction. */
class PassboltRtlPreviewWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        PassboltTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box {
                        content()
                    }
                }
            }
        }
    }
}
