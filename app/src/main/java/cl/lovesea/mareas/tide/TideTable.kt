package cl.lovesea.mareas.tide

import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos

/** Una pleamar (isHigh = true) o bajamar, en segundos UTC y metros. */
data class TideEvent(val epochSecond: Long, val heightM: Double, val isHigh: Boolean) {
    val instant: Instant get() = Instant.ofEpochSecond(epochSecond)
}

enum class Trend { RISING, FALLING }

data class TideNow(
    val heightM: Double,
    val trend: Trend,
    val next: TideEvent,
)

/**
 * Tabla de pleamares y bajamares de un puerto de referencia.
 * Entre dos extremos la altura se interpola con la regla del coseno,
 * que es la forma estándar de dibujar la curva a partir de una tabla de mareas.
 */
class TideTable(events: List<TideEvent>) {
    val events: List<TideEvent> = events.sortedBy { it.epochSecond }

    val firstInstant: Instant? get() = events.firstOrNull()?.instant
    val lastInstant: Instant? get() = events.lastOrNull()?.instant

    /** Altura en metros en un instante, o null fuera del rango de la tabla. */
    fun heightAt(instant: Instant): Double? {
        val (a, b) = bracket(instant.epochSecond) ?: return null
        val span = (b.epochSecond - a.epochSecond).toDouble()
        val x = (instant.epochSecond - a.epochSecond) / span
        return a.heightM + (b.heightM - a.heightM) * (1 - cos(PI * x)) / 2
    }

    fun now(instant: Instant): TideNow? {
        val (a, b) = bracket(instant.epochSecond) ?: return null
        val h = heightAt(instant) ?: return null
        return TideNow(h, if (b.heightM > a.heightM) Trend.RISING else Trend.FALLING, b)
    }

    /** Extremos que caen dentro del día local indicado. */
    fun eventsOn(date: LocalDate, zone: ZoneId): List<TideEvent> {
        val start = date.atStartOfDay(zone).toEpochSecond()
        val end = date.plusDays(1).atStartOfDay(zone).toEpochSecond()
        return events.filter { it.epochSecond in start until end }
    }

    /** Copia de la tabla con todas las horas corridas (puertos secundarios). */
    fun shifted(seconds: Long): TideTable =
        if (seconds == 0L) this else TideTable(events.map { it.copy(epochSecond = it.epochSecond + seconds) })

    /** Muestras (instante, altura) cada [stepMinutes] minutos para dibujar la curva del día. */
    fun curveOn(date: LocalDate, zone: ZoneId, stepMinutes: Long = 10): List<Pair<Instant, Double>> {
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val out = ArrayList<Pair<Instant, Double>>()
        var t = start
        while (!t.isAfter(end)) {
            heightAt(t)?.let { out += t to it }
            t = t.plusSeconds(stepMinutes * 60)
        }
        return out
    }

    private fun bracket(sec: Long): Pair<TideEvent, TideEvent>? {
        if (events.size < 2) return null
        var lo = 0
        var hi = events.size - 1
        if (sec < events[lo].epochSecond || sec > events[hi].epochSecond) return null
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (events[mid].epochSecond <= sec) lo = mid else hi = mid
        }
        return events[lo] to events[hi]
    }

    companion object {
        /** Lee el CSV generado por tools/harmonics/derive.py: epoch,altura,H|L */
        fun parse(input: InputStream): TideTable {
            val list = ArrayList<TideEvent>(16000)
            input.bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (line.isBlank() || line.startsWith("#")) continue
                    val p = line.split(',')
                    if (p.size < 3) continue
                    list += TideEvent(p[0].trim().toLong(), p[1].trim().toDouble(), p[2].trim() == "H")
                }
            }
            return TideTable(list)
        }
    }
}
