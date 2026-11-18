package cl.lovesea.mareas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.lovesea.mareas.data.Beach
import cl.lovesea.mareas.tide.TideTable
import cl.lovesea.mareas.tide.Trend
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeachDetailScreen(beach: Beach, table: TideTable, onBack: () -> Unit) {
    val today = remember { LocalDate.now(CHILE) }
    var dayOffset by rememberSaveable { mutableStateOf(0L) }
    val date = today.plusDays(dayOffset)
    val now = remember { Instant.now() }
    val events = remember(date, table) { table.eventsOn(date, CHILE) }
    val curve = remember(date, table) { table.curveOn(date, CHILE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(beach.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(beach.comuna, style = MaterialTheme.typography.titleMedium)

            if (dayOffset == 0L) table.now(now)?.let { t ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ahora", style = MaterialTheme.typography.labelLarge)
                        Text(t.heightM.meters(), style = MaterialTheme.typography.displaySmall)
                        val verb = if (t.trend == Trend.RISING) "Subiendo" else "Bajando"
                        val next = if (t.next.isHigh) "pleamar" else "bajamar"
                        Text("$verb. Próxima $next a las ${t.next.instant.hhmm()} (${t.next.heightM.meters()}).")
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { dayOffset-- }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior")
                }
                Text(
                    date.longEs(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (dayOffset != 0L) TextButton(onClick = { dayOffset = 0 }) { Text("Hoy") }
                IconButton(onClick = { dayOffset++ }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Día siguiente")
                }
            }

            if (curve.isEmpty()) {
                Text("No hay datos de marea para este día.")
            } else {
                TideChart(curve, if (dayOffset == 0L) now else null)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        events.forEach { e ->
                            Row {
                                Text(
                                    if (e.isHigh) "Pleamar" else "Bajamar",
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(e.instant.hhmm(), modifier = Modifier.weight(1f))
                                Text(e.heightM.meters())
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Predicción astronómica para Valparaíso calculada a partir de datos del mareógrafo " +
                    "(UHSLC). Alturas sobre el nivel mínimo de marea. No considera marejadas ni viento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TideChart(curve: List<Pair<Instant, Double>>, now: Instant?) {
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val marker = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val t0 = curve.first().first.epochSecond.toFloat()
    val t1 = curve.last().first.epochSecond.toFloat()
    val maxH = maxOf(2.0, curve.maxOf { it.second }).toFloat()

    Column {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            fun x(sec: Long) = (sec - t0) / (t1 - t0) * size.width
            fun y(h: Double) = size.height - (h.toFloat() / maxH) * size.height

            for (i in 1..3) {
                val gx = size.width * i / 4
                drawLine(grid, Offset(gx, 0f), Offset(gx, size.height),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
            }
            val path = Path()
            curve.forEachIndexed { i, (t, h) ->
                if (i == 0) path.moveTo(x(t.epochSecond), y(h)) else path.lineTo(x(t.epochSecond), y(h))
            }
            val area = Path().apply {
                addPath(path)
                lineTo(x(curve.last().first.epochSecond), size.height)
                lineTo(x(curve.first().first.epochSecond), size.height)
                close()
            }
            drawPath(area, fill)
            drawPath(path, line, style = Stroke(width = 3.dp.toPx()))
            if (now != null && now.epochSecond.toFloat() in t0..t1) {
                val nx = x(now.epochSecond)
                drawLine(marker, Offset(nx, 0f), Offset(nx, size.height), strokeWidth = 2.dp.toPx())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
