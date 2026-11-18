package cl.lovesea.mareas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import cl.lovesea.mareas.data.Beaches
import cl.lovesea.mareas.tide.TideTable
import cl.lovesea.mareas.tide.Trend
import cl.lovesea.mareas.verdict.Sport
import cl.lovesea.mareas.verdict.assess
import java.text.Normalizer
import java.time.Instant

private fun String.fold(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

@Composable
fun HomeScreen(
    table: TideTable,
    forecast: ForecastRepository.Result?,
    loading: Boolean,
    sport: Sport,
    onSport: (Sport) -> Unit,
    onRefresh: () -> Unit,
    onOpen: (Beach) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val now = Instant.now()
    val tideNow = table.now(now)
    val shown = Beaches.all.filter {
        query.isBlank() || it.name.fold().contains(query.fold()) || it.comuna.fold().contains(query.fold())
    }

    Column(Modifier.fillMaxSize().background(Sand)) {
        Column(
            Modifier.fillMaxWidth().background(Navy).statusBarsPadding()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lovesea", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text("¿Se puede entrar al mar hoy? · V región", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                EmergencyButton()
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                }
            }
            Spacer(Modifier.height(14.dp))
            SportPicker(sport, onSport)
            Spacer(Modifier.height(12.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Buscar playa o comuna") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            )
        }

        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(ageText(forecast?.fetchedAt, loading), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            tideNow?.let {
                Text(
                    "Marea ${if (it.trend == Trend.RISING) "subiendo" else "bajando"} · ${it.heightM.meters()}",
                    color = Muted, fontSize = 12.sp,
                )
            }
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shown, key = { it.id }) { beach ->
                val c = forecast?.byBeach?.get(beach.id)?.at(now)
                val v = assess(sport, c, beach.seaBearing)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .clickable { onOpen(beach) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.width(6.dp).height(48.dp).clip(RoundedCornerShape(3.dp)).background(v.level.color()),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(beach.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                        val bits = listOfNotNull(
                            beach.comuna,
                            c?.waveM?.let { "olas ${"%.1f".format(it).replace('.', ',')} m" },
                            c?.windKn?.let { "viento ${it.toInt()} kn" },
                        )
                        Text(bits.joinToString(" · "), color = Muted, fontSize = 13.sp)
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(v.level.color())
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(v.headline, color = v.level.onColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
