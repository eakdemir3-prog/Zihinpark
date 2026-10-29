package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Ses ve titreşim efektleri. Ayarlar (ses/titreşim açık mı) [soundOn] ve [vibrationOn]
 * alanlarından okunur; MainActivity bunları kullanıcı tercihleriyle eşitler.
 */
class SoundAndVibeManager(context: Context) {

    @Volatile var soundOn: Boolean = true
    @Volatile var vibrationOn: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    } catch (_: Exception) {
        null
    }

    private val audioThread = HandlerThread("zp-audio").apply { start() }
    private val audio = Handler(audioThread.looper)

    /** Kısa tonları sırayla çalar (basit melodi). */
    private fun melody(vararg notes: Pair<Int, Int>) {
        if (!soundOn) return
        val gen = toneGenerator ?: return
        audio.removeCallbacksAndMessages(null)
        var delay = 0L
        for ((tone, ms) in notes) {
            audio.postDelayed({
                try { gen.startTone(tone, ms) } catch (_: Exception) {}
            }, delay)
            delay += ms + 25
        }
    }

    // ---- Eski API (geriye dönük uyum) ----
    fun playClickSound(soundEnabled: Boolean = true) { if (soundEnabled) playTap() }
    fun playSuccessSound(soundEnabled: Boolean = true) { if (soundEnabled) playCorrect() }
    fun playErrorSound(soundEnabled: Boolean = true) { if (soundEnabled) playWrong() }
    fun playTone(toneType: Int, durationMs: Int = 100, soundEnabled: Boolean = true) {
        if (soundEnabled) melody(toneType to durationMs)
    }
    fun vibrateShort(vibrationEnabled: Boolean = true) { if (vibrationEnabled) vibrate(longArrayOf(0, 18), intArrayOf(0, 90)) }
    fun vibrateSuccess(vibrationEnabled: Boolean = true) {
        if (vibrationEnabled) vibrate(longArrayOf(0, 30, 50, 50), intArrayOf(0, 120, 0, 220))
    }
    fun vibrateError(vibrationEnabled: Boolean = true) {
        if (vibrationEnabled) vibrate(longArrayOf(0, 70, 40, 70), intArrayOf(0, 200, 0, 200))
    }

    // ---- Oyun efektleri ----
    fun playTap() = melody(ToneGenerator.TONE_DTMF_1 to 25)

    fun playCorrect() {
        melody(ToneGenerator.TONE_DTMF_6 to 60, ToneGenerator.TONE_DTMF_9 to 90)
        vibrateShort()
    }

    fun playWrong() {
        melody(ToneGenerator.TONE_PROP_NACK to 160)
        vibrateError()
    }

    fun playRoundClear() {
        melody(ToneGenerator.TONE_DTMF_3 to 60, ToneGenerator.TONE_DTMF_6 to 60, ToneGenerator.TONE_DTMF_9 to 120)
        vibrateSuccess()
    }

    fun playStar(index: Int) {
        val tone = when (index) {
            0 -> ToneGenerator.TONE_DTMF_4
            1 -> ToneGenerator.TONE_DTMF_7
            else -> ToneGenerator.TONE_DTMF_D
        }
        melody(tone to 90)
        vibrateShort()
    }

    fun playLevelFail() {
        melody(ToneGenerator.TONE_DTMF_7 to 110, ToneGenerator.TONE_DTMF_4 to 110, ToneGenerator.TONE_DTMF_1 to 220)
    }

    fun playPad(index: Int) {
        val tones = intArrayOf(
            ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_5,
            ToneGenerator.TONE_DTMF_7, ToneGenerator.TONE_DTMF_9, ToneGenerator.TONE_DTMF_A,
            ToneGenerator.TONE_DTMF_B, ToneGenerator.TONE_DTMF_C, ToneGenerator.TONE_DTMF_D
        )
        melody(tones[index % tones.size] to 160)
    }

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        if (!vibrationOn || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings.sum())
            }
        } catch (_: Exception) {}
    }
}
