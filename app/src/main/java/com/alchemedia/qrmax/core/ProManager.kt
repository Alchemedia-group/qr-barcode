package com.alchemedia.qrmax.core

import android.app.Activity
import android.app.Application
import android.content.Context
import com.alchemedia.qrmax.BuildConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProEvent { Purchased, Pending, NothingToRestore, Unavailable }

/**
 * QR Max Pro is a one-time in-app purchase (product id [PRODUCT_ID]) handled by Google Play Billing.
 * Create the product in Play Console as a one-time "managed" product priced at 4.99 GBP.
 * Debug builds also offer an "unlock without paying" button so the locked features can be tested.
 */
class ProManager(app: Application) {
    private val prefs = app.getSharedPreferences("qrmax", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var purchased = prefs.getBoolean("pro_purchased", false)
    private var debugUnlocked = BuildConfig.DEBUG && prefs.getBoolean("pro_debug", false)
    private val _isPro = MutableStateFlow(purchased || debugUnlocked)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()
    private val _price = MutableStateFlow<String?>(null)
    /** Local price from Google Play, for example "£4.99". Null until Play answers. */
    val price: StateFlow<String?> = _price.asStateFlow()
    val events = MutableSharedFlow<ProEvent>(extraBufferCapacity = 4)

    private var details: ProductDetails? = null
    private val client: BillingClient = BillingClient.newBuilder(app)
        .setListener { result, list -> if (result.responseCode == BillingClient.BillingResponseCode.OK && list != null) scope.launch { handle(list) } }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { loadDetails(); sync(silent = true) }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private suspend fun loadDetails() {
        val p = QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build())).build()
        val res = client.queryProductDetails(p)
        details = res.productDetailsList?.firstOrNull()
        _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    /** Re-reads what the Play account owns. Used at launch and by "Restore purchase". */
    private suspend fun sync(silent: Boolean) {
        val res = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        if (res.billingResult.responseCode != BillingClient.BillingResponseCode.OK) { if (!silent) events.tryEmit(ProEvent.Unavailable); return }
        val owned = res.purchasesList.any { it.products.contains(PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        handle(res.purchasesList)
        if (!owned) { setPurchased(false); if (!silent) events.tryEmit(ProEvent.NothingToRestore) }
    }

    private suspend fun handle(list: List<Purchase>) {
        for (p in list) {
            if (!p.products.contains(PRODUCT_ID)) continue
            when (p.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!p.isAcknowledged) client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
                    val first = !purchased; setPurchased(true); if (first) events.tryEmit(ProEvent.Purchased)
                }
                Purchase.PurchaseState.PENDING -> events.tryEmit(ProEvent.Pending)
            }
        }
    }

    private fun setPurchased(v: Boolean) { purchased = v; prefs.edit().putBoolean("pro_purchased", v).apply(); _isPro.value = purchased || debugUnlocked }

    fun purchase(activity: Activity) {
        val d = details
        if (!client.isReady || d == null) { start(); events.tryEmit(ProEvent.Unavailable); return }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())).build()
        val r = client.launchBillingFlow(activity, params)
        if (r.responseCode != BillingClient.BillingResponseCode.OK && r.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) events.tryEmit(ProEvent.Unavailable)
    }

    fun restore() { if (!client.isReady) { start(); events.tryEmit(ProEvent.Unavailable); return }; scope.launch { sync(silent = false) } }

    /** Debug builds only: lets testers see the unlocked app without a Play purchase. */
    fun debugUnlock(on: Boolean = true) { if (!BuildConfig.DEBUG) return; debugUnlocked = on; prefs.edit().putBoolean("pro_debug", on).apply(); _isPro.value = purchased || debugUnlocked }

    companion object { const val PRODUCT_ID = "qrmax_pro" }
}
