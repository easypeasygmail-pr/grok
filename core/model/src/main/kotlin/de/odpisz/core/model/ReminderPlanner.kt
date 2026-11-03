package de.odpisz.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class PlannedReminder(
    val offsetDays: Int,
    val fireAt: Instant,
    val message: String,
)

/**
 * Local reminders at 09:00: seven days before, two days before, and on the day.
 * Slots that are already in the past are skipped. If the deadline is today and
 * 09:00 has passed, the same-day reminder is due one minute from [now].
 */
object ReminderPlanner {
    private val offsets = listOf(WEEK_OFFSET, TWO_DAY_OFFSET, SAME_DAY_OFFSET)
    private val morning: LocalTime = LocalTime.of(MORNING_HOUR, 0)

    private const val WEEK_OFFSET = 7
    private const val TWO_DAY_OFFSET = 2
    private const val SAME_DAY_OFFSET = 0
    private const val MORNING_HOUR = 9

    fun plan(
        deadline: LocalDate,
        now: ZonedDateTime,
        zone: ZoneId = now.zone,
    ): List<PlannedReminder> =
        offsets.mapNotNull { offset ->
            val day = deadline.minusDays(offset.toLong())
            val morningSlot = ZonedDateTime.of(day, morning, zone)
            val fireAt =
                when {
                    !morningSlot.isBefore(now) -> morningSlot
                    offset == 0 && day == now.toLocalDate() -> now.plusMinutes(1)
                    else -> null
                } ?: return@mapNotNull null
            PlannedReminder(
                offsetDays = offset,
                fireAt = fireAt.toInstant(),
                message = message(offset),
            )
        }

    fun message(offsetDays: Int): String =
        when (offsetDays) {
            WEEK_OFFSET -> "Za 7 dni mija termin."
            TWO_DAY_OFFSET -> "Za 2 dni mija termin."
            else -> "Dziś mija termin."
        }
}
