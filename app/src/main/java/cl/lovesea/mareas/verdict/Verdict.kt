package cl.lovesea.mareas.verdict

import cl.lovesea.mareas.conditions.HourConditions
import kotlin.math.abs
import kotlin.math.cos

enum class Sport(val label: String, val question: String) {
    SUP("SUP", "¿Puedo remar en SUP?"),
    SWIM("Natación", "¿Puedo nadar?"),
    KAYAK("Kayak", "¿Puedo salir en kayak?"),
    SURF("Surf", "¿Hay para surfear?"),
}

/** Del mejor al peor. */
enum class Level { GREEN, YELLOW, ORANGE, RED, UNKNOWN }

data class Factor(val title: String, val value: String, val detail: String, val level: Level)

data class Verdict(
    val level: Level,
    val headline: String,
    val reason: String,
    val factors: List<Factor>,
)

private fun worst(levels: List<Level>): Level {
    val known = levels.filter { it != Level.UNKNOWN }
    return known.maxByOrNull { it.ordinal } ?: Level.UNKNOWN
}

private fun band(x: Double, ok: Double, care: Double, bad: Double): Level = when {
    x <= ok -> Level.GREEN
    x <= care -> Level.YELLOW
    x <= bad -> Level.ORANGE
    else -> Level.RED
}

/** Cuánto del viento empuja mar adentro, en nudos (0 si sopla hacia la playa). */
fun offshoreComponent(windKn: Double, windFromDeg: Double, seaBearing: Int): Double {
    // El viento va hacia windFrom+180. Es terral si va en la misma dirección en que mira la playa.
    val towards = (windFromDeg + 180) % 360
    val diff = Math.toRadians(towards - seaBearing)
    return maxOf(0.0, windKn * cos(diff))
}

/**
 * Estima la ola en la orilla a partir de la ola mar adentro del modelo:
 * la reduce según lo protegida que está la playa y según de dónde llega el oleaje
 * respecto de hacia dónde mira la playa.
 */
fun nearshoreWave(offshoreM: Double, waveFromDeg: Double?, seaBearing: Int, exposure: Double): Double {
    val dir = if (waveFromDeg == null) 0.75 else {
        val diff = Math.toRadians(waveFromDeg - seaBearing)
        maxOf(0.25, 0.5 + 0.5 * cos(diff))
    }
    return offshoreM * exposure * dir
}

/** Copia de las condiciones con la ola llevada a la orilla de la playa. */
fun HourConditions.atShore(seaBearing: Int, exposure: Double): HourConditions =
    if (waveM == null || offshoreWaveM != null) this
    else copy(waveM = nearshoreWave(waveM, waveFromDeg, seaBearing, exposure), offshoreWaveM = waveM)

fun windWord(kn: Double): String = when {
    kn < 4 -> "Calma"
    kn < 8 -> "Suave"
    kn < 13 -> "Moderado"
    kn < 18 -> "Fresco"
    kn < 25 -> "Fuerte"
    else -> "Muy fuerte"
}

fun waveWord(m: Double): String = when {
    m < 0.3 -> "Plano"
    m < 0.5 -> "A los tobillos"
    m < 0.8 -> "A la cintura"
    m < 1.2 -> "Al pecho"
    m < 1.8 -> "Sobre la cabeza"
    m < 2.5 -> "Grande"
    else -> "Muy grande"
}

private val COMPASS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
fun compass(deg: Double): String = COMPASS[(((deg % 360) + 360) % 360 / 45.0 + 0.5).toInt() % 8]

/**
 * Reglas propias, conservadoras, pensadas para alguien de nivel intermedio.
 * El veredicto es el peor de los factores.
 */
