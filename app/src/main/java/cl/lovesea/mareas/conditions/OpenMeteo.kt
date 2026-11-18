package cl.lovesea.mareas.conditions

import android.content.Context
import cl.lovesea.mareas.data.Beach
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId

/**
 * Pronóstico de viento (Open-Meteo Forecast) y de olas y temperatura del mar
 * (Open-Meteo Marine) para todas las playas en dos llamadas.
 * Guarda la última respuesta para mostrarla sin conexión.
 */
class ForecastRepository(context: Context) {
    private val cacheDir = File(context.filesDir, "forecast").apply { mkdirs() }
    private val zone = ZoneId.of("America/Santiago")

    data class Result(val byBeach: Map<String, BeachForecast>, val fetchedAt: Instant, val fromCache: Boolean)

    /** Bloqueante: llamar fuera del hilo principal. */
    fun load(beaches: List<Beach>, maxAgeMinutes: Long = 60): Result? {
        val wind = File(cacheDir, "wind.json")
        val marine = File(cacheDir, "marine.json")
        val stamp = File(cacheDir, "stamp")
        val cachedAt = stamp.takeIf { it.exists() }?.readText()?.trim()?.toLongOrNull()?.let(Instant::ofEpochSecond)
        val fresh = cachedAt != null && Instant.now().epochSecond - cachedAt.epochSecond < maxAgeMinutes * 60

        if (!fresh) {
            val ok = runCatching {
                val lat = beaches.joinToString(",") { "%.3f".format(java.util.Locale.US, it.lat) }
                val lon = beaches.joinToString(",") { "%.3f".format(java.util.Locale.US, it.lon) }
                val common = "latitude=$lat&longitude=$lon&timezone=America%2FSantiago&timeformat=unixtime&forecast_days=6"
                val w = get(
                    "https://api.open-meteo.com/v1/forecast?$common" +
                        "&hourly=wind_speed_10m,wind_gusts_10m,wind_direction_10m,is_day" +
                        "&daily=sunrise,sunset&wind_speed_unit=kn"
                )
                val m = get(
                    "https://marine-api.open-meteo.com/v1/marine?$common" +
                        "&hourly=wave_height,wave_period,wave_direction,sea_surface_temperature"
                )
                wind.writeText(w)
                marine.writeText(m)
                stamp.writeText(Instant.now().epochSecond.toString())
            }.isSuccess
            if (!ok && cachedAt == null) return null
        }
        val at = stamp.readText().trim().toLong().let(Instant::ofEpochSecond)
        return runCatching {
            Result(parse(beaches, wind.readText(), marine.readText(), at), at, fromCache = Instant.now().epochSecond - at.epochSecond > 120)
        }.getOrNull()
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        try {
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().readText()
        } finally {
            c.disconnect()
        }
    }

    private fun asArray(json: String): List<JSONObject> {
        val t = json.trim()
        return if (t.startsWith("[")) JSONArray(t).let { a -> List(a.length()) { a.getJSONObject(it) } }
        else listOf(JSONObject(t))
    }

    private fun parse(beaches: List<Beach>, windJson: String, marineJson: String, at: Instant): Map<String, BeachForecast> {
        val w = asArray(windJson)
        val m = asArray(marineJson)
        return beaches.mapIndexedNotNull { i, b ->
            val wo = w.getOrNull(i) ?: return@mapIndexedNotNull null
            val mo = m.getOrNull(i)
            val wh = wo.getJSONObject("hourly")
            val mh = mo?.optJSONObject("hourly")
            val times = wh.getJSONArray("time")
            val marineIndex = HashMap<Long, Int>()
            mh?.optJSONArray("time")?.let { mt -> for (k in 0 until mt.length()) marineIndex[mt.getLong(k)] = k }
            val hours = List(times.length()) { k ->
                val t = times.getLong(k)
                val mk = marineIndex[t]
                HourConditions(
                    time = Instant.ofEpochSecond(t),
                    windKn = wh.num("wind_speed_10m", k),
                    gustKn = wh.num("wind_gusts_10m", k),
                    windFromDeg = wh.num("wind_direction_10m", k),
                    waveM = mk?.let { mh?.num("wave_height", it) },
                    wavePeriodS = mk?.let { mh?.num("wave_period", it) },
                    waveFromDeg = mk?.let { mh?.num("wave_direction", it) },
                    waterTempC = mk?.let { mh?.num("sea_surface_temperature", it) },
                    isDay = wh.num("is_day", k)?.let { it >= 0.5 },
                )
            }
            val daily = wo.optJSONObject("daily")
            val light = if (daily == null) emptyList() else {
                val d = daily.getJSONArray("time")
                List(d.length()) { k ->
                    DayLight(
                        Instant.ofEpochSecond(d.getLong(k)).atZone(zone).toLocalDate(),
                        Instant.ofEpochSecond(daily.getJSONArray("sunrise").getLong(k)),
                        Instant.ofEpochSecond(daily.getJSONArray("sunset").getLong(k)),
                    )
                }
            }
            b.id to BeachForecast(hours, light, at)
        }.toMap()
    }

    private fun JSONObject.num(key: String, i: Int): Double? {
        val a = optJSONArray(key) ?: return null
        return if (a.isNull(i)) null else a.optDouble(i).takeIf { !it.isNaN() }
    }
}
