package app.xrtm8.athar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. XRTM8 OBSIDIAN STUDIO SURFACES
// ==========================================

// AMOLED Dark Palette
val AmoledBlack = Color(0xFF000000)
val StudioBackground = Color(0xFF0A0B0E)
val StudioSurface = Color(0xFF14151B)
val StudioSurfaceElevated = Color(0xFF1E2028)
val StudioSurfaceActive = Color(0xFF282A36)

// AMOLED White Palette
val AmoledWhite = Color(0xFFFFFFFF)
val WhiteSurface = Color(0xFFF6F7F9)
val WhiteSurfaceElevated = Color(0xFFECEEF2)
val WhiteSurfaceActive = Color(0xFFDFE2E8)

// ==========================================
// 2. TYPOGRAPHY HIERARCHY
// ==========================================

// Dark mode text
val PureWhite = Color(0xFFF5F6F8)
val MutedWhite = Color(0xFFA1A3AF)
val SubtleGray = Color(0xFF858999)

// Light mode text
val DarkText = Color(0xFF0F1014)
val DarkTextMuted = Color(0xFF5A606E)
val DarkTextSubtle = Color(0xFF666D7C)

// ==========================================
// 3. VELVET BURGUNDY ACCENT SYSTEM
// ==========================================

val BurgundyAccent = Color(0xFF9E1B42)
val BurgundyInkDark = Color(0xFFE86488)
val BurgundyDeep = Color(0xFF72142B)
val BurgundyWarm = Color(0xFF8B1538)
val BurgundyMuted = Color(0x299E1B42)
val BurgundyMutedLight = Color(0x1A72142B)
val BurgundyBorderActive = Color(0x73E86488)
val BurgundyBorderActiveLight = Color(0x7372142B)

// Semantic Status
val StatusSuccessDark = Color(0xFF30D158)
val StatusSuccessLight = Color(0xFF15803D)
val StatusWarningDark = Color(0xFFF59E0B)
val StatusWarningLight = Color(0xFFB45309)
val StatusDangerDark = Color(0xFFE02447)
val StatusDangerLight = Color(0xFFB91C1C)

// Legacy aliases kept for incremental migration (never use black text on BurgundyAccent)
val CherryAccent = BurgundyAccent
val CherryWarm = BurgundyWarm
val CherryDeep = BurgundyDeep
val CherryMuted = BurgundyMuted
val CherryBorderActive = BurgundyBorderActive

val GoldAccent = BurgundyAccent
val GoldWarm = BurgundyWarm
val GoldMuted = BurgundyMuted

// ==========================================
// 4. BORDERS & SURFACE TOKENS
// ==========================================

val StudioBorder = Color(0x14FFFFFF)
val StudioBorderSubtle = Color(0x24FFFFFF)
val StudioBorderActive = BurgundyBorderActive

val WhiteBorder = Color(0x1A000000)
val WhiteBorderSubtle = Color(0x14000000)
val WhiteBorderActive = BurgundyBorderActiveLight

val GlassBackground = Color(0xD90A0B0E)
val GlassBackgroundWhite = Color(0xF4FFFFFF)
val GlassSurface = StudioSurface
val GlassCard = StudioSurface
val GlassSurfaceHover = StudioSurfaceElevated
val GlassBorder = StudioBorder
val GlassBorderActive = BurgundyBorderActive

@Immutable
data class AtharColors(
    val isLight: Boolean,
    val canvas: Color,
    val recessed: Color,
    val surface: Color,
    val elevated: Color,
    val active: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentFill: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val border: Color,
    val borderActive: Color,
    val success: Color,
    val warning: Color,
    val danger: Color
)

