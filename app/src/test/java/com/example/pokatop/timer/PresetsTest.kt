package com.example.pokatop.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetsTest {

    @Test
    fun unknownKeyResolvesToClassic() {
        assertEquals(Preset("classic", 25, 5, 15), Presets.resolve("nope", 30, 5, 15))
    }

    @Test
    fun builtInsMatchSpec() {
        val rows = Presets.BUILT_IN.map { listOf(it.key, it.focusMin, it.shortBreakMin, it.longBreakMin) }
        assertEquals(
            listOf(
                listOf("short", 15, 3, 10),
                listOf("classic", 25, 5, 15),
                listOf("long", 45, 10, 20),
                listOf("p5217", 52, 17, 30),
                listOf("max", 90, 20, 30)
            ),
            rows
        )
    }

    @Test
    fun customIsClamped() {
        assertEquals(Preset("custom", 120, 1, 60), Presets.resolve("custom", 200, 0, 100))
    }

    @Test
    fun matchFindsBuiltIn() {
        assertEquals("p5217", Presets.match(52, 17, 30)?.key)
        assertNull(Presets.match(30, 5, 15))
    }

    @Test
    fun configClampsLongEvery() {
        val classic = Presets.resolve("classic", 0, 0, 0)
        val config = sessionConfigOf(classic, 9, true)
        assertEquals(6, config.longEvery)
        assertEquals(1_500_000L, config.focusMs)
    }
}
