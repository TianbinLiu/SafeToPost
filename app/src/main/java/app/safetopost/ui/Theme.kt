package app.safetopost.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF14212B)
val Accent = Color(0xFFB5532F)
val Sand = Color(0xFFF6F1E7)
val Muted = Color(0xFF6F767E)
val Line = Color(0xFFE4DCCD)
val HighRisk = Color(0xFFB3261E)
val MediumRisk = Color(0xFFB07A1E)
val LowRisk = Color(0xFF2E7D5B)

fun riskColor(level: app.safetopost.model.RiskLevel): Color = when (level) {
    app.safetopost.model.RiskLevel.HIGH -> HighRisk
    app.safetopost.model.RiskLevel.MEDIUM -> MediumRisk
    app.safetopost.model.RiskLevel.LOW -> LowRisk
}

fun riskColor(risk: app.safetopost.model.Risk): Color = when (risk) {
    app.safetopost.model.Risk.HIGH -> HighRisk
    app.safetopost.model.Risk.MEDIUM -> MediumRisk
    app.safetopost.model.Risk.LOW -> Muted
}

private val scheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DDD4),
    onPrimaryContainer = Color(0xFF4A1B0B),
    secondary = Ink,
    onSecondary = Color.White,
    secondaryContainer = Sand,
    onSecondaryContainer = Ink,
    background = Color(0xFFFBF8F2),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = HighRisk
)

private val typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        letterSpacing = 0.9.sp
    )
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SafeToPostTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
