package com.example.monetize

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.data.pref.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Google Play ürünleri. Play Console'da aynı kimliklerle "Uygulama içi ürün" olarak açılmalıdır. */
enum class Product(val id: String, val gems: Int, val title: String) {
    PRO("zp_pro", 500, "ZihinPark PRO"),
    GEMS_S("zp_gems_small", 250, "Avuç dolusu elmas"),
    GEMS_M("zp_gems_medium", 700, "Elmas kesesi"),
    GEMS_L("zp_gems_large", 1600, "Elmas sandığı");

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } }
}

object Billing {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var client: BillingClient? = null
    private lateinit var prefs: UserPreferencesRepository
    private val details = HashMap<String, ProductDetails>()

    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Ürün kimliği -> Play'den gelen yerel fiyat ("₺149,99"). Boşsa mağaza henüz hazır değil. */
    val prices: StateFlow<Map<String, String>> = _prices

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun consumeMessage() { _message.value = null }

    fun init(context: Context) {
        if (client != null) return
        prefs = UserPreferencesRepository(context.applicationContext)
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener { result, purchases ->
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> purchases?.forEach { handle(it) }
                    BillingClient.BillingResponseCode.USER_CANCELED -> {}
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
                    else -> _message.value = "Satın alma tamamlanamadı (${result.responseCode})"
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client = c
        connect()
    }

    private fun connect() {
        val c = client ?: return
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    restore()
                } else Log.d("ZP", "billing kurulum: ${result.debugMessage}")
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun loadProducts() {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(Product.entries.map {
                QueryProductDetailsParams.Product.newBuilder().setProductId(it.id).setProductType(BillingClient.ProductType.INAPP).build()
            }).build()
        c.queryProductDetailsAsync(params) { result, res ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val map = HashMap<String, String>()
            res.productDetailsList.forEach { pd ->
                details[pd.productId] = pd
                pd.oneTimePurchaseOfferDetails?.formattedPrice?.let { map[pd.productId] = it }
            }
            _prices.value = map
        }
    }

    /** Satın almaları geri yükler (yeniden kurulum, başka cihaz). */
    fun restore() {
        val c = client ?: return
        c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) list.forEach { handle(it) }
        }
    }

    fun buy(activity: Activity, product: Product) {
        val c = client
        val pd = details[product.id]
        if (c == null || !c.isReady || pd == null) {
            _message.value = "Mağaza şu an hazır değil. İnternetini kontrol edip tekrar dene."
            if (c != null && !c.isReady) connect() else loadProducts()
            return
        }
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).build()))
            .build()
        c.launchBillingFlow(activity, flow)
    }

    private fun handle(p: Purchase) {
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) {
            if (p.purchaseState == Purchase.PurchaseState.PENDING) _message.value = "Ödemen onay bekliyor; onaylanınca hesabına eklenecek."
            return
        }
        val c = client ?: return
        for (id in p.products) {
            val product = Product.of(id) ?: continue
            if (product == Product.PRO) {
                scope.launch {
                    val wasPro = prefs.isProMember.first()
                    prefs.setProMember(true)
                    if (!wasPro) {
                        prefs.addDiamonds(product.gems)
                        _message.value = "👑 PRO'ya hoş geldin! Reklamsız oyun + ${product.gems} 💎"
                    }
                }
                if (!p.isAcknowledged) {
                    c.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) {}
                }
            } else {
                // Elmas paketleri tüketilir; elmas, tüketim başarılı olunca bir kez verilir
                c.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r, _ ->
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                        scope.launch {
                            prefs.addDiamonds(product.gems * p.quantity.coerceAtLeast(1))
                            _message.value = "+${product.gems} 💎 hesabına eklendi. Teşekkürler!"
                        }
                    }
                }
            }
        }
    }
}
