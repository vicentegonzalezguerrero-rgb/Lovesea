package cl.lovesea.mareas.verdict

import cl.lovesea.mareas.conditions.HourConditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class VerdictTest {
    private fun hour(
        wind: Double = 5.0, gust: Double = 8.0, from: Double = 225.0,
        wave: Double = 0.4, period: Double = 12.0, water: Double = 17.0, day: Boolean = true,
    ) = HourConditions(Instant.EPOCH, wind, gust, from, wave, period, 270.0, water, day)

    @Test fun calmDayIsGreenForSup() {
        val v = assess(Sport.SUP, hour(), 270)
        assertEquals(Level.GREEN, v.level)
        assertEquals("Sí, ideal", v.headline)
    }

    @Test fun waistHighWavesMeanCareForSwimming() {
        assertEquals(Level.YELLOW, assess(Sport.SWIM, hour(wave = 0.7), 270).level)
    }

    @Test fun bigSwellIsRedForSwimmingButGoodForSurf() {
        assertEquals(Level.RED, assess(Sport.SWIM, hour(wave = 1.6), 270).level)
        assertEquals(Level.GREEN, assess(Sport.SURF, hour(wave = 1.6), 270).level)
    }

    @Test fun flatSeaIsNotWorthSurfing() {
        assertEquals(Level.ORANGE, assess(Sport.SURF, hour(wave = 0.2), 270).level)
    }

    @Test fun offshoreWindBlocksSup() {
        // Viento del este (90°) en una playa que mira al oeste: empuja mar adentro.
        val v = assess(Sport.SUP, hour(wind = 11.0, gust = 13.0, from = 90.0), 270)
        assertTrue(v.level.ordinal >= Level.ORANGE.ordinal)
        assertTrue(v.factors.any { it.title == "Te aleja de la costa" && it.level >= Level.ORANGE })
    }

    @Test fun onshoreWindIsNotOffshore() {
        assertEquals(0.0, offshoreComponent(15.0, 270.0, 270), 1e-9)
        assertEquals(15.0, offshoreComponent(15.0, 90.0, 270), 1e-9)
    }

    @Test fun nightIsNotRecommended() {
        assertEquals(Level.ORANGE, assess(Sport.SWIM, hour(day = false), 270).level)
    }

    @Test fun noDataIsUnknown() {
        assertEquals(Level.UNKNOWN, assess(Sport.KAYAK, null, 270).level)
    }

    @Test fun compassNames() {
        assertEquals("SW", compass(225.0))
        assertEquals("N", compass(355.0))
    }
}
