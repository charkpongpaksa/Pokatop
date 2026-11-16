package com.example.pokatop.ui

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.pokatop.R
import com.example.pokatop.data.Prefs
import com.example.pokatop.timer.Presets
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var alertPlayer: AlertPlayer
    private lateinit var customPanel: View
    private lateinit var saveCustom: MaterialButton
    private val draft = IntArray(3)
    private val saved = IntArray(3)
    private var every = 4
    private val soundRadios = mutableMapOf<String, MaterialRadioButton>()

    private val dirty: Boolean
        get() = !draft.contentEquals(saved)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<View>(R.id.main).applySystemBarPadding()
        prefs = Prefs(this)
        alertPlayer = AlertPlayer(this)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<View>(R.id.pending_note).visibility = if (prefs.pendingSession != null) View.VISIBLE else View.GONE

        saved[0] = prefs.customFocusMin
        saved[1] = prefs.customShortMin
        saved[2] = prefs.customLongMin
        if (savedInstanceState != null) {
            savedInstanceState.getIntArray(KEY_DRAFT)?.copyInto(draft)
        } else {
            saved.copyInto(draft)
        }

        setupDetection()
        setupPresets()
        setupCustom()
        setupEvery()
        setupSounds()
        setupLanguage()
        setupTheme()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (dirty) confirmDiscard() else finish()
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray(KEY_DRAFT, draft)
    }

    override fun onPause() {
        alertPlayer.stop()
        super.onPause()
    }

    private fun setupDetection() {
        val switch = findViewById<MaterialSwitch>(R.id.switch_detection)
        val desc = findViewById<TextView>(R.id.detection_desc)
        val hasSensor = getSystemService(SensorManager::class.java).getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        if (!hasSensor) {
            switch.isChecked = false
            switch.isEnabled = false
            desc.setText(R.string.settings_no_sensor)
            return
        }
        switch.isChecked = prefs.detection
        desc.setText(if (prefs.detection) R.string.settings_detection_on_desc else R.string.settings_detection_off_desc)
        switch.setOnCheckedChangeListener { _, checked ->
            prefs.detection = checked
            desc.setText(if (checked) R.string.settings_detection_on_desc else R.string.settings_detection_off_desc)
        }
    }

    private fun setupPresets() {
        customPanel = findViewById(R.id.custom_panel)
        val ids = mapOf(
            "short" to R.id.preset_short,
            "classic" to R.id.preset_classic,
            "long" to R.id.preset_long,
            "p5217" to R.id.preset_p5217,
            "max" to R.id.preset_max,
            Presets.CUSTOM to R.id.preset_custom
        )
        Presets.BUILT_IN.forEach { p ->
            findViewById<MaterialRadioButton>(ids.getValue(p.key)).text = getString(
                R.string.preset_row_fmt,
                getString(MainActivity.presetNameRes(p.key)),
                p.focusMin,
                p.shortBreakMin,
                p.longBreakMin
            )
        }
        renderCustomRow()
        val current = ids[prefs.presetKey] ?: R.id.preset_classic
        val group = findViewById<android.widget.RadioGroup>(R.id.preset_group)
        group.check(current)
        customPanel.visibility = if (current == R.id.preset_custom) View.VISIBLE else View.GONE
        group.setOnCheckedChangeListener { _, checkedId ->
            val key = ids.entries.firstOrNull { it.value == checkedId }?.key ?: return@setOnCheckedChangeListener
            prefs.presetKey = key
            customPanel.visibility = if (key == Presets.CUSTOM) View.VISIBLE else View.GONE
        }
    }

    private fun renderCustomRow() {
        findViewById<MaterialRadioButton>(R.id.preset_custom).text = getString(
            R.string.preset_row_fmt,
            getString(R.string.preset_custom),
            saved[0],
            saved[1],
            saved[2]
        )
    }

    private fun setupCustom() {
        saveCustom = findViewById(R.id.btn_save_custom)
        bindStepper(R.id.step_focus, R.string.settings_focus, Presets.FOCUS_RANGE, 5, 0)
        bindStepper(R.id.step_short, R.string.settings_short, Presets.SHORT_RANGE, 1, 1)
        bindStepper(R.id.step_long, R.string.settings_long, Presets.LONG_RANGE, 5, 2)
        saveCustom.setOnClickListener {
            draft.copyInto(saved)
            prefs.customFocusMin = saved[0]
            prefs.customShortMin = saved[1]
            prefs.customLongMin = saved[2]
            renderCustomRow()
            saveCustom.isEnabled = false
        }
        saveCustom.isEnabled = dirty
    }

    private fun bindStepper(@IdRes rowId: Int, labelRes: Int, range: IntRange, step: Int, index: Int) {
        val row = findViewById<View>(rowId)
        val label = getString(labelRes)
        row.findViewById<TextView>(R.id.stepper_label).text = label
        row.findViewById<TextView>(R.id.stepper_range).text = getString(R.string.range_min_fmt, range.first, range.last)
        val value = row.findViewById<TextView>(R.id.stepper_value)
        val render = {
            value.text = getString(R.string.minutes_value_fmt, draft[index])
            saveCustom.isEnabled = dirty
        }
        row.findViewById<MaterialButton>(R.id.stepper_minus).apply {
            contentDescription = getString(R.string.decrease_fmt, label)
            setOnClickListener {
                draft[index] = (draft[index] - step).coerceIn(range)
                render()
            }
        }
        row.findViewById<MaterialButton>(R.id.stepper_plus).apply {
            contentDescription = getString(R.string.increase_fmt, label)
            setOnClickListener {
                draft[index] = (draft[index] + step).coerceIn(range)
                render()
            }
        }
        render()
    }

    private fun setupEvery() {
        every = prefs.longEvery.coerceIn(Presets.EVERY_RANGE)
        val row = findViewById<View>(R.id.step_every)
        val label = getString(R.string.settings_long_every)
        row.findViewById<TextView>(R.id.stepper_label).text = label
        row.findViewById<TextView>(R.id.stepper_range).setText(R.string.settings_long_every_range)
        val value = row.findViewById<TextView>(R.id.stepper_value)
        val render = { value.text = getString(R.string.rounds_value_fmt, every) }
        row.findViewById<MaterialButton>(R.id.stepper_minus).apply {
            contentDescription = getString(R.string.decrease_fmt, label)
            setOnClickListener {
                every = (every - 1).coerceIn(Presets.EVERY_RANGE)
                prefs.longEvery = every
                render()
            }
        }
        row.findViewById<MaterialButton>(R.id.stepper_plus).apply {
            contentDescription = getString(R.string.increase_fmt, label)
            setOnClickListener {
                every = (every + 1).coerceIn(Presets.EVERY_RANGE)
                prefs.longEvery = every
                render()
            }
        }
        render()
    }

    private fun setupSounds() {
        SOUNDS.forEach { (key, radioId, playId) ->
            val radio = findViewById<MaterialRadioButton>(radioId)
            soundRadios[key] = radio
            radio.isChecked = prefs.soundKey == key
            radio.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    prefs.soundKey = key
                    soundRadios.filterKeys { it != key }.values.forEach { it.isChecked = false }
                }
            }
            findViewById<ImageButton>(playId).setOnClickListener { alertPlayer.preview(key) }
        }
        val vibrate = findViewById<MaterialSwitch>(R.id.switch_vibrate)
        vibrate.isChecked = prefs.vibrate
        vibrate.setOnCheckedChangeListener { _, checked -> prefs.vibrate = checked }
    }

    private fun setupLanguage() {
        val group = findViewById<ChipGroup>(R.id.lang_group)
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        group.check(
            when {
                current.startsWith("th") -> R.id.chip_lang_th
                current.startsWith("en") -> R.id.chip_lang_en
                else -> R.id.chip_lang_system
            }
        )
        group.setOnCheckedStateChangeListener { _, ids ->
            val tag = when (ids.firstOrNull()) {
                R.id.chip_lang_th -> "th"
                R.id.chip_lang_en -> "en"
                else -> ""
            }
            if (tag != AppCompatDelegate.getApplicationLocales().toLanguageTags()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
    }

    private fun setupTheme() {
        val group = findViewById<ChipGroup>(R.id.theme_group)
        group.check(
            when (prefs.nightMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.chip_theme_light
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.chip_theme_dark
                else -> R.id.chip_theme_system
            }
        )
        group.setOnCheckedStateChangeListener { _, ids ->
            val mode = when (ids.firstOrNull()) {
                R.id.chip_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.chip_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (mode != prefs.nightMode) {
                prefs.nightMode = mode
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    private fun confirmDiscard() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.discard_title)
            .setMessage(R.string.discard_body)
            .setNegativeButton(R.string.keep_editing, null)
            .setPositiveButton(R.string.discard) { _, _ -> finish() }
            .show()
    }

    companion object {
        private const val KEY_DRAFT = "custom_draft"
        private val SOUNDS = listOf(
            Triple("chime", R.id.radio_chime, R.id.play_chime),
            Triple("bell", R.id.radio_bell, R.id.play_bell),
            Triple("beep", R.id.radio_beep, R.id.play_beep)
        )
    }
}
