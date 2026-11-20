package app.xrtm8.athar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pure True AMOLED Black
val AmoledBlack = Color(0xFF000000)

// Luxury Accents
val GoldAccent = Color(0xFFE5C07B)
val GoldWarm = Color(0xFFD49438)
val PureWhite = Color(0xFFFFFFFF)
val MutedWhite = Color(0xFFB8BAC6)
val SubtleGray = Color(0xFF6B6E7E)

// Frosted Glass & Surface Tokens
val GlassBackground = Color(0xB308090D)   // Translucent deep backdrop
val GlassSurface = Color(0x40252836)      // Frosted card fill
val GlassSurfaceHover = Color(0x6633374A) // Active frosted fill
val GlassBorder = Color(0x33FFFFFF)       // Crisp glass edge highlight
val GlassBorderActive = Color(0x80E5C07B) // Gold highlighted glass edge

private val AtharColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = GoldWarm,
    background = AmoledBlack,
    surface = GlassSurface,
    surfaceVariant = GlassSurfaceHover,
    onPrimary = AmoledBlack,
    onSecondary = AmoledBlack,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onSurfaceVariant = PureWhite
)

@Composable
fun AtharTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtharColorScheme,
        content = content
    )
}
