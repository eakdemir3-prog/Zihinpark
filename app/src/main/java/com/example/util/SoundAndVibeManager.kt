package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import com.example.domain.model.GameRegistry
import kotlin.math.pow

/**
 * Ses ve titreşim efektleri. Sesler uygulamaya gömülü, sentezlenmiş enstrüman örnekleridir
 * (marimba, çan, telli, synth, 8-bit, kalimba, cam, tahta). Her oyun kendi enstrümanını ve
 * ton yüksekliğini kullanır; seviye/tur/yıldız/hata sesleri ortaktır ve kolay tanınır.
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

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val main = Handler(Looper.getMainLooper())

    private val taps = intArrayOf(R.raw.t0_tap, R.raw.t1_tap, R.raw.t2_tap, R.raw.t3_tap, R.raw.t4_tap, R.raw.t5_tap, R.raw.t6_tap, R.raw.t7_tap)
        .map { pool.load(context, it, 1) }
    private val oks = intArrayOf(R.raw.t0_ok, R.raw.t1_ok, R.raw.t2_ok, R.raw.t3_ok, R.raw.t4_ok, R.raw.t5_ok, R.raw.t6_ok, R.raw.t7_ok)
        .map { pool.load(context, it, 1) }
    private val notes = intArrayOf(R.raw.t0_note, R.raw.t1_note, R.raw.t2_note, R.raw.t3_note, R.raw.t4_note, R.raw.t5_note, R.raw.t6_note, R.raw.t7_note)
        .map { pool.load(context, it, 1) }
    private val sErr = pool.load(context, R.raw.err, 1)
    private val sRound = pool.load(context, R.raw.round_clear, 1)
    private val sWin = pool.load(context, R.raw.level_win, 1)
    private val sFail = pool.load(context, R.raw.level_fail, 1)
    private val sStar = pool.load(context, R.raw.star, 1)
    private val sCoin = pool.load(context, R.raw.coin, 1)

    // ---- Oyuna özel tını ----
    private var instrument = 0
    private var keyRate = 1f

    /** Oyuna girerken çağrılır: her oyunun enstrüman + ton birleşimi farklıdır. */
    fun useGameTheme(gameId: String?) {
        val i = GameRegistry.ALL_GAMES.indexOfFirst { it.id == gameId }
        if (i < 0) { instrument = 0; keyRate = 1f; return }
        instrument = i % taps.size
        val shift = intArrayOf(0, 3, -2, 5)[(i / taps.size) % 4]
        keyRate = semis(shift)
    }

    private fun semis(n: Int) = 2f.pow(n / 12f)

    private fun play(id: Int, vol: Float = 1f, rate: Float = 1f) {
        if (!soundOn) return
        try { pool.play(id, vol, vol, 1, 0, rate.coerceIn(0.5f, 2f)) } catch (_: Exception) {}
    }

    // ---- Eski API (geriye dönük uyum) ----
    fun playClickSound(soundEnabled: Boolean = true) { if (soundEnabled) playTap() }
    fun playSuccessSound(soundEnabled: Boolean = true) { if (soundEnabled) playCorrect() }
    fun playErrorSound(soundEnabled: Boolean = true) { if (soundEnabled) playWrong() }
    @Suppress("UNUSED_PARAMETER")
    fun playTone(toneType: Int, durationMs: Int = 100, soundEnabled: Boolean = true) { if (soundEnabled) playTap() }
    fun vibrateShort(vibrationEnabled: Boolean = true) { if (vibrationEnabled) vibrate(longArrayOf(0, 18), intArrayOf(0, 90)) }
    fun vibrateSuccess(vibrationEnabled: Boolean = true) {
        if (vibrationEnabled) vibrate(longArrayOf(0, 30, 50, 50), intArrayOf(0, 120, 0, 220))
    }
    fun vibrateError(vibrationEnabled: Boolean = true) {
        if (vibrationEnabled) vibrate(longArrayOf(0, 70, 40, 70), intArrayOf(0, 200, 0, 200))
    }

    // ---- Oyun efektleri ----
    fun playTap() = play(taps[instrument], 0.55f, keyRate)

    fun playCorrect() {
        play(oks[instrument], 0.9f, keyRate)
        vibrateShort()
    }

    fun playWrong() {
        play(sErr, 0.9f)
        vibrateError()
    }

    fun playRoundClear() {
        play(sRound, 1f)
        vibrateSuccess()
    }

    /** Sonuç ekranında yıldızlar tek tek düşerken; her yıldız biraz daha tiz. */
    fun playStar(index: Int) {
        play(sStar, 0.8f, semis(intArrayOf(0, 4, 7)[index.coerceIn(0, 2)]))
        vibrateShort()
    }

    fun playLevelWin() {
        play(sWin, 1f)
        vibrate(longArrayOf(0, 40, 60, 40, 60, 90), intArrayOf(0, 140, 0, 180, 0, 255))
    }

    fun playLevelFail() = play(sFail, 0.9f)

    fun playCoin() = play(sCoin, 0.8f)

    /** Hafıza Işıkları gibi oyunlarda her ped kendi notasını çalar (pentatonik). */
    fun playPad(index: Int) {
        val steps = intArrayOf(-10, -7, -5, -3, 0, 2, 5, 7, 9)
        play(notes[instrument], 0.9f, semis(steps[index % steps.size]))
    }

    /** Küçük gecikmeli çalma (ör. fanfar yıldızlardan sonra). */
    fun later(ms: Long, block: SoundAndVibeManager.() -> Unit) { main.postDelayed({ block() }, ms) }

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
