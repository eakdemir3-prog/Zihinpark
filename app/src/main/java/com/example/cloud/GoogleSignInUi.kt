package com.example.cloud

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Google ile giriş düğmesinin durumu.
 *  - Önce Credential Manager (alttan açılan hesap penceresi) denenir.
 *  - Bazı telefonlarda (ör. Xiaomi/HyperOS) bu pencere açılmayabiliyor; birkaç saniye sonra
 *    "Başka yöntemle dene" çıkar ve klasik Google hesap seçme ekranı (Authorization API) açılır.
 * İşler ekrandan bağımsız bir kapsamda çalışır; ekran değişse de giriş iptal olmaz.
 */
class GoogleSignInState internal constructor() {
    var busy by mutableStateOf(false)
        internal set
    var showAlternative by mutableStateOf(false)
        internal set
    var error by mutableStateOf<String?>(null)
        internal set
    internal var start: () -> Unit = {}
    internal var alternative: () -> Unit = {}
    fun signIn() = start()
    fun signInAlternative() = alternative()
}

@Composable
fun rememberGoogleSignIn(onSuccess: () -> Unit): GoogleSignInState {
    val activity = LocalContext.current as? Activity
    val state = remember { GoogleSignInState() }
    val success by rememberUpdatedState(onSuccess)
    var job by remember { mutableStateOf<Job?>(null) }

    fun finish(err: String?) {
        state.busy = false
        state.showAlternative = false
        CloudSync._phase.value = ""
        state.error = err
        if (err == null) success()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val act = activity ?: return@rememberLauncherForActivityResult finish("Giriş yapılamadı.")
        try {
            val r = Identity.getAuthorizationClient(act).getAuthorizationResultFromIntent(res.data ?: Intent())
            CloudSync.mainScope.launch { finish(withToken(r.accessToken)) }
        } catch (e: ApiException) {
            finish(if (e.statusCode == CommonStatusCodes.CANCELED) "Giriş iptal edildi." else "Giriş yapılamadı (kod ${e.statusCode}).")
        } catch (e: Exception) {
            finish("Giriş yapılamadı: ${e.message}")
        }
    }

    state.alternative = alt@{
        val act = activity ?: return@alt
        job?.cancel()
        state.busy = true; state.error = null; state.showAlternative = false
        CloudSync._phase.value = "Google hesabı seçiliyor..."
        val req = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(Scopes.EMAIL), Scope(Scopes.PROFILE), Scope(Scopes.OPEN_ID)))
            .build()
        Identity.getAuthorizationClient(act).authorize(req)
            .addOnSuccessListener { r ->
                val pi = r.pendingIntent
                if (r.hasResolution() && pi != null) {
                    launcher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else {
                    CloudSync.mainScope.launch { finish(withToken(r.accessToken)) }
                }
            }
            .addOnFailureListener { e -> finish("Giriş yapılamadı: ${e.message}") }
    }

    state.start = start@{
        val act = activity ?: return@start
        if (state.busy) return@start
        state.busy = true; state.error = null; state.showAlternative = false
        job = CloudSync.mainScope.launch {
            // Pencere birkaç saniyede gelmezse alternatif yöntemi göster
            val watcher = launch { delay(6_000); if (state.busy) state.showAlternative = true }
            val err = CloudSync.signIn(act)
            watcher.cancel()
            if (err != null && !err.contains("iptal", true)) {
                // Credential Manager başarısız: klasik yönteme otomatik geç
                state.busy = false
                state.alternative()
            } else finish(err)
        }
    }
    return state
}

private suspend fun withToken(accessToken: String?): String? {
    if (accessToken.isNullOrBlank()) return "Google erişimi alınamadı."
    return CloudSync.firebaseSignIn(GoogleAuthProvider.getCredential(null, accessToken))
}
