package net.svaroh.passly.core.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun PassboltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) PassboltColors.DarkColorScheme else PassboltColors.LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