fun assess(sport: Sport, c: HourConditions?, seaBearing: Int): Verdict {
    if (c == null || (c.windKn == null && c.waveM == null)) {
        return Verdict(Level.UNKNOWN, "Sin datos", "No hay pronóstico para esta hora.", emptyList())
    }
    val factors = mutableListOf<Factor>()

    c.windKn?.let { w ->
        val (ok, care, bad) = when (sport) {
            Sport.SUP -> Triple(8.0, 12.0, 16.0)
            Sport.SWIM -> Triple(12.0, 16.0, 22.0)
            Sport.KAYAK -> Triple(10.0, 15.0, 20.0)
            Sport.SURF -> Triple(10.0, 15.0, 22.0)
        }
        val dir = c.windFromDeg?.let { " del ${compass(it)}" } ?: ""
        factors += Factor("Viento", "${fmt(w)} kn", "${windWord(w)}$dir", band(w, ok, care, bad))
    }
    c.gustKn?.let { g ->
        if (sport != Sport.SWIM) {
            val (ok, care, bad) = when (sport) {
                Sport.SUP -> Triple(14.0, 18.0, 24.0)
                Sport.KAYAK -> Triple(16.0, 21.0, 27.0)
                else -> Triple(20.0, 26.0, 32.0)
            }
            val lvl = band(g, ok, care, bad)
            factors += Factor("Rachas", "${fmt(g)} kn", if (lvl == Level.GREEN) "Sin golpes fuertes" else "Golpes de viento", lvl)
        }
    }
    if (c.windKn != null && c.windFromDeg != null && sport != Sport.SURF) {
        val off = offshoreComponent(c.windKn, c.windFromDeg, seaBearing)
        val lvl = when (sport) {
            Sport.SUP -> band(off, 5.0, 8.0, 12.0)
            Sport.KAYAK -> band(off, 7.0, 10.0, 14.0)
            else -> band(off, 10.0, 14.0, 18.0)
        }
        factors += Factor(
            "Te aleja de la costa",
            if (off < 2) "No" else "Sí",
            if (off < 2) "El viento no te empuja mar adentro" else "Empuja mar adentro con ${fmt(off)} kn",
            lvl,
        )
    }
    c.waveM?.let { h ->
        val period = c.wavePeriodS?.let { " · cada ${it.toInt()} s" } ?: ""
        val off = c.offshoreWaveM?.let { " · mar adentro ${fmt(it)} m" } ?: ""
        val lvl = when (sport) {
            Sport.SUP -> band(h, 0.5, 0.9, 1.3)
            Sport.SWIM -> band(h, 0.5, 0.9, 1.3)
            Sport.KAYAK -> band(h, 0.7, 1.2, 1.8)
            Sport.SURF -> when {
                h < 0.3 -> Level.ORANGE
                h < 0.5 -> Level.YELLOW
                h <= 2.0 -> Level.GREEN
                h <= 2.8 -> Level.YELLOW
                h <= 3.5 -> Level.ORANGE
                else -> Level.RED
            }
        }
        factors += Factor("Olas en la orilla", "${fmt(h)} m", "${waveWord(h)}$period$off", lvl)
    }
    if (sport == Sport.SURF) c.wavePeriodS?.let { p ->
        val lvl = when {
            p >= 10 -> Level.GREEN
            p >= 7 -> Level.YELLOW
            else -> Level.ORANGE
        }
        factors += Factor("Período", "${p.toInt()} s", if (p >= 10) "Olas ordenadas" else "Olas cortas y desordenadas", lvl)
    }
    c.waterTempC?.let { t ->
        val lvl = when (sport) {
            Sport.SWIM -> if (t >= 16) Level.GREEN else if (t >= 13) Level.YELLOW else Level.ORANGE
            else -> if (t >= 13) Level.GREEN else Level.YELLOW
        }
        val word = when {
            t < 13 -> "Agua muy fría, usa traje"
            t < 16 -> "Agua fría, ideal con traje"
            else -> "Agua templada para la zona"
        }
        factors += Factor("Agua", "${fmt(t)} °C", word, lvl)
    }
    c.isDay?.let { day ->
        factors += Factor(
            "Luz",
            if (day) "Día" else "Noche",
            if (day) "Hay luz natural" else "De noche no es seguro meterse",
            if (day) Level.GREEN else Level.ORANGE,
        )
    }

    val level = worst(factors.map { it.level })
    val headline = when (level) {
        Level.GREEN -> if (sport == Sport.SURF) "Sí, buenas olas" else "Sí, ideal"
        Level.YELLOW -> "Sí, con cuidado"
        Level.ORANGE -> "Mejor no"
        Level.RED -> if (sport == Sport.SURF) "Solo expertos" else "No te metas"
        Level.UNKNOWN -> "Sin datos"
    }
    val main = factors.filter { it.level == level }.firstOrNull()
    val reason = when {
        main == null -> ""
        level == Level.GREEN -> "${main.title}: ${main.detail.lowercase()}"
        else -> "${main.title}: ${main.detail.replaceFirstChar { it.lowercase() }}"
    }
    return Verdict(level, headline, reason.replaceFirstChar { it.uppercase() }, factors)
}

/** Puntaje para elegir la mejor hora: menor es mejor. */
fun score(v: Verdict): Int = v.level.ordinal * 100 + v.factors.count { it.level != Level.GREEN }

private fun fmt(x: Double): String =
    if (abs(x) >= 10) String.format(java.util.Locale("es", "CL"), "%.0f", x)
    else String.format(java.util.Locale("es", "CL"), "%.1f", x)
