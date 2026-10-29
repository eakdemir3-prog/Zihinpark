package com.example.cloud

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.data.local.ZihinParkDatabase
import com.example.data.local.entity.LevelProgressEntity
import com.example.data.pref.UserPreferencesRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

data class CloudUser(val uid: String, val name: String, val email: String?, val photo: String?)

data class LeagueEntry(val uid: String, val name: String, val avatar: Int, val frame: String, val score: Long)

data class LeagueBoard(val weekId: String, val top: List<LeagueEntry>, val myRank: Int?, val myScore: Long)

data class WeeklyReward(val weekId: String, val rank: Int, val diamonds: Int)

/**
 * Firebase katmanı: Google ile giriş, bulut yedek ve haftalık lig.
 * google-services.json yoksa [isConfigured] false olur ve uygulama çevrimdışı çalışmaya devam eder.
 */
object CloudSync {
    private const val TAG = "ZP-Cloud"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    /** Lig ödülleri: sıra -> elmas. */
    val REWARDS: Map<Int, Int> = (1..10).associateWith { r -> when (r) { 1 -> 500; 2 -> 300; 3 -> 150; else -> 50 } }

    private val _user = MutableStateFlow<CloudUser?>(null)
    val user: StateFlow<CloudUser?> = _user.asStateFlow()

    private val _syncState = MutableStateFlow("")
    val syncState: StateFlow<String> = _syncState.asStateFlow()

    private val _pendingReward = MutableStateFlow<WeeklyReward?>(null)
    val pendingReward: StateFlow<WeeklyReward?> = _pendingReward.asStateFlow()

    var isConfigured: Boolean = false
        private set

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    fun init(context: Context) {
        appContext = context.applicationContext
        isConfigured = try {
            FirebaseApp.getApps(appContext).isNotEmpty() || FirebaseApp.initializeApp(appContext) != null
        } catch (t: Throwable) {
            Log.w(TAG, "Firebase yapılandırılmamış: ${t.message}")
            false
        }
        if (!isConfigured) return
        auth.currentUser?.let { u ->
            _user.value = CloudUser(u.uid, u.displayName ?: "Oyuncu", u.email, u.photoUrl?.toString())
            scope.launch { afterSignIn() }
        }
    }

    private fun webClientId(): String? {
        val id = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        return if (id != 0) appContext.getString(id) else null
    }

