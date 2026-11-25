package app.safetopost.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF1A2A3A)
val Accent = Color(0xFFB5532F)
val Sand = Color(0xFFF4F0E8)
val Muted = Color(0xFF6B7280)

private val scheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Ink,
    onSecondary = Color.White,
    background = Color(0xFFFAF8F3),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = Ink.copy(alpha = 0.78f),
    error = Color(0xFFB3261E)
)

@Composable
fun SafeToPostTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
