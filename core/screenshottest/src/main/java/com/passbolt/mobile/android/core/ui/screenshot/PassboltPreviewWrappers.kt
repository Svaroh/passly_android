/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

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
