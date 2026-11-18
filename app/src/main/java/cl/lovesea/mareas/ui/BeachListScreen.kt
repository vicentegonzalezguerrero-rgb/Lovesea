package cl.lovesea.mareas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.lovesea.mareas.data.Beach
import cl.lovesea.mareas.data.Beaches
import cl.lovesea.mareas.tide.TideTable
import cl.lovesea.mareas.tide.Trend
import java.text.Normalizer
import java.time.Instant

private fun String.fold(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeachListScreen(table: TideTable, onOpen: (Beach) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val now = remember { Instant.now() }
    val tideNow = remember(now) { table.now(now) }
    val shown = Beaches.all.filter {
        query.isBlank() || it.name.fold().contains(query.fold()) || it.comuna.fold().contains(query.fold())
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Mareas V región") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Buscar playa o comuna") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (tideNow != null) {
                val verb = if (tideNow.trend == Trend.RISING) "subiendo" else "bajando"
                val next = if (tideNow.next.isHigh) "pleamar" else "bajamar"
                Text(
                    "Ahora la marea está $verb (${tideNow.heightM.meters()}). Próxima $next a las ${tideNow.next.instant.hhmm()}.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            LazyColumn {
                items(shown, key = { it.id }) { beach ->
                    val bt = beach.offsetMinutes.takeIf { it != 0 }?.let { table.shifted(it * 60L).now(now) } ?: tideNow
                    ListItem(
                        headlineContent = { Text(beach.name) },
                        supportingContent = { Text(beach.comuna) },
                        trailingContent = {
                            if (bt != null) Icon(
                                if (bt.trend == Trend.RISING) Icons.AutoMirrored.Filled.TrendingUp
                                else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = if (bt.trend == Trend.RISING) "Subiendo" else "Bajando",
                            )
                        },
                        modifier = Modifier.clickable { onOpen(beach) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
