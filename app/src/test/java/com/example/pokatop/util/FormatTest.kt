package com.example.pokatop.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun clockUsesCeilingSeconds() {
        assertEquals("00:00", Format.clock(0))
        assertEquals("00:01", Format.clock(1))
        assertEquals("01:00", Format.clock(59_001))
        assertEquals("25:00", Format.clock(1_500_000))
    }

    @Test
    fun clockShowsHoursFromOneHour() {
        assertEquals("1:00:00", Format.clock(3_600_000))
        assertEquals("1:30:00", Format.clock(5_400_000))
    }

    @Test
    fun clockNeverNegative() {
        assertEquals("00:00", Format.clock(-5_000))
    }

    @Test
    fun hoursMinutesFloors() {
        assertEquals(1 to 25, Format.hoursMinutes(5_100_000))
        assertEquals(0 to 0, Format.hoursMinutes(59_999))
    }
}
