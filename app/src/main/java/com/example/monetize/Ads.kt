package com.example.monetize

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.pref.UserPreferencesRepository
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reklam kuralları (kullanıcıyı boğmamak için):
 *  - Banner YOK.
 *  - Ödüllü reklam: yalnızca kullanıcı isterse (elmas, ipucu, 2× ödül), günde en fazla 10.
 *  - Geçiş reklamı: yalnızca seviye sonu ekranından çıkarken; ilk gün ve ilk 10 seviye yok,
 *    kaybedilen seviyeden sonra yok, en az 3 seviye ve 3 dakika arayla. PRO'da hiç yok.
 */
object Ads {
    // Google'ın resmi test birimleri. Gerçek kimlikler AdMob hesabı açılınca buraya yazılır.
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val REAL_REWARDED = ""
    private const val REAL_INTERSTITIAL = ""

    private val rewardedId get() = if (BuildConfig.DEBUG || REAL_REWARDED.isEmpty()) TEST_REWARDED else REAL_REWARDED
    private val interstitialId get() = if (BuildConfig.DEBUG || REAL_INTERSTITIAL.isEmpty()) TEST_INTERSTITIAL else REAL_INTERSTITIAL

    private const val INTERSTITIAL_EVERY_LEVELS = 3
    private const val INTERSTITIAL_MIN_GAP_MS = 3 * 60_000L
    private const val INTERSTITIAL_MIN_LEVELS = 10

    private val started = AtomicBoolean(false)
    private lateinit var appContext: Context
    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    private var loadingRewarded = false
    private var loadingInterstitial = false
    private var levelsSinceAd = 0
    private var lastInterstitialAt = 0L
    private var levelsThisInstall = 0

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady

    /** İzin formu (AB için UMP) ve SDK başlatma. MainActivity.onCreate'ten çağrılır. */
    fun init(activity: Activity) {
        appContext = activity.applicationContext
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .apply {
                if (BuildConfig.DEBUG) setConsentDebugSettings(ConsentDebugSettings.Builder(activity).build())
            }
            .build()
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                if (info.canRequestAds()) startSdk()
            }
        }, { _ -> if (info.canRequestAds()) startSdk() })
        if (info.canRequestAds()) startSdk()
    }

    private fun startSdk() {
        if (!started.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                .build()
        )
        MobileAds.initialize(appContext) {
            loadRewarded()
            loadInterstitial()
        }
    }

    private fun loadRewarded() {
        if (!started.get() || loadingRewarded || rewarded != null) return
        loadingRewarded = true
        RewardedAd.load(appContext, rewardedId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewarded = ad; loadingRewarded = false; _rewardedReady.value = true
            }
            override fun onAdFailedToLoad(err: LoadAdError) {
                Log.d("ZP", "ödüllü yüklenemedi: ${err.message}")
                loadingRewarded = false; _rewardedReady.value = false
            }
        })
    }

    private fun loadInterstitial() {
        if (!started.get() || loadingInterstitial || interstitial != null) return
        loadingInterstitial = true
        InterstitialAd.load(appContext, interstitialId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loadingInterstitial = false }
            override fun onAdFailedToLoad(err: LoadAdError) { loadingInterstitial = false }
        })
    }

    /**
     * Ödüllü reklam gösterir. Kullanıcı reklamı sonuna kadar izlerse [onReward] çağrılır.
     * Günlük sınır dolduysa ya da reklam hazır değilse [onUnavailable] mesajla çağrılır.
     */
    suspend fun showRewarded(activity: Activity, prefs: UserPreferencesRepository, onUnavailable: (String) -> Unit, onReward: () -> Unit) {
        if (prefs.rewardedLeftToday.first() <= 0) { onUnavailable("Bugünlük reklam hakkın bitti, yarın yine gel"); return }
        val ad = rewarded
        if (ad == null) { loadRewarded(); onUnavailable("Reklam hazırlanıyor, birazdan tekrar dene"); return }
        if (!prefs.useRewardedSlot()) { onUnavailable("Bugünlük reklam hakkın bitti"); return }
        rewarded = null; _rewardedReady.value = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadRewarded() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { loadRewarded() }
        }
        ad.show(activity) { onReward() }
    }

    /**
     * Seviye sonu ekranından çıkılırken çağrılır. Kurallara uyuyorsa geçiş reklamı gösterir,
     * reklam kapanınca (ya da gösterilmezse hemen) [then] çalışır.
     */
    suspend fun maybeInterstitial(activity: Activity, prefs: UserPreferencesRepository, passed: Boolean, then: () -> Unit) {
        levelsThisInstall++
        levelsSinceAd++
        val ad = interstitial
        val now = System.currentTimeMillis()
        val ok = ad != null && passed &&
            !prefs.isProMember.first() &&
            prefs.firstOpenDay() < prefs.today() &&
            totalPlayed(prefs) >= INTERSTITIAL_MIN_LEVELS &&
            levelsSinceAd >= INTERSTITIAL_EVERY_LEVELS &&
            now - lastInterstitialAt >= INTERSTITIAL_MIN_GAP_MS
        if (!ok) { loadInterstitial(); then(); return }
        interstitial = null
        levelsSinceAd = 0
        lastInterstitialAt = now
        ad!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadInterstitial(); then() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { loadInterstitial(); then() }
        }
        ad.show(activity)
    }

    @Volatile var playedLevelsProvider: (suspend () -> Int)? = null
    private suspend fun totalPlayed(@Suppress("UNUSED_PARAMETER") prefs: UserPreferencesRepository): Int =
        playedLevelsProvider?.invoke() ?: levelsThisInstall
}
