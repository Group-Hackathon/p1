package com.preappointment1.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BillingManager : PurchasesUpdatedListener, BillingClientStateListener {

    private lateinit var billingClient: BillingClient
    private var isConnected = false

    private val _purchaseSuccessFlow = MutableStateFlow<String?>(null)
    val purchaseSuccessFlow: StateFlow<String?> = _purchaseSuccessFlow.asStateFlow()

    private val _pricesFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    val pricesFlow: StateFlow<Map<String, String>> = _pricesFlow.asStateFlow()

    private val productIds = listOf("p1_pack_short", "p1_pack_medium", "p1_pack_long")

    fun initialize(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        connect()
    }

    private fun connect() {
        if (isConnected) return
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            isConnected = true
            Log.d("BillingManager", "Billing client connected")
            queryProductDetails()
        } else {
            Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        isConnected = false
        Log.w("BillingManager", "Billing client disconnected")
        // Optionally implement retry logic
    }

    private fun queryProductDetails() {
        val productList = productIds.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val prices = mutableMapOf<String, String>()
                for (productDetails in productDetailsList) {
                    // For INAPP products, we use getOneTimePurchaseOfferDetails
                    val price = productDetails.oneTimePurchaseOfferDetails?.formattedPrice
                    if (price != null) {
                        prices[productDetails.productId] = price
                    }
                }
                _pricesFlow.value = prices
            } else {
                Log.e("BillingManager", "Query products failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        if (!isConnected) {
            connect()
            // Might need to wait, but for simplicity we just return if not connected
            Log.e("BillingManager", "Cannot launch purchase flow, not connected")
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                Log.e("BillingManager", "Could not find product details for purchase: $productId")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i("BillingManager", "User canceled the purchase")
        } else {
            Log.e("BillingManager", "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Must acknowledge or consume
            // Since these are "packs", we consume them so the user can buy them again later
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Purchase consumed successfully")
                    // Notify the UI that purchase succeeded
                    _purchaseSuccessFlow.value = purchase.products.firstOrNull()
                }
            }
        }
    }

    fun clearPurchaseSuccess() {
        _purchaseSuccessFlow.value = null
    }
}
