package app.xrtm8.mada.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GoldPrimary = Color(0xFFE5C07B)
val GoldSecondary = Color(0xFFD19A66)
val DarkBackground = Color(0xFF0C0D12)
val DarkSurface = Color(0xFF161821)
val DarkCard = Color(0xFF1F222E)
val TextLight = Color(0xFFF0F0F5)
val TextMuted = Color(0xFF8F93A3)

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = TextLight
)

@Composable
fun MadaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
