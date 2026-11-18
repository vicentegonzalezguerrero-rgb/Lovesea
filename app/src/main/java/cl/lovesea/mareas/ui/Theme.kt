package cl.lovesea.mareas.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cl.lovesea.mareas.verdict.Level

val Navy = Color(0xFF0E2A47)
val NavySoft = Color(0xFF1C4166)
val Teal = Color(0xFF1B8A8F)
val Sand = Color(0xFFF5F2EC)
val Ink = Color(0xFF13263A)
val Muted = Color(0xFF5F6F80)
val Line = Color(0xFFE2DDD3)

fun Level.color(): Color = when (this) {
    Level.GREEN -> Color(0xFF2E9E5B)
    Level.YELLOW -> Color(0xFFF2C230)
    Level.ORANGE -> Color(0xFFEF8423)
    Level.RED -> Color(0xFFD93A3A)
    Level.UNKNOWN -> Color(0xFF9AA5B1)
}

fun Level.onColor(): Color = if (this == Level.YELLOW) Navy else Color.White

private val type = Typography().run {
    copy(
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Black),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    )
}

val Eyebrow = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Muted)

@Composable
fun LoveseaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            onPrimary = Color.White,
            secondary = Teal,
            background = Sand,
            surface = Color.White,
            onSurface = Ink,
            onBackground = Ink,
            surfaceVariant = Color(0xFFEDE8DF),
            outlineVariant = Line,
        ),
        typography = type,
        content = content,
    )
}
