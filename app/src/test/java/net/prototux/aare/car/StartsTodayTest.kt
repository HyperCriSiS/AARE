package net.prototux.aare.car

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StartsTodayTest {
    private fun at(zone: String, year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone(zone)).apply { clear(); set(year, month, day, hour, minute) }

    private fun timed(start: Calendar) = NextEvent("Meeting", start.timeInMillis, allDay = false, location = "")

    /** All-day events are stored as midnight UTC of their day. */
    private fun allDay(year: Int, month: Int, day: Int) = NextEvent("Holiday", at("UTC", year, month, day, 0).timeInMillis, allDay = true, location = "")

    @Test
    fun aTimedEventLaterTodayIsToday() {
        val now = at("Europe/Berlin", 2026, Calendar.NOVEMBER, 13, 9)
        assertTrue(startsToday(timed(at("Europe/Berlin", 2026, Calendar.NOVEMBER, 13, 23, 30)), now))
    }

    @Test
    fun aTimedEventAfterMidnightIsTomorrow() {
        val now = at("Europe/Berlin", 2026, Calendar.NOVEMBER, 13, 22)
        assertFalse(startsToday(timed(at("Europe/Berlin", 2026, Calendar.NOVEMBER, 14, 0, 30)), now))
    }

    @Test
    fun anAllDayEventKeepsItsDayWestOfGreenwich() {
        // Midnight UTC is still the day before in Los Angeles: read in the phone's zone it would be yesterday.
        val now = at("America/Los_Angeles", 2026, Calendar.NOVEMBER, 13, 10)
        assertTrue(startsToday(allDay(2026, Calendar.NOVEMBER, 13), now))
        assertFalse(startsToday(allDay(2026, Calendar.NOVEMBER, 14), now))
    }

    @Test
    fun anAllDayEventKeepsItsDayEastOfGreenwich() {
        val now = at("Asia/Tokyo", 2026, Calendar.NOVEMBER, 13, 20)
        assertTrue(startsToday(allDay(2026, Calendar.NOVEMBER, 13), now))
    }
}