private val DarkAtharPalette = AtharColors(
    isLight = false,
    canvas = AmoledBlack,
    recessed = StudioBackground,
    surface = StudioSurface,
    elevated = StudioSurfaceElevated,
    active = StudioSurfaceActive,
    textPrimary = PureWhite,
    textSecondary = MutedWhite,
    textMuted = SubtleGray,
    accentFill = BurgundyAccent,
    accentInk = BurgundyInkDark,
    accentSoft = BurgundyMuted,
    onAccent = AmoledWhite,
    border = StudioBorder,
    borderActive = BurgundyBorderActive,
    success = StatusSuccessDark,
    warning = StatusWarningDark,
    danger = StatusDangerDark
)

private val LightAtharPalette = AtharColors(
    isLight = true,
    canvas = AmoledWhite,
    recessed = WhiteSurface,
    surface = WhiteSurfaceElevated,
    elevated = WhiteSurfaceActive,
    active = Color(0xFFD5D9E1),
    textPrimary = DarkText,
    textSecondary = DarkTextMuted,
    textMuted = DarkTextSubtle,
    accentFill = BurgundyDeep,
    accentInk = BurgundyDeep,
    accentSoft = BurgundyMutedLight,
    onAccent = AmoledWhite,
    border = WhiteBorder,
    borderActive = BurgundyBorderActiveLight,
    success = StatusSuccessLight,
    warning = StatusWarningLight,
    danger = StatusDangerLight
)

val LocalAmoledWhite = staticCompositionLocalOf { false }
val LocalAtharColors = staticCompositionLocalOf { DarkAtharPalette }

// ==========================================
// 5. COLOR SCHEMES & THEME WRAPPER
// ==========================================

private val DarkAtharColorScheme = darkColorScheme(
    primary = BurgundyAccent,
    onPrimary = AmoledWhite,
    primaryContainer = Color(0xFF330D1A),
    onPrimaryContainer = PureWhite,
    secondary = BurgundyWarm,
    onSecondary = AmoledWhite,
    secondaryContainer = BurgundyMuted,
    onSecondaryContainer = BurgundyInkDark,
    background = AmoledBlack,
    onBackground = PureWhite,
    surface = StudioSurface,
    onSurface = PureWhite,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = MutedWhite,
    surfaceContainerLowest = StudioBackground,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceActive,
    outline = StudioBorder,
    outlineVariant = StudioBorderSubtle,
    error = StatusDangerDark,
    onError = AmoledWhite,
    surfaceTint = Color.Transparent
)

private val LightAtharColorScheme = lightColorScheme(
    primary = BurgundyDeep,
    onPrimary = AmoledWhite,
    primaryContainer = Color(0xFFF4DCE2),
    onPrimaryContainer = Color(0xFF4A0B1B),
    secondary = BurgundyWarm,
    onSecondary = AmoledWhite,
    secondaryContainer = WhiteSurfaceElevated,
    onSecondaryContainer = BurgundyDeep,
    background = AmoledWhite,
    onBackground = DarkText,
    surface = WhiteSurface,
    onSurface = DarkText,
    surfaceVariant = WhiteSurfaceElevated,
    onSurfaceVariant = DarkTextMuted,
    surfaceContainerLowest = AmoledWhite,
    surfaceContainerLow = WhiteSurface,
    surfaceContainer = WhiteSurfaceElevated,
    surfaceContainerHigh = WhiteSurfaceActive,
    outline = WhiteBorder,
    outlineVariant = WhiteBorderSubtle,
    error = StatusDangerLight,
    onError = AmoledWhite,
    surfaceTint = Color.Transparent
)

@Composable
fun AtharTheme(
    isAmoledWhite: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (isAmoledWhite) LightAtharPalette else DarkAtharPalette
    CompositionLocalProvider(
        LocalAmoledWhite provides isAmoledWhite,
        LocalAtharColors provides palette
    ) {
        MaterialTheme(
            colorScheme = if (isAmoledWhite) LightAtharColorScheme else DarkAtharColorScheme,
            typography = AtharTypography,
            shapes = AtharShapes,
            content = content
        )
    }
}
