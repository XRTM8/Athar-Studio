package app.xrtm8.athar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pure Deep Canvas & Studio Surfaces (Linear & Apple Pro Studio Caliber)
val AmoledBlack = Color(0xFF000000)
val StudioBackground = Color(0xFF0A0B0E)      // Deep studio background
val StudioSurface = Color(0xFF14151B)         // Card & panel surface
val StudioSurfaceElevated = Color(0xFF1E2028)    // Interactive / hover fill
val StudioSurfaceActive = Color(0xFF282A36)      // Selected item fill

// Typography Hierarchy (Calibrated optical contrast)
val PureWhite = Color(0xFFF5F6F8)             // Primary text & key elements
val MutedWhite = Color(0xFFA1A3AF)            // Secondary labels & descriptions
val SubtleGray = Color(0xFF636674)            // Tertiary, hints & inactive icons

// Hairline Optical Boundaries (Clean, non-distracting 8%-14% separators)
val StudioBorder = Color(0x14FFFFFF)          // Crisp hairline border (8%)
val StudioBorderSubtle = Color(0x24FFFFFF)    // Interactive container outline (14%)
val StudioBorderActive = Color(0x66E5C07B)    // Restrained gold focus outline

// Refined Luxury Accent (Champagne Gold - Restrained & Dignified)
val GoldAccent = Color(0xFFE5C07B)
val GoldWarm = Color(0xFFD49438)
val GoldMuted = Color(0x26E5C07B)             // 15% gold tint for active chips/tabs

// Backward-compatible surface & glass tokens
val GlassBackground = Color(0xD90A0B0E)
val GlassSurface = StudioSurface
val GlassCard = StudioSurface
val GlassSurfaceHover = StudioSurfaceElevated
val GlassBorder = StudioBorder
val GlassBorderActive = StudioBorderActive

private val AtharColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = GoldWarm,
    background = AmoledBlack,
    surface = StudioSurface,
    surfaceVariant = StudioSurfaceElevated,
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
