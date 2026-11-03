package de.odpisz.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPlannerTest {
    private val zone: ZoneId = ZoneId.of("Europe/Berlin")

    @Test
    fun schedulesSevenTwoAndSameDayAtNine() {
        val deadline = LocalDate.of(2026, 11, 20)
        val now = ZonedDateTime.of(2026, 11, 3, 12, 0, 0, 0, zone)
        val planned = ReminderPlanner.plan(deadline, now, zone)

        assertEquals(listOf(7, 2, 0), planned.map { it.offsetDays })
        assertEquals(9, planned[0].fireAt.atZone(zone).hour)
        assertEquals(LocalDate.of(2026, 11, 13), planned[0].fireAt.atZone(zone).toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 18), planned[1].fireAt.atZone(zone).toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 20), planned[2].fireAt.atZone(zone).toLocalDate())
        assertTrue(planned[0].message.contains("7"))
        assertTrue(planned[2].message.contains("Dziś"))
    }

    @Test
    fun dropsPastSlotsAndKeepsSameDayAfterNine() {
        val deadline = LocalDate.of(2026, 11, 3)
        val now = ZonedDateTime.of(2026, 11, 3, 15, 0, 0, 0, zone)
        val planned = ReminderPlanner.plan(deadline, now, zone)

        assertEquals(listOf(0), planned.map { it.offsetDays })
        assertTrue(planned.single().fireAt.isAfter(now.toInstant()))
    }
}
