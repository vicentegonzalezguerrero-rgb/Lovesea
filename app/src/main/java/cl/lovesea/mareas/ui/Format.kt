package cl.lovesea.mareas.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val CHILE: ZoneId = ZoneId.of("America/Santiago")
private val ES = Locale("es", "CL")
private val HOUR = DateTimeFormatter.ofPattern("HH:mm", ES)
private val DAY = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)

fun Instant.hhmm(): String = HOUR.format(atZone(CHILE))
fun LocalDate.longEs(): String = DAY.format(this).replaceFirstChar { it.uppercase(ES) }
fun Double.meters(): String = String.format(ES, "%.2f m", this)
