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

enum class AppPalette(
    val label: String,
    val accent: Color,
    val background: Color,
    val ink: Color,
    val sand: Color,
    val muted: Color,
    val line: Color
) {
    EMBER(
        label = "Ember",
        accent = Color(0xFFB5532F),
        background = Color(0xFFFBF8F2),
        ink = Color(0xFF14212B),
        sand = Color(0xFFF6F1E7),
        muted = Color(0xFF6F767E),
        line = Color(0xFFE4DCCD)
    ),
    FOREST(
        label = "Forest",
        accent = Color(0xFF2F6B4F),
        background = Color(0xFFF5FAF4),
        ink = Color(0xFF12251B),
        sand = Color(0xFFE7F0E5),
        muted = Color(0xFF5F7267),
        line = Color(0xFFD3E0D2)
    ),
    OCEAN(
        label = "Ocean",
        accent = Color(0xFF2F5D8A),
        background = Color(0xFFF4F8FC),
        ink = Color(0xFF0F2132),
        sand = Color(0xFFE4EEF8),
        muted = Color(0xFF5C6E7E),
        line = Color(0xFFD2DFEC)
    ),
    PLUM(
        label = "Plum",
        accent = Color(0xFF7A3E6B),
        background = Color(0xFFFBF6FA),
        ink = Color(0xFF241423),
        sand = Color(0xFFF2E6EF),
        muted = Color(0xFF6F5F6C),
        line = Color(0xFFE1D2DC)
    );

    val surface: Color get() = Color.White
}

val HighRisk = Color(0xFFB3261E)
val MediumRisk = Color(0xFFB07A1E)
val LowRisk = Color(0xFF2E7D5B)

fun riskColor(level: app.safetopost.model.RiskLevel): Color = when (level) {
    app.safetopost.model.RiskLevel.HIGH -> HighRisk
    app.safetopost.model.RiskLevel.MEDIUM -> MediumRisk
    app.safetopost.model.RiskLevel.LOW -> LowRisk
}

@Composable
fun riskColor(risk: app.safetopost.model.Risk): Color = when (risk) {
    app.safetopost.model.Risk.HIGH -> HighRisk
    app.safetopost.model.Risk.MEDIUM -> MediumRisk
    app.safetopost.model.Risk.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
}

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
fun SafeToPostTheme(palette: AppPalette = AppPalette.EMBER, content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = palette.accent,
        onPrimary = Color.White,
        primaryContainer = palette.sand,
        onPrimaryContainer = palette.ink,
        secondary = palette.ink,
        onSecondary = Color.White,
        secondaryContainer = palette.sand,
        onSecondaryContainer = palette.ink,
        background = palette.background,
        onBackground = palette.ink,
        surface = palette.surface,
        onSurface = palette.ink,
        surfaceVariant = palette.sand,
        onSurfaceVariant = palette.muted,
        outline = palette.line,
        outlineVariant = palette.line,
        error = HighRisk
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
