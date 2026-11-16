package com.example.pokatop.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.pokatop.R

fun soundRes(key: String): Int = when (key) {
    "bell" -> R.raw.bell
    "beep" -> R.raw.beep
    else -> R.raw.chime
}

class AlertPlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun play(soundKey: String, vibrate: Boolean) {
        start(soundKey, AudioAttributes.USAGE_ALARM)
        if (vibrate) vibrateOnce()
    }

    fun preview(soundKey: String) {
        start(soundKey, AudioAttributes.USAGE_MEDIA)
    }

    fun stop() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "stop failed", e)
            }
            it.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun start(soundKey: String, usage: Int) {
        stop()
        val attributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val mp = MediaPlayer()
        try {
            context.resources.openRawResourceFd(soundRes(soundKey)).use { afd ->
                mp.setAudioAttributes(attributes)
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.setOnCompletionListener { done ->
                done.release()
                if (player === done) player = null
            }
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            Log.w(TAG, "tone failed", e)
            mp.release()
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateOnce() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(PATTERN, -1))
        } else {
            v.vibrate(PATTERN, -1)
        }
        vibrator = v
    }

    companion object {
        private const val TAG = "Pokatop"
        private val PATTERN = longArrayOf(0, 400, 200, 400)
    }
}
