package com.example.pokatop.timer

data class Preset(val key: String, val focusMin: Int, val shortBreakMin: Int, val longBreakMin: Int)

object Presets {
    const val CUSTOM = "custom"
    const val DEFAULT = "classic"
    val FOCUS_RANGE = 5..120
    val SHORT_RANGE = 1..30
    val LONG_RANGE = 5..60
    val EVERY_RANGE = 2..6

    val BUILT_IN: List<Preset> = listOf(
        Preset("short", 15, 3, 10),
        Preset("classic", 25, 5, 15),
        Preset("long", 45, 10, 20),
        Preset("p5217", 52, 17, 30),
        Preset("max", 90, 20, 30)
    )

    fun resolve(key: String, customFocus: Int, customShort: Int, customLong: Int): Preset {
        if (key == CUSTOM) {
            return Preset(
                CUSTOM,
                customFocus.coerceIn(FOCUS_RANGE),
                customShort.coerceIn(SHORT_RANGE),
                customLong.coerceIn(LONG_RANGE)
            )
        }
        return BUILT_IN.firstOrNull { it.key == key } ?: BUILT_IN.first { it.key == DEFAULT }
    }

    fun match(focusMin: Int, shortMin: Int, longMin: Int): Preset? =
        BUILT_IN.firstOrNull { it.focusMin == focusMin && it.shortBreakMin == shortMin && it.longBreakMin == longMin }
}
