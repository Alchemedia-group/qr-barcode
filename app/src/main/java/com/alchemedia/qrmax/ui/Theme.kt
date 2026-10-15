package com.alchemedia.qrmax.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The same palette as the web demo: cool paper grey, one ultramarine accent, a coral scan laser. */
data class QrColors(
    val bg: Color, val surface: Color, val surface2: Color, val ink: Color, val muted: Color, val line: Color,
    val accent: Color, val accentInk: Color, val accentSoft: Color, val danger: Color, val ok: Color, val dark: Boolean,
)

val LightQr = QrColors(Color(0xFFE9EDF6), Color.White, Color(0xFFF3F5FA), Color(0xFF0E1626), Color(0xFF65748B), Color(0xFFE3E8F1),
    Color(0xFF3A5BFF), Color.White, Color(0xFFE8EDFF), Color(0xFFD92D20), Color(0xFF12B76A), false)
val DarkQr = QrColors(Color(0xFF060A12), Color(0xFF111827), Color(0xFF19223A), Color(0xFFEEF2FB), Color(0xFF8B99B2), Color(0xFF212C45),
    Color(0xFF6F8CFF), Color(0xFF0A1020), Color(0xFF1B2850), Color(0xFFFF8A7D), Color(0xFF35C98A), true)

val LocalQr = staticCompositionLocalOf { LightQr }
val Laser = Color(0xFFFF5A4D)
val Gold1 = Color(0xFFFFCF5A)
val Gold2 = Color(0xFFFF9A3D)
val GoldInk = Color(0xFF2B1700)
val BrandInk = Color(0xFF0E1626)

@Composable
fun QrTheme(content: @Composable () -> Unit) {
    val c = if (isSystemInDarkTheme()) DarkQr else LightQr
    val scheme = if (c.dark) darkColorScheme(primary = c.accent, onPrimary = c.accentInk, background = c.bg, surface = c.surface, onSurface = c.ink, onBackground = c.ink, surfaceVariant = c.surface2, outline = c.line)
    else lightColorScheme(primary = c.accent, onPrimary = c.accentInk, background = c.bg, surface = c.surface, onSurface = c.ink, onBackground = c.ink, surfaceVariant = c.surface2, outline = c.line)
    MaterialTheme(colorScheme = scheme) { CompositionLocalProvider(LocalQr provides c, content = content) }
}

/** A hue per code type, used only to tint icons. */
fun hueColor(hue: Int, dark: Boolean): Color = Color.hsl(hue.toFloat(), if (dark) 0.90f else 0.70f, if (dark) 0.72f else 0.45f)
