package com.example.games

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.R
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk

/** Uygulamadaki rozetlerin Play Games başarımlarına karşılık gelen ilerleme değerleri. */
data class AchievementStats(
    val cleared: Int,
    val gamesStarted: Int,
    val totalGames: Int,
    val bosses: Int,
    val totalStars: Int,
    val perfect: Int,
    val zonesMastered: Int,
    val endless: Int,
    val streak: Int
)

/**
 * Google Play Games Hizmetleri: otomatik giriş ve 12 başarım.
 * Oyuncu Play Games'e giriş yapmamışsa hiçbir şey yapmaz; uygulama her durumda normal çalışır.
 */
object GamesServices {
    private const val TAG = "ZP-Games"
    private const val RC_ACHIEVEMENTS = 9101
    @Volatile private var initialized = false
    /** Son gönderilen değerler: aynı ilerlemeyi tekrar tekrar göndermemek için. */
    private val sent = HashMap<Int, Int>()

    fun init(context: Context) {
        if (initialized) return
        runCatching { PlayGamesSdk.initialize(context.applicationContext); initialized = true }
            .onFailure { Log.w(TAG, "init", it) }
    }

    /** İlerlemeyi Play Games'e yansıtır (giriş yapılmamışsa sessizce çıkar). */
    fun sync(activity: Activity, s: AchievementStats) {
        if (!initialized) return
        runCatching {
            PlayGames.getGamesSignInClient(activity).isAuthenticated.addOnCompleteListener { t ->
                if (t.isSuccessful && t.result?.isAuthenticated == true) push(activity, s)
            }
        }.onFailure { Log.w(TAG, "sync", it) }
    }

    private fun push(activity: Activity, s: AchievementStats) {
        val c = PlayGames.getAchievementsClient(activity)
        fun unlock(id: Int, done: Boolean) {
            if (done && sent[id] != 1) { c.unlock(activity.getString(id)); sent[id] = 1 }
        }
        fun steps(id: Int, value: Int, goal: Int) {
            val v = value.coerceAtMost(goal)
            // setSteps yalnızca artırır; 0 gönderilemez
            if (v > 0 && (sent[id] ?: 0) < v) { c.setSteps(activity.getString(id), v); sent[id] = v }
        }
        runCatching {
            unlock(R.string.achievement_first_step, s.cleared >= 1)
            steps(R.string.achievement_explorer, s.gamesStarted, 10)
            steps(R.string.achievement_full_circle, s.gamesStarted, minOf(30, s.totalGames))
            steps(R.string.achievement_boss_hunter, s.bosses, 10)
            steps(R.string.achievement_boss_legend, s.bosses, 50)
            steps(R.string.achievement_star_collector, s.totalStars, 100)
            steps(R.string.achievement_constellation, s.totalStars, 1000)
            steps(R.string.achievement_flawless, s.perfect, 25)
            unlock(R.string.achievement_zone_master, s.zonesMastered >= 1)
            unlock(R.string.achievement_eternity, s.endless >= 1)
            steps(R.string.achievement_dedicated, s.streak, 7)
            steps(R.string.achievement_mind_legend, s.cleared, 1500)
        }.onFailure { Log.w(TAG, "push", it) }
    }

    /**
     * Play Games başarımlar ekranını açar. Oyuncu giriş yapmamışsa önce giriş ister.
     * [onUnavailable]: Play Games kullanılamıyorsa (giriş iptal vb.) çağrılır.
     */
    fun showAchievements(activity: Activity, onUnavailable: () -> Unit) {
        if (!initialized) init(activity)
        runCatching {
            val signIn = PlayGames.getGamesSignInClient(activity)
            signIn.isAuthenticated.addOnCompleteListener { t ->
                if (t.isSuccessful && t.result?.isAuthenticated == true) open(activity, onUnavailable)
                else signIn.signIn().addOnCompleteListener { r ->
                    if (r.isSuccessful && r.result?.isAuthenticated == true) open(activity, onUnavailable) else onUnavailable()
                }
            }
        }.onFailure { onUnavailable() }
    }

    private fun open(activity: Activity, onUnavailable: () -> Unit) {
        PlayGames.getAchievementsClient(activity).achievementsIntent
            .addOnSuccessListener { activity.startActivityForResult(it, RC_ACHIEVEMENTS) }
            .addOnFailureListener { onUnavailable() }
    }
}
