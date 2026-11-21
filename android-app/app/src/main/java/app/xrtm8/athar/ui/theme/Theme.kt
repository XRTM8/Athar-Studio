package app.xrtm8.athar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. PURE AMOLED SURFACES (Dark & White)
// ==========================================

// AMOLED Black Palette
val AmoledBlack = Color(0xFF000000)
val StudioBackground = Color(0xFF0A0B0E)          // Deep studio background
val StudioSurface = Color(0xFF14151B)             // Card & panel surface
val StudioSurfaceElevated = Color(0xFF1E2028)        // Interactive / hover fill
val StudioSurfaceActive = Color(0xFF282A36)          // Selected item fill

// AMOLED White Palette (Pure high-contrast crisp light mode)
val AmoledWhite = Color(0xFFFFFFFF)
val WhiteSurface = Color(0xFFF6F7F9)
val WhiteSurfaceElevated = Color(0xFFECEEF2)
val WhiteSurfaceActive = Color(0xFFDFE2E8)

// ==========================================
// 2. TYPOGRAPHY HIERARCHY
// ==========================================

// Light text (for dark mode)
val PureWhite = Color(0xFFF5F6F8)                 // Primary text & key elements
val MutedWhite = Color(0xFFA1A3AF)                // Secondary labels & descriptions
val SubtleGray = Color(0xFF636674)                // Tertiary, hints & inactive icons

// Dark text (for AMOLED White mode)
val DarkText = Color(0xFF0F1014)                  // Primary dark text
val DarkTextMuted = Color(0xFF5A606E)             // Secondary dark labels
val DarkTextSubtle = Color(0xFF8B92A0)            // Tertiary hints

// ==========================================
// 3. CHERRY RED LUXURY ACCENT
// ==========================================

// Rich Cherry / Crimson Palette (Replacing Gold/Yellow)
val CherryAccent = Color(0xFFE02447)              // Vibrant cherry red
val CherryWarm = Color(0xFFD81B60)                // Velvet crimson cherry
val CherryDeep = Color(0xFF9C1030)                // Deep imperial cherry
val CherryMuted = Color(0x26E02447)               // 15% cherry tint for active chips/tabs
val CherryBorderActive = Color(0x66E02447)        // Cherry focus outline

// Backward-compatible aliases mapped 1:1 to Cherry
val GoldAccent = CherryAccent
val GoldWarm = CherryWarm
val GoldMuted = CherryMuted

// ==========================================
// 4. BORDERS & GLASS TOKENS
// ==========================================

// Dark theme borders
val StudioBorder = Color(0x14FFFFFF)              // Crisp hairline border (8%)
val StudioBorderSubtle = Color(0x24FFFFFF)        // Interactive container outline (14%)
val StudioBorderActive = CherryBorderActive

// White theme borders
val WhiteBorder = Color(0x1F000000)               // Hairline dark border on white
val WhiteBorderSubtle = Color(0x14000000)
val WhiteBorderActive = CherryBorderActive

// Glass surface tokens
val GlassBackground = Color(0xD90A0B0E)
val GlassBackgroundWhite = Color(0xF4FFFFFF)
val GlassSurface = StudioSurface
val GlassCard = StudioSurface
val GlassSurfaceHover = StudioSurfaceElevated
val GlassBorder = StudioBorder
val GlassBorderActive = CherryBorderActive

// CompositionLocal for immediate reactive access across composables
val LocalAmoledWhite = staticCompositionLocalOf { false }

// ==========================================
// 5. COLOR SCHEMES & THEME WRAPPER
// ==========================================

private val DarkAtharColorScheme = darkColorScheme(
    primary = CherryAccent,
    secondary = CherryWarm,
    background = AmoledBlack,
    surface = StudioSurface,
    surfaceVariant = StudioSurfaceElevated,
    onPrimary = AmoledBlack,
    onSecondary = AmoledBlack,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onSurfaceVariant = PureWhite
)

private val LightAtharColorScheme = lightColorScheme(
    primary = CherryWarm,
    secondary = CherryDeep,
    background = AmoledWhite,
    surface = WhiteSurface,
    surfaceVariant = WhiteSurfaceElevated,
    onPrimary = AmoledWhite,
    onSecondary = AmoledWhite,
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = DarkText
)

@Composable
fun AtharTheme(
    isAmoledWhite: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAmoledWhite provides isAmoledWhite) {
        MaterialTheme(
            colorScheme = if (isAmoledWhite) LightAtharColorScheme else DarkAtharColorScheme,
            content = content
        )
    }
}
