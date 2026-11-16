package com.example.pokatop.ui

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pokatop.R
import com.example.pokatop.data.PokatopDbHelper
import com.example.pokatop.data.Prefs
import com.example.pokatop.sensing.Detectors
import com.example.pokatop.sensing.Posture
import com.example.pokatop.sensing.PostureDetector
import com.example.pokatop.timer.DailyStats
import com.example.pokatop.timer.FocusSession
import com.example.pokatop.timer.Phase
import com.example.pokatop.timer.SessionAlert
import com.example.pokatop.timer.SessionCodec
import com.example.pokatop.timer.SessionState
import com.example.pokatop.util.Format
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var prefs: Prefs
    private lateinit var session: FocusSession
    private lateinit var alertPlayer: AlertPlayer
    private lateinit var db: PokatopDbHelper
    private var savedTodayMs = 0L
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val detector: PostureDetector = Detectors.create()
    private var posture = Posture.UNKNOWN
    private val handler = Handler(Looper.getMainLooper())
    private var lastBackAt = 0L
    private var alertUntil = 0L
    private var loggedPhase: Phase? = null
    private var overlayShown = false

    private lateinit var ring: TimerRingView
    private lateinit var presetLabel: TextView
    private lateinit var statusCard: LinearLayout
    private lateinit var statusIcon: ImageView
    private lateinit var statusTitle: TextView
    private lateinit var statusBody: TextView
    private lateinit var distractionsValue: TextView
    private lateinit var todayValue: TextView
    private lateinit var btnPrimary: MaterialButton
    private lateinit var btnEnd: MaterialButton
    private lateinit var navButtons: List<ImageButton>
    private lateinit var alertCard: View
    private lateinit var alertTitle: TextView
    private lateinit var alertBody: TextView
    private lateinit var overlay: View

    private val ticker = object : Runnable {
        override fun run() {
            onTick()
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val hasAccelerometer: Boolean
        get() = accelerometer != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<ViewGroup>(R.id.main).getChildAt(0).applySystemBarPadding()
        prefs = Prefs(this)
        alertPlayer = AlertPlayer(this)
        db = PokatopDbHelper(this)
        sensorManager = getSystemService(SensorManager::class.java)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        ring = findViewById(R.id.ring)
        presetLabel = findViewById(R.id.preset_label)
        statusCard = findViewById(R.id.status_card)
        statusIcon = findViewById(R.id.status_icon)
        statusTitle = findViewById(R.id.status_title)
        statusBody = findViewById(R.id.status_body)
        distractionsValue = findViewById(R.id.distractions_value)
        todayValue = findViewById(R.id.today_value)
        btnPrimary = findViewById(R.id.btn_primary)
        btnEnd = findViewById(R.id.btn_end)
        navButtons = listOf(findViewById(R.id.btn_stats), findViewById(R.id.btn_history), findViewById(R.id.btn_settings))
        alertCard = findViewById(R.id.alert_card)
        alertTitle = findViewById(R.id.alert_title)
        alertBody = findViewById(R.id.alert_body)
        overlay = findViewById(R.id.face_down_overlay)

        val restored = savedInstanceState?.getString(KEY_STATE)?.let(SessionCodec::decode)
            ?: SessionCodec.decode(prefs.pendingSession)
        if (restored == null) prefs.pendingSession = null
        session = FocusSession(restored ?: freshState())

        btnPrimary.setOnClickListener { onPrimaryClick() }
        btnEnd.setOnClickListener { confirmEnd() }
        findViewById<View>(R.id.btn_stats).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        findViewById<View>(R.id.btn_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<View>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })
    }

    override fun onResume() {
        super.onResume()
        if (!session.isInProgress) session = FocusSession(freshState())
        val now = SystemClock.elapsedRealtime()
        session.onReturn(now)
        loadSavedToday()
        detector.reset()
        posture = Posture.UNKNOWN
        if (session.state.config.detection) {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
        handler.post(ticker)
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(ticker)
        alertPlayer.stop()
        if (!isChangingConfigurations) {
            val before = session.state.distractions
            session.onLeave(SystemClock.elapsedRealtime())
            if (session.state.distractions > before) Log.d(TAG, "distraction reason=leave")
            prefs.pendingSession = if (session.isInProgress) SessionCodec.encode(session.state) else null
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_STATE, SessionCodec.encode(session.state))
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = SystemClock.elapsedRealtime()
        val p = detector.onSample(event.values[0], event.values[1], event.values[2], now)
        if (p == posture || p == Posture.UNKNOWN) return
        posture = p
        Log.d(TAG, "posture=$p")
        session.onPosture(p, now, System.currentTimeMillis())
        render(now)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun loadSavedToday() {
        val todayStart = DailyStats.startOfDay(System.currentTimeMillis())
        savedTodayMs = DailyStats.summarize(db.getSessionsSince(todayStart), todayStart).focusMs
    }

    private fun freshState() = SessionState(prefs.currentConfig(hasAccelerometer))

    private fun onTick() {
        val now = SystemClock.elapsedRealtime()
        val before = session.state.distractions
        val alerts = session.tick(now)
        if (session.state.distractions > before) Log.d(TAG, "distraction reason=grace")
        alerts.forEach { showAlert(it, now) }
        render(now)
    }

    private fun onPrimaryClick() {
        val now = SystemClock.elapsedRealtime()
        when (session.state.phase) {
            Phase.WAITING -> session.start(now, System.currentTimeMillis())
            Phase.FOCUSING -> session.pause(now)
            Phase.PAUSED -> session.resume(now)
            else -> Unit
        }
        render(now)
    }

    private fun showAlert(alert: SessionAlert, now: Long) {
        val c = session.state.config
        when (alert) {
            SessionAlert.FOCUS_DONE -> {
                alertTitle.text = getString(R.string.alert_focus_done_title, c.focusMin)
                alertBody.text = if (session.state.isLongBreak) {
                    getString(R.string.alert_long_break_body, c.longBreakMin)
                } else {
                    getString(R.string.alert_short_break_body, c.shortBreakMin)
                }
            }
            SessionAlert.BREAK_DONE -> {
                alertTitle.setText(R.string.alert_break_done_title)
                alertBody.setText(if (c.detection) R.string.alert_break_done_flip_body else R.string.alert_break_done_plain_body)
            }
        }
        alertUntil = now + ALERT_MS
        alertPlayer.play(prefs.soundKey, prefs.vibrate)
    }

    private fun endSession() {
        val now = SystemClock.elapsedRealtime()
        val record = session.toRecord(now, System.currentTimeMillis())
        if (record == null) {
            Toast.makeText(this, R.string.not_saved_short, Toast.LENGTH_SHORT).show()
        } else {
            db.insertSession(record)
            loadSavedToday()
            Toast.makeText(
                this,
                getString(R.string.saved_fmt, Format.duration(this, record.focusMs), resources.getQuantityString(R.plurals.rounds_count, record.rounds, record.rounds)),
                Toast.LENGTH_SHORT
            ).show()
        }
        session = FocusSession(freshState())
        prefs.pendingSession = null
        alertUntil = 0
        render(now)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (session.state.config.detection && (overlayShown || detector.screenDown)) return true
        return super.dispatchTouchEvent(ev)
    }

    private fun confirmEnd() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.end_title)
            .setMessage(R.string.end_body)
            .setPositiveButton(R.string.end_confirm) { _, _ -> endSession() }
            .setNegativeButton(R.string.end_keep_going, null)
            .show()
    }

    private fun onBack() {
        if (session.isInProgress) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_body)
                .setPositiveButton(R.string.exit_end_save) { _, _ ->
                    endSession()
                    finish()
                }
                .setNeutralButton(R.string.exit_keep) { _, _ -> finish() }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackAt <= DOUBLE_BACK_MS) {
            finish()
        } else {
            lastBackAt = now
            Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_SHORT).show()
        }
    }

    private fun render(now: Long) {
        val s = session.state
        val c = s.config
        if (s.phase != loggedPhase) {
            Log.d(TAG, "phase=${s.phase}")
            loggedPhase = s.phase
        }
        val inBreak = s.phase == Phase.BREAK || s.phase == Phase.BREAK_PAUSED

        if (inBreak) {
            val total = if (s.isLongBreak) c.longBreakMs else c.shortBreakMs
            val left = session.breakRemainingMs(now)
            ring.ringColor = ContextCompat.getColor(this, R.color.pk_pastel)
            ring.progress = 1f - left.toFloat() / total
            ring.centerText = Format.clock(left)
            ring.subText = getString(if (s.isLongBreak) R.string.status_long_break else R.string.status_short_break)
        } else {
            val elapsed = session.roundElapsedMs(now)
            ring.ringColor = ContextCompat.getColor(this, R.color.pk_mint)
            ring.progress = elapsed.toFloat() / c.focusMs
            ring.centerText = Format.clock(c.focusMs - elapsed)
            ring.subText = if (session.isInProgress) {
                getString(R.string.round_sub_fmt, s.completedRounds + 1)
            } else {
                getString(R.string.ring_focus_sub_fmt, c.focusMin)
            }
        }

        presetLabel.text = getString(
            R.string.preset_label_fmt,
            getString(presetNameRes(c.presetKey)),
            c.focusMin,
            c.shortBreakMin,
            session.roundsUntilLongBreak()
        )

        renderStatus(now)

        distractionsValue.text = if (c.detection) {
            resources.getQuantityString(R.plurals.times_count, s.distractions, s.distractions)
        } else {
            getString(R.string.distractions_none)
        }
        todayValue.text = Format.duration(this, savedTodayMs + session.focusSoFarMs(now))

        val primaryText = when {
            c.detection -> null
            s.phase == Phase.WAITING -> if (session.isInProgress) R.string.btn_next_round else R.string.btn_start
            s.phase == Phase.FOCUSING -> R.string.btn_pause
            s.phase == Phase.PAUSED -> R.string.btn_resume
            else -> null
        }
        btnPrimary.visibility = if (primaryText == null) View.GONE else View.VISIBLE
        if (primaryText != null) btnPrimary.setText(primaryText)
        btnEnd.visibility = if (session.isInProgress) View.VISIBLE else View.GONE

        val navEnabled = s.phase != Phase.FOCUSING && s.phase != Phase.GRACE
        navButtons.forEach {
            it.isEnabled = navEnabled
            it.alpha = if (navEnabled) 1f else 0.35f
        }

        alertCard.visibility = if (now < alertUntil) View.VISIBLE else View.GONE

        setOverlay(c.detection && posture == Posture.FACE_DOWN)
        if (c.detection || session.isInProgress) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun renderStatus(now: Long) {
        val s = session.state
        val detection = s.config.detection
        var warn = false
        var showIcon = false
        val title: String
        val body: String
        when (s.phase) {
            Phase.BREAK, Phase.BREAK_PAUSED -> {
                title = getString(if (s.isLongBreak) R.string.status_long_break else R.string.status_short_break)
                body = getString(R.string.status_break_body)
            }
            Phase.WAITING -> {
                showIcon = detection
                title = when {
                    session.isInProgress -> getString(R.string.status_ready_round, s.completedRounds + 1)
                    detection -> getString(R.string.status_flip_to_start)
                    else -> getString(R.string.status_ready_plain)
                }
                body = when {
                    detection && session.isInProgress -> getString(R.string.status_ready_flip_body)
                    detection -> getString(R.string.status_flip_to_start_body)
                    else -> getString(R.string.status_ready_plain_body)
                }
            }
            Phase.FOCUSING -> {
                title = getString(R.string.status_focusing)
                body = getString(if (detection) R.string.status_focusing_flip_body else R.string.status_focusing_plain_body)
            }
            Phase.GRACE -> {
                warn = true
                val seconds = ((session.graceRemainingMs(now) + 999) / 1000).toInt()
                title = getString(R.string.status_grace, seconds)
                body = getString(R.string.status_grace_body)
            }
            Phase.PAUSED -> {
                showIcon = detection
                title = getString(R.string.status_paused)
                body = getString(if (detection) R.string.status_paused_flip_body else R.string.status_paused_tap_body)
            }
        }
        statusTitle.text = title
        statusBody.text = body
        statusIcon.visibility = if (showIcon) View.VISIBLE else View.GONE
        statusCard.setBackgroundResource(if (warn) R.drawable.bg_status_warn else R.drawable.bg_card)
        val fg = ContextCompat.getColor(this, if (warn) R.color.pk_btn2_text else R.color.pk_text)
        statusTitle.setTextColor(fg)
        statusBody.setTextColor(fg)
    }

    private fun setOverlay(show: Boolean) {
        if (show == overlayShown) return
        overlayShown = show
        overlay.visibility = if (show) View.VISIBLE else View.GONE
        val attrs = window.attributes
        attrs.screenBrightness = if (show) DIM_BRIGHTNESS else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = attrs
    }

    companion object {
        const val KEY_STATE = "session_state"
        private const val TAG = "Pokatop"
        private const val TICK_MS = 250L
        private const val ALERT_MS = 4500L
        private const val DOUBLE_BACK_MS = 2000L
        private const val DIM_BRIGHTNESS = 0.01f

        fun presetNameRes(key: String): Int = when (key) {
            "short" -> R.string.preset_short
            "long" -> R.string.preset_long
            "p5217" -> R.string.preset_p5217
            "max" -> R.string.preset_max
            "custom" -> R.string.preset_custom
            else -> R.string.preset_classic
        }
    }
}
