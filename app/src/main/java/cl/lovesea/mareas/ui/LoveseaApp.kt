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
import androidx.lifecycle.compose.LifecycleResumeEffect
import cl.lovesea.mareas.conditions.ForecastRepository
import cl.lovesea.mareas.data.Beaches
import cl.lovesea.mareas.tide.TideRepository
import cl.lovesea.mareas.verdict.Sport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.Instant

/** Cada cuánto se vuelve a pedir el pronóstico mientras la app está abierta. */
private const val REFRESH_MINUTES = 15L

@Composable
fun LoveseaApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("lovesea", Context.MODE_PRIVATE) }
    val forecastRepo = remember { ForecastRepository(ctx) }
    val tideRepo = remember { TideRepository(ctx) }

    var sport by remember {
        mutableStateOf(runCatching { Sport.valueOf(prefs.getString("sport", "SUP")!!) }.getOrDefault(Sport.SUP))
    }
    val setSport: (Sport) -> Unit = {
        sport = it
        prefs.edit().putString("sport", it.name).apply()
    }

    var table by remember { mutableStateOf(tideRepo.local()) }
    var forecast by remember { mutableStateOf<ForecastRepository.Result?>(null) }
    var loading by remember { mutableStateOf(true) }
    var now by remember { mutableStateOf(Instant.now()) }

    // Cada pedido de actualización incrementa este contador. force = ignora la caché.
    var request by remember { mutableIntStateOf(0) }
    var force by remember { mutableStateOf(false) }
    val refresh: (Boolean) -> Unit = { f ->
        force = f
        request++
    }

    LaunchedEffect(request) {
        loading = true
        val maxAge = if (force) 0L else REFRESH_MINUTES - 1
        val r = withContext(Dispatchers.IO) { forecastRepo.load(Beaches.all, maxAgeMinutes = maxAge) }
        if (r != null) forecast = r
        withContext(Dispatchers.IO) { tideRepo.refresh() }?.let { table = it }
        now = Instant.now()
        loading = false
    }

    // Reloj: recalcula veredicto, marea y "hace X min" cada minuto,
    // y pide datos nuevos cada REFRESH_MINUTES.
    LaunchedEffect(Unit) {
        var minutes = 0L
        while (isActive) {
            delay(60_000)
            now = Instant.now()
            minutes++
            if (minutes % REFRESH_MINUTES == 0L) refresh(false)
        }
    }

    // Al volver a la app (desde otra app o con la pantalla apagada) actualiza si los datos son viejos.
    LifecycleResumeEffect(Unit) {
        now = Instant.now()
        val age = forecast?.fetchedAt?.let { now.epochSecond - it.epochSecond } ?: Long.MAX_VALUE
        if (age > 10 * 60 && !loading) refresh(false)
        onPauseOrDispose { }
    }

    LoveseaTheme {
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        val beach = selectedId?.let { Beaches.byId(it) }
        if (beach == null) {
            HomeScreen(
                table = table,
                forecast = forecast,
                loading = loading,
                now = now,
                sport = sport,
                onSport = setSport,
                onRefresh = { refresh(true) },
                onOpen = { selectedId = it.id },
            )
        } else {
            BackHandler { selectedId = null }
            BeachScreen(
                beach = beach,
                table = table.shifted(beach.offsetMinutes * 60L),
                forecast = forecast,
                loading = loading,
                now = now,
                sport = sport,
                onSport = setSport,
                onRefresh = { refresh(true) },
                onBack = { selectedId = null },
            )
        }
    }
}