    /** Google ile giriş. Başarılıysa null, değilse kullanıcıya gösterilecek hata mesajı döner. */
    suspend fun signIn(activity: Activity): String? {
        if (!isConfigured) return "Bulut bağlantısı henüz kurulmadı."
        val clientId = webClientId() ?: return "Google girişi yapılandırılmamış."
        val cm = CredentialManager.create(activity)
        val token = try {
            val quick = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()
            val result = try {
                cm.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(quick).build())
            } catch (e: Exception) {
                // Hesap listesi gösterilemezse tam ekran Google girişine düş
                val full = GetSignInWithGoogleOption.Builder(clientId).build()
                cm.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(full).build())
            }
            val cred = result.credential
            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleIdTokenCredential.createFrom(cred.data).idToken
            } else return "Google hesabı alınamadı."
        } catch (e: Exception) {
            Log.w(TAG, "signIn", e)
            val msg = e.localizedMessage.orEmpty()
            return when {
                // Kullanıcı pencereyi kendisi kapattıysa
                e.javaClass.simpleName.contains("Cancel") && (msg.isBlank() || msg.contains("cancel", true)) -> "Giriş iptal edildi."
                // [16] / [28444]: SHA-1 ya da OAuth ayarı uyuşmazlığı
                msg.contains("16") || msg.contains("28444") || msg.contains("reauth", true) || msg.contains("Developer console", true) ->
                    "Google girişi bu sürümde henüz etkin değil (kod: ${msg.take(60)}). Biraz sonra tekrar dene."
                else -> "Giriş yapılamadı: ${e.javaClass.simpleName} ${msg.take(80)}"
            }
        }
        return try {
            val res = auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
            val u = res.user ?: return "Giriş yapılamadı."
            _user.value = CloudUser(u.uid, u.displayName ?: "Oyuncu", u.email, u.photoUrl?.toString())
            afterSignIn()
            null
        } catch (e: Exception) {
            Log.w(TAG, "firebase signIn", e)
            "Giriş yapılamadı: ${e.localizedMessage}"
        }
    }

    /**
     * Hesabı ve buluttaki tüm ZihinPark verilerini kalıcı olarak siler (Play hesap silme şartı).
     * Hata yoksa null, varsa kullanıcıya gösterilecek mesaj döner.
     */
    suspend fun deleteAccount(activity: android.app.Activity): String? {
        if (!isConfigured) return "Bulut hesabı yok"
        val u = auth.currentUser ?: return "Giriş yapılmamış"
        return try {
            val uid = u.uid
            // Son haftaların lig kayıtları + kullanıcı belgesi
            for (w in 0 downTo -12) runCatching { scores(weekId(w)).document(uid).delete().await() }
            runCatching { db.collection("zp_users").document(uid).delete().await() }
            try {
                u.delete().await()
            } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                // Güvenlik gereği yakın zamanda giriş gerekir: yeniden giriş yapıp tekrar dene
                val err = signIn(activity)
                if (err != null) return err
                auth.currentUser?.delete()?.await()
            }
            auth.signOut()
            _user.value = null
            _syncState.value = ""
            null
        } catch (e: Exception) {
            "Silinemedi: ${e.message ?: "bilinmeyen hata"}"
        }
    }

    fun signOut() {
        if (!isConfigured) return
        auth.signOut()
        _user.value = null
        _syncState.value = ""
    }

    // ------------------------------------------------------------------
    // Bulut yedek
    // ------------------------------------------------------------------

    private suspend fun afterSignIn() {
        try {
            _syncState.value = "Eşitleniyor..."
            val restored = restoreIfCloudIsAhead()
            if (!restored) backupNow()
            _syncState.value = if (restored) "Buluttan geri yüklendi" else "Yedeklendi"
            checkWeeklyReward()
            ensureLeagueEntry()
        } catch (e: Exception) {
            Log.w(TAG, "afterSignIn", e)
            _syncState.value = "Eşitleme hatası"
        }
    }

    private var backupJob: Job? = null

    /** Değişiklikten sonra 20 sn içinde tek bir yedekleme yapar (gereksiz trafiği önler). */
    fun scheduleBackup() {
        if (!isConfigured || _user.value == null) return
        backupJob?.cancel()
        backupJob = scope.launch {
            delay(20_000)
            runCatching { backupNow() }
        }
    }

    /** Uygulama arka plana geçince bekleyen yedeği hemen gönderir. */
    fun flush() {
        if (backupJob?.isActive == true) {
            backupJob?.cancel()
            scope.launch { runCatching { backupNow() } }
        }
    }

    private suspend fun snapshot(): Map<String, Any> {
        val dbLocal = ZihinParkDatabase.getInstance(appContext)
        val prefs = UserPreferencesRepository(appContext)
        val levels = dbLocal.levelProgressDao().getAllLevels().first()
        val games = dbLocal.gameDao().getAllGamesSync()
        return mapOf(
            "name" to prefs.userName.first(),
            "avatar" to prefs.avatarId.first(),
            "frame" to prefs.equippedFrame.first(),
            "diamonds" to prefs.diamondBalance.first(),
            "hints" to prefs.hintsCount.first(),
            "times" to prefs.extraTimeCount.first(),
            "undos" to prefs.undosCount.first(),
            "owned" to prefs.ownedCosmetics.first().toList(),
            "levels" to levels.filter { it.stars > 0 || it.attempts > 0 }.associate {
                "${it.gameId}|${it.levelNumber}" to listOf(it.stars, it.bestScore, if (it.isRewardClaimed) 1 else 0, if (it.perfectBonusClaimed) 1 else 0, it.attempts)
            },
            "endless" to games.filter { it.endlessBest > 0 }.associate { it.id to it.endlessBest },
            "totalStars" to levels.sumOf { it.stars },
            "wowLevel" to prefs.wowLevel.first(),
            "updatedAt" to System.currentTimeMillis()
        )
    }

    suspend fun backupNow() {
        val u = _user.value ?: return
        db.collection("zp_users").document(u.uid).set(snapshot(), SetOptions.merge()).await()
        _syncState.value = "Yedeklendi"
    }

    /** Buluttaki ilerleme daha fazlaysa (yıldız) yereli onunla birleştirir. */
    @Suppress("UNCHECKED_CAST")
    private suspend fun restoreIfCloudIsAhead(): Boolean {
        val u = _user.value ?: return false
        val doc = db.collection("zp_users").document(u.uid).get().await()
        if (!doc.exists()) return false
        val cloudStars = (doc.getLong("totalStars") ?: 0L).toInt()
        val dbLocal = ZihinParkDatabase.getInstance(appContext)
        val dao = dbLocal.levelProgressDao()
        val local = dao.getAllLevels().first()
        val localStars = local.sumOf { it.stars }
        // Kelime Diyarı seviyesi her durumda ileri alınır
        val prefsW = UserPreferencesRepository(appContext)
        val cloudWow = (doc.getLong("wowLevel") ?: 0L).toInt()
        if (cloudWow > prefsW.wowLevel.first()) prefsW.setWowLevel(cloudWow)
        if (cloudStars <= localStars) return false
        // Seviyeleri birleştir: her seviyede en iyisini tut
        val levels = doc.get("levels") as? Map<String, List<Long>> ?: emptyMap()
        val localMap = local.associateBy { "${it.gameId}|${it.levelNumber}" }
        for ((key, v) in levels) {
            val (gameId, lvl) = key.split("|").let { it[0] to it[1].toInt() }
            val ex = localMap[key]
            dao.insertOrUpdateLevel(
                LevelProgressEntity(
                    id = ex?.id ?: 0, gameId = gameId, levelNumber = lvl, isUnlocked = true,
                    stars = maxOf(ex?.stars ?: 0, v.getOrNull(0)?.toInt() ?: 0),
                    bestScore = maxOf(ex?.bestScore ?: 0, v.getOrNull(1)?.toInt() ?: 0),
                    isRewardClaimed = (ex?.isRewardClaimed == true) || v.getOrNull(2) == 1L,
                    completedAt = ex?.completedAt ?: System.currentTimeMillis(),
                    perfectBonusClaimed = (ex?.perfectBonusClaimed == true) || v.getOrNull(3) == 1L,
                    attempts = maxOf(ex?.attempts ?: 0, v.getOrNull(4)?.toInt() ?: 0)
                )
            )
        }
        val endless = doc.get("endless") as? Map<String, Long> ?: emptyMap()
        for ((g, best) in endless) dbLocal.gameDao().updateEndlessBest(g, best.toInt())
        val prefs = UserPreferencesRepository(appContext)
        prefs.restoreFromCloud(
            name = doc.getString("name"),
            avatar = doc.getLong("avatar")?.toInt(),
            frame = doc.getString("frame"),
            diamonds = doc.getLong("diamonds")?.toInt(),
            hints = doc.getLong("hints")?.toInt(),
            times = doc.getLong("times")?.toInt(),
            undos = doc.getLong("undos")?.toInt(),
            owned = (doc.get("owned") as? List<String>)?.toSet()
        )
        return true
    }

    // ------------------------------------------------------------------
    // Haftalık lig
    // ------------------------------------------------------------------

    private val tz: TimeZone = TimeZone.getTimeZone("Europe/Istanbul")

    /** Pazartesi başlayan hafta kimliği, ör. "2026-W44". */
    fun weekId(offsetWeeks: Int = 0, now: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance(tz).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = now
            add(Calendar.WEEK_OF_YEAR, offsetWeeks)
        }
        val week = c.get(Calendar.WEEK_OF_YEAR)
        var year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        if (week >= 52 && month == Calendar.JANUARY) year--
        if (week == 1 && month == Calendar.DECEMBER) year++
        return "%d-W%02d".format(year, week)
    }

    /** Haftanın bitimine kalan süre (ms). */
    fun msUntilWeekEnd(now: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance(tz).apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (timeInMillis <= now) add(Calendar.WEEK_OF_YEAR, 1)
        }
        return c.timeInMillis - now
    }

    private fun scores(week: String) = db.collection("zp_leagues").document(week).collection("scores")

    private suspend fun ensureLeagueEntry() {
        val u = _user.value ?: return
        val prefs = UserPreferencesRepository(appContext)
        val ref = scores(weekId()).document(u.uid)
        val data = mapOf(
            "name" to prefs.userName.first(),
            "avatar" to prefs.avatarId.first(),
            "frame" to prefs.equippedFrame.first(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.set(data, SetOptions.merge()).await()
    }

    /** Bir seviye/sonsuz mod sonunda kazanılan puanı bu haftanın lig puanına ekler. */
    fun addWeeklyScore(points: Int) {
        if (!isConfigured || points <= 0) return
        val u = _user.value ?: return
        scope.launch {
            try {
                val prefs = UserPreferencesRepository(appContext)
                scores(weekId()).document(u.uid).set(
                    mapOf(
                        "name" to prefs.userName.first(),
                        "avatar" to prefs.avatarId.first(),
                        "frame" to prefs.equippedFrame.first(),
                        "score" to FieldValue.increment(points.coerceAtMost(1500).toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "addWeeklyScore", e)
            }
        }
        scheduleBackup()
    }

    suspend fun loadBoard(offsetWeeks: Int = 0): LeagueBoard? {
        if (!isConfigured) return null
        val week = weekId(offsetWeeks)
        return try {
            val snap = scores(week).orderBy("score", Query.Direction.DESCENDING).limit(50).get().await()
            val top = snap.documents.map { d ->
                LeagueEntry(
                    uid = d.id,
                    name = d.getString("name") ?: "Oyuncu",
                    avatar = d.getLong("avatar")?.toInt() ?: 0,
                    frame = d.getString("frame") ?: "none",
                    score = d.getLong("score") ?: 0L
                )
            }.filter { it.score > 0 }
            val me = _user.value
            var myScore = 0L
            var myRank: Int? = null
            if (me != null) {
                val idx = top.indexOfFirst { it.uid == me.uid }
                if (idx >= 0) { myRank = idx + 1; myScore = top[idx].score }
                else {
                    val mine = scores(week).document(me.uid).get().await()
                    myScore = mine.getLong("score") ?: 0L
                    if (myScore > 0) {
                        val above = scores(week).whereGreaterThan("score", myScore).count()
                            .get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
                        myRank = above.toInt() + 1
                    }
                }
            }
            LeagueBoard(week, top, myRank, myScore)
        } catch (e: Exception) {
            Log.w(TAG, "loadBoard", e)
            null
        }
    }

    /** Geçen haftanın ilk 10'una girildiyse ödülü bir kez verir. */
    private suspend fun checkWeeklyReward() {
        val u = _user.value ?: return
        val prev = weekId(-1)
        val claimRef = db.collection("zp_users").document(u.uid).collection("claims").document(prev)
        if (claimRef.get().await().exists()) return
        val top = scores(prev).orderBy("score", Query.Direction.DESCENDING).limit(10).get().await()
        val rank = top.documents.indexOfFirst { it.id == u.uid && (it.getLong("score") ?: 0) > 0 } + 1
        if (rank <= 0) return
        val diamonds = REWARDS[rank] ?: return
        claimRef.set(mapOf("rank" to rank, "diamonds" to diamonds, "at" to FieldValue.serverTimestamp())).await()
        UserPreferencesRepository(appContext).addDiamonds(diamonds)
        _pendingReward.value = WeeklyReward(prev, rank, diamonds)
        backupNow()
    }

    fun consumeReward() {
        _pendingReward.value = null
    }
}
