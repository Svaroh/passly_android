package net.svaroh.passly.core.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

@Suppress("MagicNumber")
object PassboltColors {
    val LightColorScheme: ColorScheme =
        lightColorScheme(
            primary = Color(0xFF2A9CEB),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFFFFFFF),
            onBackground = Color(0xFF333333),
            outline = Color(0xFFDDDDDD),
            outlineVariant = Color(0xFFE4E4E4),
        )

    val DarkColorScheme: ColorScheme =
        darkColorScheme(
            primary = Color(0xFF2A9CEB),
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            surfaceVariant = Color(0xFF333333),
            onBackground = Color(0xFFDDDDDD),
            outline = Color(0xFF0f0f0f),
            outlineVariant = Color(0xFF333333),
        )
}
