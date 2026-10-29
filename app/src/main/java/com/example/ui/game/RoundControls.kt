package com.example.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.game.engine.RoundSpec
import com.example.util.SoundAndVibeManager
import kotlinx.coroutines.delay

/**
 * Bir oyunun tur içeriği. Oyun bitince [finish] (başarı, kalite 0..100) çağrılır.
 */
typealias RoundContent = @Composable (spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) -> Unit

/**
 * Tur sırasında oyun ile çerçeve (üst bar, süre, can, güçlendirmeler, efektler) arasındaki köprü.
 */
class RoundControls(val fx: GameFxState, val sound: SoundAndVibeManager) {

    // ---- Süre ----
    var timeLimitMs by mutableLongStateOf(0L)
        private set
    var timeLeftMs by mutableLongStateOf(0L)
        internal set
    var timerRunning by mutableStateOf(false)
        private set
    internal var onTimeout: (() -> Unit)? = null

    /** Çerçeve duraklatıldığında (menü/öğretici açıkken) true olur; oyunlar animasyonlarını bekletebilir. */
    var paused by mutableStateOf(false)
        internal set

    fun startTimer(seconds: Int, onTimeout: () -> Unit) {
        timeLimitMs = seconds * 1000L
        timeLeftMs = timeLimitMs
        this.onTimeout = onTimeout
        timerRunning = true
    }

    fun stopTimer() {
        timerRunning = false
    }

    fun addTime(seconds: Int) {
        if (timeLimitMs <= 0) return
        timeLeftMs += seconds * 1000L
        timeLimitMs += seconds * 1000L
    }

    /** Kullanılan süre oranı (0 = hiç, 1 = tamamı). Süre yoksa 0.5. */
    val timeUsedRatio: Float
        get() = if (timeLimitMs <= 0) 0.5f else 1f - (timeLeftMs.toFloat() / timeLimitMs).coerceIn(0f, 1f)

    // ---- Can / hata ----
    var mistakes by mutableIntStateOf(0)
        private set
    var maxMistakes by mutableIntStateOf(0)
        private set

    fun setLives(max: Int) {
        maxMistakes = max
    }

    /** Hata kaydeder, efekt oynatır. Can bittiyse true döner. */
    fun mistake(label: String? = null): Boolean {
        mistakes++
        fx.wrong(label)
        sound.playWrong()
        return maxMistakes > 0 && mistakes >= maxMistakes
    }

    fun correct(label: String? = null) {
        fx.correct(label)
        sound.playCorrect()
    }

    fun tap() = sound.playTap()

    // ---- Durum yazısı ve ipucu ----
    var statusText by mutableStateOf<String?>(null)

    /** İpucu desteği olan oyunlar buraya bir eylem bağlar. */
    var hint by mutableStateOf<(() -> Unit)?>(null)

    /** Geri alma desteği olan oyunlar buraya bir eylem bağlar. */
    var undo by mutableStateOf<(() -> Unit)?>(null)

    internal fun resetForRound() {
        timeLimitMs = 0L
        timeLeftMs = 0L
        timerRunning = false
        onTimeout = null
        mistakes = 0
        maxMistakes = 0
        statusText = null
        hint = null
        undo = null
    }
}

/** Çerçevenin süre sayacını çalıştırır. */
@Composable
internal fun TimerDriver(ctl: RoundControls) {
    val running = ctl.timerRunning && !ctl.paused
    val latest by rememberUpdatedState(ctl)
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = System.nanoTime()
        while (latest.timerRunning && !latest.paused) {
            delay(50)
            val now = System.nanoTime()
            val dt = (now - last) / 1_000_000
            last = now
            latest.timeLeftMs = (latest.timeLeftMs - dt).coerceAtLeast(0)
            if (latest.timeLeftMs <= 0L) {
                latest.stopTimer()
                latest.onTimeout?.invoke()
                break
            }
        }
    }
}
