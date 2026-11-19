package cl.lovesea.mareas.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.lovesea.mareas.verdict.Level
import cl.lovesea.mareas.verdict.Sport
import java.time.Duration
import java.time.Instant

@Composable
fun SportPicker(sport: Sport, onSport: (Sport) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Sport.entries.forEach { s ->
            val on = s == sport
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Color.White else NavySoft)
                    .clickable { onSport(s) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    s.label,
                    color = if (on) Navy else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
fun LevelDot(level: Level, size: Int = 12) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(level.color()))
}

@Composable
fun EmergencyButton() {
    val ctx = LocalContext.current
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Level.RED.color())
            .clickable { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:137"))) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Default.Call, contentDescription = "Llamar a emergencias marítimas", tint = Color.White, modifier = Modifier.size(16.dp))
        Text("137", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

fun ageText(at: Instant?, loading: Boolean, now: Instant = Instant.now()): String {
    if (at == null) return if (loading) "Cargando pronóstico…" else "Sin conexión: solo marea"
    val m = Duration.between(at, now).toMinutes()
    val age = when {
        m < 2 -> "recién actualizado"
        m < 60 -> "datos de hace $m min"
        else -> "datos de hace ${m / 60} h"
    }
    return if (loading) "$age · actualizando…" else age.replaceFirstChar { it.uppercase() }
}

@Composable
fun OfflineBanner(forecast: cl.lovesea.mareas.conditions.ForecastRepository.Result?, loading: Boolean) {
    val show = !loading && (forecast == null || forecast.offline)
    if (!show) return
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFFFF3C4)).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (forecast == null) "Sin internet. Conéctate para ver viento y olas."
            else "Sin internet. Mostrando datos guardados; revisa el mar antes de entrar.",
            color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}
