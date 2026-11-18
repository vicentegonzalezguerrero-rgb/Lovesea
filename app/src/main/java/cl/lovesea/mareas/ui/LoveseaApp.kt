package cl.lovesea.mareas.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cl.lovesea.mareas.conditions.ForecastRepository
import cl.lovesea.mareas.data.Beaches
import cl.lovesea.mareas.tide.TideTable
import cl.lovesea.mareas.verdict.Sport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LoveseaApp(table: TideTable) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("lovesea", Context.MODE_PRIVATE) }
    val repo = remember { ForecastRepository(ctx) }

    var sport by remember {
        mutableStateOf(runCatching { Sport.valueOf(prefs.getString("sport", "SUP")!!) }.getOrDefault(Sport.SUP))
    }
    val setSport: (Sport) -> Unit = {
        sport = it
        prefs.edit().putString("sport", it.name).apply()
    }

    var forecast by remember { mutableStateOf<ForecastRepository.Result?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        loading = true
        val r = withContext(Dispatchers.IO) { repo.load(Beaches.all, maxAgeMinutes = if (reload == 0) 60 else 0) }
        if (r != null) forecast = r
        loading = false
    }

    LoveseaTheme {
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        val beach = selectedId?.let { Beaches.byId(it) }
        if (beach == null) {
            HomeScreen(
                table = table,
                forecast = forecast,
                loading = loading,
                sport = sport,
                onSport = setSport,
                onRefresh = { reload++ },
                onOpen = { selectedId = it.id },
            )
        } else {
            BackHandler { selectedId = null }
            BeachScreen(
                beach = beach,
                table = table.shifted(beach.offsetMinutes * 60L),
                forecast = forecast,
                loading = loading,
                sport = sport,
                onSport = setSport,
                onRefresh = { reload++ },
                onBack = { selectedId = null },
            )
        }
    }
}
