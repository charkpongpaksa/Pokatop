package com.example.pokatop.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.pokatop.timer.Presets
import com.example.pokatop.timer.SessionConfig
import com.example.pokatop.timer.sessionConfigOf

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var nightMode: Int
        get() = sp.getInt(KEY_NIGHT, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = sp.edit { putInt(KEY_NIGHT, value) }

    var soundKey: String
        get() = sp.getString(KEY_SOUND, "chime") ?: "chime"
        set(value) = sp.edit { putString(KEY_SOUND, value) }

    var vibrate: Boolean
        get() = sp.getBoolean(KEY_VIBRATE, true)
        set(value) = sp.edit { putBoolean(KEY_VIBRATE, value) }

    var detection: Boolean
        get() = sp.getBoolean(KEY_DETECTION, true)
        set(value) = sp.edit { putBoolean(KEY_DETECTION, value) }

    var presetKey: String
        get() = sp.getString(KEY_PRESET, Presets.DEFAULT) ?: Presets.DEFAULT
        set(value) = sp.edit { putString(KEY_PRESET, value) }

    var customFocusMin: Int
        get() = sp.getInt(KEY_CUSTOM_FOCUS, 30)
        set(value) = sp.edit { putInt(KEY_CUSTOM_FOCUS, value) }

    var customShortMin: Int
        get() = sp.getInt(KEY_CUSTOM_SHORT, 5)
        set(value) = sp.edit { putInt(KEY_CUSTOM_SHORT, value) }

    var customLongMin: Int
        get() = sp.getInt(KEY_CUSTOM_LONG, 15)
        set(value) = sp.edit { putInt(KEY_CUSTOM_LONG, value) }

    var longEvery: Int
        get() = sp.getInt(KEY_EVERY, 4)
        set(value) = sp.edit { putInt(KEY_EVERY, value) }

    var pendingSession: String?
        get() = sp.getString(KEY_PENDING, null)
        set(value) = sp.edit { if (value == null) remove(KEY_PENDING) else putString(KEY_PENDING, value) }

    fun currentConfig(hasAccelerometer: Boolean): SessionConfig {
        val preset = Presets.resolve(presetKey, customFocusMin, customShortMin, customLongMin)
        return sessionConfigOf(preset, longEvery, detection && hasAccelerometer)
    }

    companion object {
        const val FILE = "pokatop_prefs"
        private const val KEY_NIGHT = "night_mode"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_DETECTION = "detection"
        private const val KEY_PRESET = "preset"
        private const val KEY_CUSTOM_FOCUS = "custom_focus"
        private const val KEY_CUSTOM_SHORT = "custom_short"
        private const val KEY_CUSTOM_LONG = "custom_long"
        private const val KEY_EVERY = "long_every"
        const val KEY_PENDING = "pending_session"
    }
}
