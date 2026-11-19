package cl.lovesea.mareas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.lovesea.mareas.conditions.ForecastRepository
import cl.lovesea.mareas.data.Beach
import cl.lovesea.mareas.tide.TideTable
import cl.lovesea.mareas.tide.Trend
import cl.lovesea.mareas.verdict.Level
import cl.lovesea.mareas.verdict.Sport
import cl.lovesea.mareas.verdict.assess
import cl.lovesea.mareas.verdict.atShore
import cl.lovesea.mareas.verdict.compass
import cl.lovesea.mareas.verdict.score
import cl.lovesea.mareas.verdict.waveWord
import cl.lovesea.mareas.verdict.windWord
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun BeachScreen(
    beach: Beach,
    table: TideTable,
    forecast: ForecastRepository.Result?,
    loading: Boolean,
    sport: Sport,
    onSport: (Sport) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    val now = Instant.now()
    val bf = forecast?.byBeach?.get(beach.id)
    val c = bf?.at(now)?.atShore(beach.seaBearing, beach.exposure)
    val verdict = assess(sport, c, beach.seaBearing)
    val tideNow = table.now(now)

    Column(Modifier.fillMaxSize().background(Sand)) {
        // Encabezado
        Column(Modifier.fillMaxWidth().background(Navy).statusBarsPadding().padding(bottom = 14.dp)) {
            Row(Modifier.padding(end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(beach.name, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                    Text("${beach.comuna} · ${ageText(forecast?.fetchedAt, loading).lowercase()}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                EmergencyButton()
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                }
            }
            SportPicker(sport, onSport, Modifier.padding(start = 16.dp, top = 8.dp))
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            VerdictCard(sport, verdict.level, verdict.headline, verdict.reason)

            // Viento · Olas · Marea
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 14.dp),
            ) {
                Metric(
                    "VIENTO",
                    c?.windKn?.let { "%.0f".format(it) } ?: "–", "kn",
                    c?.windKn?.let { windWord(it) } ?: "Sin datos",
                    listOfNotNull(
                        c?.windFromDeg?.let { "del ${compass(it)}" },
                        c?.gustKn?.let { "rachas ${it.toInt()}" },
                    ).joinToString(" · "),
                    Modifier.weight(1f),
                )
                Metric(
                    "OLAS",
                    c?.waveM?.let { "%.1f".format(it).replace('.', ',') } ?: "–", "m",
                    c?.waveM?.let { waveWord(it) } ?: "Sin datos",
                    listOfNotNull(
                        c?.wavePeriodS?.let { "cada ${it.toInt()} s" },
                        c?.offshoreWaveM?.let { "afuera " + "%.1f".format(it).replace('.', ',') + " m" },
                    ).joinToString(" · "),
                    Modifier.weight(1f),
                )
                Metric(
                    "MAREA",
                    tideNow?.let { "%.1f".format(it.heightM).replace('.', ',') } ?: "–", "m",
                    tideNow?.let { if (it.trend == Trend.RISING) "Subiendo" else "Bajando" } ?: "Sin datos",
                    tideNow?.let { "${if (it.next.isHigh) "alta" else "baja"} a las ${it.next.instant.hhmm()}" } ?: "",
                    Modifier.weight(1f),
                )
            }

            if (bf != null) HoursStrip(bf, sport, beach, now)

            Card {
                Text("¿POR QUÉ?", style = Eyebrow)
                Spacer(Modifier.height(4.dp))
                if (verdict.factors.isEmpty()) {
                    Text("Aún no hay pronóstico de viento y olas. Revisa tu conexión.", color = Muted)
                }
                verdict.factors.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(color = Line)
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        LevelDot(f.level)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.title, fontWeight = FontWeight.Bold, color = Ink)
                            Text(f.detail, color = Muted, fontSize = 13.sp)
                        }
                        Text(f.value, fontWeight = FontWeight.Bold, color = Ink)
                    }
                }
                bf?.daylightOn(LocalDate.now(CHILE))?.let { d ->
                    HorizontalDivider(color = Line)
                    Row(Modifier.padding(vertical = 10.dp)) {
                        Text("Sol", fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                        Text("${d.sunrise.hhmm()} – ${d.sunset.hhmm()}", color = Ink)
                    }
                }
            }

            TideSection(table)

            Text(
                "Estimación automática con pronóstico de Open-Meteo y marea calculada para Valparaíso. " +
                    "No reemplaza los avisos de la Autoridad Marítima: revisa siempre los avisos de marejadas " +
                    "de la Armada y las banderas de la playa.",
                color = Muted, fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun VerdictCard(sport: Sport, level: Level, headline: String, reason: String) {
    val fg = level.onColor()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(level.color()).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Flag, contentDescription = null, tint = fg, modifier = Modifier.size(52.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(sport.question, color = fg.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            Text(headline, color = fg, fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp)
            if (reason.isNotBlank()) Text(reason, color = fg, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String, word: String, sub: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = Eyebrow)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
            Text(" $unit", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(bottom = 5.dp))
        }
        Text(word, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
        if (sub.isNotBlank()) Text(sub, color = Muted, fontSize = 12.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun HoursStrip(
    bf: cl.lovesea.mareas.conditions.BeachForecast,
    sport: Sport,
    beach: Beach,
    now: Instant,
) {
    val start = now.truncatedTo(ChronoUnit.HOURS)
    val hours = bf.hours.filter { !it.time.isBefore(start) }.take(24)
    if (hours.isEmpty()) return
    val verdicts = hours.map { it.atShore(beach.seaBearing, beach.exposure) }.map { it to assess(sport, it, beach.seaBearing) }
    val today = LocalDate.now(CHILE)
    val best = verdicts
        .filter { (h, _) -> h.time.atZone(CHILE).toLocalDate() == today && h.isDay != false }
        .minByOrNull { score(it.second) }

    Card {
        if (best != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelDot(best.second.level, 14)
                Spacer(Modifier.width(10.dp))
                Text("Mejor hora hoy: ", color = Ink)
                Text(best.first.time.hhmm(), fontWeight = FontWeight.Black, color = Ink)
                Text(" · ${best.second.headline}", color = Muted)
            }
        } else {
            Text("Hoy ya no quedan horas de luz", color = Ink)
        }
        Spacer(Modifier.height(12.dp))
        Text("PRÓXIMAS HORAS", style = Eyebrow)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(verdicts) { (h, v) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                    Text(h.time.atZone(CHILE).hour.toString().padStart(2, '0'), fontSize = 12.sp, color = Muted)
                    Box(
                        Modifier.padding(vertical = 4.dp).size(width = 34.dp, height = 28.dp)
                            .clip(RoundedCornerShape(8.dp)).background(v.level.color())
                            .then(if (h.isDay == false) Modifier.border(2.dp, Navy, RoundedCornerShape(8.dp)) else Modifier),
                    )
                    Text(h.waveM?.let { "%.1f".format(it).replace('.', ',') } ?: "–", fontSize = 11.sp, color = Ink)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Altura de ola en la orilla, en metros.", fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun TideSection(table: TideTable) {
    val today = remember { LocalDate.now(CHILE) }
    var dayOffset by rememberSaveable { mutableLongStateOf(0L) }
    val date = today.plusDays(dayOffset)
    val events = remember(date, table) { table.eventsOn(date, CHILE) }
    val curve = remember(date, table) { table.curveOn(date, CHILE) }

    Card {
        Text("MAREA", style = Eyebrow)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { dayOffset-- }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior")
            }
            Text(date.longEs(), fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            if (dayOffset != 0L) TextButton(onClick = { dayOffset = 0 }) { Text("Hoy") }
            IconButton(onClick = { dayOffset++ }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Día siguiente")
            }
        }
        if (curve.isEmpty()) {
            Text("No hay datos de marea para este día.", color = Muted)
        } else {
            TideChart(curve, if (dayOffset == 0L) Instant.now() else null)
            Spacer(Modifier.height(10.dp))
            events.forEach { e ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text(if (e.isHigh) "Pleamar" else "Bajamar", fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    Text(e.instant.hhmm(), color = Ink, modifier = Modifier.weight(1f))
                    Text(e.heightM.meters(), color = Ink)
                }
            }
        }
    }
}

@Composable
fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp)) {
        content()
    }
}
