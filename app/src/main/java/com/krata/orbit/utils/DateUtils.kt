package com.krata.orbit.utils

import com.krata.orbit.data.model.Event
import com.krata.orbit.data.model.EventFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, hh:mm a", Locale.ENGLISH)

    /** Returns "YYYY-MM" for the current month, offset by [offsetMonths]. */
    fun monthKeyForOffset(offsetMonths: Long = 0): String {
        val ym = YearMonth.now().plusMonths(offsetMonths)
        return "%04d-%02d".format(ym.year, ym.monthValue)
    }

    fun currentMonthKey(): String = monthKeyForOffset(0)

    fun Long.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(this),
            ZoneId.systemDefault()
        )

    fun LocalDateTime.toMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun formatDateTime(millis: Long): String =
        millis.toLocalDateTime().format(DATETIME_FORMATTER)

    fun formatDate(millis: Long): String =
        millis.toLocalDateTime().toLocalDate().format(DATE_FORMATTER)

    fun formatTime(millis: Long): String =
        millis.toLocalDateTime().format(TIME_FORMATTER)

    /** Calculates the next occurrence (in millis) for a recurring event after its current date. */
    fun nextOccurrenceMillis(event: Event): Long {
        val current = event.dateTimeMillis.toLocalDateTime()
        val nextDt: LocalDateTime = when (event.frequency) {
            EventFrequency.DAILY   -> current.plusDays(1)
            EventFrequency.WEEKLY  -> {
                val targetDow = if (event.weekday in 1..7)
                    DayOfWeek.of(event.weekday) else current.dayOfWeek
                var next = current.plusDays(1)
                while (next.dayOfWeek != targetDow) next = next.plusDays(1)
                next
            }
            EventFrequency.MONTHLY -> {
                val days = event.monthDays.sorted()
                if (days.isEmpty()) {
                    current.plusMonths(1)
                } else {
                    val currentDay = current.dayOfMonth
                    val nextDay = days.firstOrNull { it > currentDay }
                    if (nextDay != null) {
                        safeWithDayOfMonth(current.year, current.monthValue, nextDay, current.hour, current.minute)
                    } else {
                        // Wrap to next month
                        val nm = YearMonth.of(current.year, current.monthValue).plusMonths(1)
                        // Find the first valid day in that month
                        val validDay = days.firstOrNull { it <= nm.lengthOfMonth() }
                            ?: days.lastOrNull { it <= nm.lengthOfMonth() }
                            ?: nm.lengthOfMonth()
                        safeWithDayOfMonth(nm.year, nm.monthValue, validDay, current.hour, current.minute)
                    }
                }
            }
            EventFrequency.NONE -> current.plusDays(1) // fallback
        }
        return nextDt.toMillis()
    }

    private fun safeWithDayOfMonth(year: Int, month: Int, day: Int, hour: Int, minute: Int): LocalDateTime {
        val ym = YearMonth.of(year, month)
        val safeDay = minOf(day, ym.lengthOfMonth())
        return LocalDateTime.of(year, month, safeDay, hour, minute)
    }

    fun weekdayName(dow: Int): String =
        DayOfWeek.of(dow).getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    fun daysInMonth(year: Int, month: Int): Int = YearMonth.of(year, month).lengthOfMonth()

    fun todayString(): String = LocalDate.now().toString() // yyyy-MM-dd
}
