package cl.lovesea.mareas.conditions

import java.time.Instant

/** Condiciones del mar y del viento en una hora para una playa. Nulos = sin datos. */
data class HourConditions(
    val time: Instant,
    val windKn: Double?,
    val gustKn: Double?,
    /** De dónde viene el viento, en grados (0 = norte). */
    val windFromDeg: Double?,
    val waveM: Double?,
    val wavePeriodS: Double?,
    val waveFromDeg: Double?,
    val waterTempC: Double?,
    val isDay: Boolean?,
)

data class DayLight(val date: java.time.LocalDate, val sunrise: Instant, val sunset: Instant)

data class BeachForecast(
    val hours: List<HourConditions>,
    val daylight: List<DayLight>,
    val fetchedAt: Instant,
) {
    /** La hora de pronóstico más cercana al instante dado. */
    fun at(instant: Instant): HourConditions? =
        hours.minByOrNull { kotlin.math.abs(it.time.epochSecond - instant.epochSecond) }
            ?.takeIf { kotlin.math.abs(it.time.epochSecond - instant.epochSecond) <= 3600 }

    fun daylightOn(date: java.time.LocalDate): DayLight? = daylight.firstOrNull { it.date == date }
}
