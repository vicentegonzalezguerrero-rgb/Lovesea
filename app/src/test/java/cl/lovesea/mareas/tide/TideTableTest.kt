package cl.lovesea.mareas.tide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TideTableTest {

    private val low = TideEvent(0, 0.2, false)
    private val high = TideEvent(6 * 3600, 1.4, true)
    private val low2 = TideEvent(12 * 3600, 0.4, false)
    private val table = TideTable(listOf(high, low, low2))

    @Test fun interpolatesWithCosineRule() {
        assertEquals(0.2, table.heightAt(Instant.ofEpochSecond(0))!!, 1e-9)
        assertEquals(1.4, table.heightAt(Instant.ofEpochSecond(6 * 3600))!!, 1e-9)
        assertEquals(0.8, table.heightAt(Instant.ofEpochSecond(3 * 3600))!!, 1e-9)
    }

    @Test fun outsideRangeIsNull() {
        assertNull(table.heightAt(Instant.ofEpochSecond(-1)))
        assertNull(table.heightAt(Instant.ofEpochSecond(13 * 3600)))
    }

    @Test fun trendAndNextEvent() {
        val rising = table.now(Instant.ofEpochSecond(3600))!!
        assertEquals(Trend.RISING, rising.trend)
        assertEquals(high, rising.next)
        val falling = table.now(Instant.ofEpochSecond(7 * 3600))!!
        assertEquals(Trend.FALLING, falling.trend)
        assertEquals(low2, falling.next)
    }

    @Test fun eventsOnLocalDay() {
        val zone = ZoneId.of("UTC")
        assertEquals(3, table.eventsOn(LocalDate.of(1970, 1, 1), zone).size)
        assertEquals(0, table.eventsOn(LocalDate.of(1970, 1, 2), zone).size)
    }

    @Test fun shiftMovesTimes() {
        val s = table.shifted(600)
        assertEquals(600L, s.events.first().epochSecond)
        assertEquals(0.2, s.heightAt(Instant.ofEpochSecond(600))!!, 1e-9)
    }

    @Test fun parsesCsv() {
        val t = TideTable.parse("# x\n10,0.50,H\n\n20,0.10,L\n".byteInputStream())
        assertEquals(2, t.events.size)
        assertEquals(true, t.events[0].isHigh)
        assertEquals(0.1, t.events[1].heightM, 1e-9)
    }
}
