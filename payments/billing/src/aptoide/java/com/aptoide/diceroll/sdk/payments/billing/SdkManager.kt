package com.aptoide.diceroll.sdk.payments.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aptoide.sdk.billing.AcknowledgePurchaseParams
import com.aptoide.sdk.billing.AptoideBillingClient
import com.aptoide.sdk.billing.BillingFlowParams
import com.aptoide.sdk.billing.BillingResult
import com.aptoide.sdk.billing.ConsumeParams
import com.aptoide.sdk.billing.ProductDetails
import com.aptoide.sdk.billing.Purchase
import com.aptoide.sdk.billing.QueryProductDetailsParams
import com.aptoide.sdk.billing.QueryProductDetailsResult
import com.aptoide.sdk.billing.QueryPurchasesParams
import com.aptoide.sdk.billing.listeners.AcknowledgePurchaseResponseListener
import com.aptoide.sdk.billing.listeners.AptoideBillingClientStateListener
import com.aptoide.sdk.billing.listeners.ConsumeResponseListener
import com.aptoide.sdk.billing.listeners.PurchasesUpdatedListener
import com.aptoide.diceroll.sdk.payments.billing.repository.PurchaseValidatorRepository
import com.aptoide.diceroll.sdk.payments.data.models.InternalResponseCode
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuDetails
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuType
import com.aptoide.diceroll.sdk.payments.data.models.Item
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState
import com.aptoide.diceroll.sdk.payments.data.streams.PurchaseStateStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface SdkManager {
    val billingClient: AptoideBillingClient
    val _connectionState: MutableStateFlow<Boolean>
    val _attemptsPrice: MutableStateFlow<String?>
    val _purchasableItems: MutableList<InternalSkuDetails>
    val _purchases: ArrayList<Purchase>
    val _purchaseValidatorRepository: PurchaseValidatorRepository
    val _myItems: MutableList<ProductDetails>

    fun setupSdkConnection(context: Context)
    fun setupRTDNListener()
    fun processSuccessfulPurchase(purchase: Purchase)
    fun processExpiredPurchases(purchases: List<Purchase>)

    val billingClientStateListener: AptoideBillingClientStateListener
        get() =
            object : AptoideBillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    when (billingResult.responseCode) {
                        AptoideBillingClient.BillingResponseCode.OK -> {
                            Log.i(
                                LOG_TAG,
                                "BillingClientStateListener: Aptoide SDK Setup successful. Querying inventory."
                            )
                            _connectionState.value = true
                            setupRTDNListener()
                            queryPurchases()
                            queryActiveSubscriptions()
                            queryInappProducts(ArrayList(Skus.INAPPS))
                            querySubsProducts(ArrayList(Skus.SUBS))
                        }

                        else -> {
                            Log.i(
                                LOG_TAG,
                                "BillingClientStateListener: Problem setting up Aptoide SDK: ${billingResult.responseCode}"
                            )
                            _connectionState.value = false
                            _attemptsPrice.value = null
                            _purchasableItems.clear()
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.i(LOG_TAG, "BillingClientStateListener: Aptoide SDK Disconnected")
                    _connectionState.value = false
                    _attemptsPrice.value = null
                    _purchasableItems.clear()
                }
            }

    val purchasesUpdatedListener: PurchasesUpdatedListener
        get() = PurchasesUpdatedListener { billingResult: BillingResult, purchases: MutableList<Purchase>? ->
            when (billingResult.responseCode) {
                AptoideBillingClient.BillingResponseCode.OK -> {
                    if (!purchases.isNullOrEmpty()) {
                        for (purchase in purchases) {
                            _purchases.add(purchase)
                            Log.i(
                                LOG_TAG, "PurchasesUpdatedListener: purchase data:" +
                                    "\nsku: ${purchase.products.first()}" +
                                    "\npackageName: ${purchase.packageName}" +
                                    "\ndeveloperPayload: ${purchase.developerPayload}" +
                                    "\npurchaseState: ${purchase.purchaseState}" +
                                    "\npurchaseTime: ${purchase.purchaseTime}" +
                                    "\ntoken: ${purchase.purchaseToken}" +
                                    "\norderId: ${purchase.orderId}" +
                                    "\nsignature: ${purchase.signature}" +
                                    "\noriginalJson: ${purchase.originalJson}" +
                                    "\nisAutoRenewing: ${purchase.isAutoRenewing}"
                            )

                            val product = purchase.products.first()
                            if (isSubscriptionTypeProduct(product) || isNonConsumableProduct(product)) {
                                validateAndAcknowledgePurchase(purchase)
                            } else {
                                validateAndConsumePurchase(purchase)
                            }
                        }
                    } else {
                        CoroutineScope(Job()).launch {
                            PurchaseStateStream.publish(
                                PaymentState.PaymentError(
                                    null,
                                    InternalResponseCode.entries.find { it.value == billingResult.responseCode }
                                        ?: InternalResponseCode.ERROR)
                            )
                        }
                    }
                }

                else -> {
                    CoroutineScope(Job()).launch {
                        PurchaseStateStream.publish(
                            PaymentState.PaymentError(
                                null,
                                InternalResponseCode.entries.find { it.value == billingResult.responseCode }
                                    ?: InternalResponseCode.ERROR)
                        )
                    }
                    Log.d(
                        LOG_TAG,
                        "PurchasesUpdatedListener: response ${billingResult.responseCode} response message: ${billingResult.debugMessage}"
                    )
                }
            }
        }

    val consumeResponseListener: ConsumeResponseListener
        get() =
            ConsumeResponseListener { billingResult, purchaseToken ->
                Log.d(
                    LOG_TAG,
                    "ConsumeResponseListener: Consumption finished. Purchase: $purchaseToken, result: $billingResult"
                )
            }

    val acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener
        get() =
            AcknowledgePurchaseResponseListener { billingResult ->
                Log.d(
                    LOG_TAG,
                    "AcknowledgePurchaseResponseListener: Acknowledge finished. result: $billingResult"
                )
            }

    fun startPayment(activity: Activity, sku: String, skuType: String, developerPayload: String?) {
        CoroutineScope(Job()).launch {
            PurchaseStateStream.eventFlow.emit(PaymentState.PaymentLoading)
        }

        val productDetails = _myItems.firstOrNull { it.productId == sku }

        if (productDetails == null) {
            CoroutineScope(Job()).launch {
                PurchaseStateStream.eventFlow.emit(
                    PaymentState.PaymentError(
                        null,
                        InternalResponseCode.ITEM_UNAVAILABLE
                    )
                )
            }
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    if (skuType == AptoideBillingClient.ProductType.SUBS) {
                        setOfferToken(
                            productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
                        )
                    }
                }
                .build()
        )

        val billingFlowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .apply {
                    developerPayload?.let {
                        setObfuscatedAccountId(it)
                    }
                }.build()

        CoroutineScope(Dispatchers.IO).launch {
            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }

    fun launchAppUpdateDialog(context: Context) {
    }

    private fun validateAndConsumePurchase(purchase: Purchase, skipValidation: Boolean = false) {
        CoroutineScope(Job()).launch {
            val product = purchase.products.first()
            val purchaseToken = purchase.purchaseToken
            val isPurchaseValid =
                skipValidation || BuildConfig.DEBUG || isPurchaseValid(product, purchaseToken)

            if (isPurchaseValid) {
                Log.i(LOG_TAG, "Purchase verified successfully from Server side.")
                billingClient.consumeAsync(
                    ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build(),
                    consumeResponseListener
                )
                processSuccessfulPurchase(purchase)
            } else {
                CoroutineScope(Job()).launch {
                    PurchaseStateStream.publish(
                        PaymentState.PaymentError(
                            Item.fromSku(product),
                            InternalResponseCode.ERROR
                        )
                    )
                }
                Log.e(LOG_TAG, "There was an error verifying the Purchase on Server side.")
            }
        }
    }

    private fun validateAndAcknowledgePurchase(
        purchase: Purchase,
        skipValidation: Boolean = false
    ) {
        CoroutineScope(Job()).launch {
            val product = purchase.products.first()
            val purchaseToken = purchase.purchaseToken
            val isPurchaseValid =
                skipValidation || BuildConfig.DEBUG || isPurchaseValid(product, purchaseToken)

            if (isPurchaseValid) {
                Log.i(LOG_TAG, "Purchase verified successfully from Server side.")
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build(),
                    acknowledgePurchaseResponseListener
                )
                processSuccessfulPurchase(purchase)
            } else {
                CoroutineScope(Job()).launch {
                    PurchaseStateStream.publish(
                        PaymentState.PaymentError(
                            Item.fromSku(product),
                            InternalResponseCode.ERROR
                        )
                    )
                }
                Log.e(LOG_TAG, "There was an error verifying the Purchase on Server side.")
            }
        }
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(AptoideBillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == AptoideBillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    _purchases.add(purchase)
                    validateAndConsumePurchase(purchase)
                }
            }
        }
    }

    private fun queryActiveSubscriptions() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(AptoideBillingClient.ProductType.SUBS).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == AptoideBillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    _purchases.add(purchase)
                    validateAndAcknowledgePurchase(purchase)
                }
                processExpiredPurchases(purchases)
            }
        }
    }

    private fun queryInappProducts(skuList: List<String>) {
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    skuList.map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it)
                            .setProductType(AptoideBillingClient.ProductType.INAPP)
                            .build()
                    }
                )
                .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsResult ->
            processProductDetailsResult(
                billingResult,
                productDetailsResult,
                AptoideBillingClient.ProductType.INAPP
            )
        }
    }

    private fun querySubsProducts(skuList: List<String>) {
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    skuList.map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it)
                            .setProductType(AptoideBillingClient.ProductType.SUBS)
                            .build()
                    }
                )
                .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, details ->
            processProductDetailsResult(
                billingResult,
                details,
                AptoideBillingClient.ProductType.SUBS
            )
        }
    }

    private fun processProductDetailsResult(
        billingResult: BillingResult,
        productDetailsResult: QueryProductDetailsResult,
        skuType: String
    ) {
        Log.d(
            LOG_TAG,
            "processProductDetailsResult: item response ${billingResult.responseCode}, response message: ${billingResult.debugMessage}"
        )
        if (billingResult.responseCode == 0) {
            for (productDetails in productDetailsResult.productDetailsList) {
                if (_purchasableItems.find { it.sku == productDetails.productId } == null) {
                    _purchasableItems.add(
                        InternalSkuDetails(
                            productDetails.productId,
                            InternalSkuType.entries.first {
                                skuType.equals(it.value, true)
                            },
                            productDetails.title,
                            getPriceFromProduct(productDetails, skuType)
                        )
                    )
                    _myItems.add(productDetails)
                    if (productDetails.productId == "attempts") {
                        _attemptsPrice.value =
                            productDetails.oneTimePurchaseOfferDetails?.formattedPrice
                    }
                }
            }
        }
    }

    private fun getPriceFromProduct(productDetails: ProductDetails, skuType: String): String {
        return if (skuType == AptoideBillingClient.ProductType.SUBS) {
            productDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                ?: ""
        } else {
            productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
        }
    }

    private suspend fun isPurchaseValid(sku: String, token: String): Boolean =
        _purchaseValidatorRepository
            .isPurchaseValid(sku, token)
            .getOrDefault(false)

    private fun isSubscriptionTypeProduct(product: String?): Boolean {
        return _myItems.firstOrNull { it.productId == product }?.productType == AptoideBillingClient.ProductType.SUBS
    }

    private fun isNonConsumableProduct(product: String?): Boolean {
        val nonConsumableProducts = listOf("non_consumable_attempts")
        return nonConsumableProducts.contains(product)
    }

    companion object {
        const val LOG_TAG = "SdkManager"
    }
}
