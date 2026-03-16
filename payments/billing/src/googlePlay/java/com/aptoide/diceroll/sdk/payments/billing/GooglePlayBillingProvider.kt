package com.aptoide.diceroll.sdk.payments.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.aptoide.diceroll.sdk.core.analytics.managers.AnalyticsManager
import com.aptoide.diceroll.sdk.core.network.clients.rtdn.RTDNWebSocketClient
import com.aptoide.diceroll.sdk.core.ui.notifications.NotificationHandler
import com.aptoide.diceroll.sdk.payments.billing.repository.PurchaseValidatorRepository
import com.aptoide.diceroll.sdk.payments.data.PaymentsResultManager
import com.aptoide.diceroll.sdk.payments.data.models.InternalPurchase
import com.aptoide.diceroll.sdk.payments.data.models.InternalResponseCode
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuDetails
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuType
import com.aptoide.diceroll.sdk.payments.data.models.Item
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState
import com.aptoide.diceroll.sdk.payments.data.rtdn.RTDNMessageListenerImpl
import com.aptoide.diceroll.sdk.payments.data.streams.PurchaseStateStream
import com.aptoide.diceroll.sdk.payments.data.usecases.GetMessageFromRTDNResponseUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

class GooglePlayBillingProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val purchaseValidatorRepository: PurchaseValidatorRepository,
    getMessageFromRTDNResponseUseCase: GetMessageFromRTDNResponseUseCase,
    notificationHandler: NotificationHandler,
    private val webSocketClient: RTDNWebSocketClient,
    private val paymentsResultManager: PaymentsResultManager,
    private val analyticsManager: AnalyticsManager,
) : IBillingProvider {
    private lateinit var billingClient: BillingClient

    override val connectionState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val attemptsPrice: MutableStateFlow<String?> = MutableStateFlow(null)
    override val purchasableItems: MutableList<InternalSkuDetails> = mutableStateListOf()
    override val purchaseStates: Flow<PaymentState> =
        PurchaseStateStream.eventFlow.filterIsInstance<PaymentState>()

    private val myItems: MutableList<ProductDetails> = mutableStateListOf()
    private val purchases: ArrayList<Purchase> = ArrayList()
    private var isRTDNConnectionEstablished = false

    private val rtdnListener = RTDNMessageListenerImpl(
        notificationHandler,
        getMessageFromRTDNResponseUseCase,
        ::onRemoveSubscription,
    )

    override fun initialize(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build(),
            )
            .build()
        billingClient.startConnection(billingClientStateListener)
    }

    override fun queryProducts(ids: List<String>) {
        val inappIds = ids.filter { Skus.INAPPS.contains(it) }
        val subsIds = ids.filter { Skus.SUBS.contains(it) }

        if (inappIds.isNotEmpty()) {
            queryProductsByType(inappIds, BillingClient.ProductType.INAPP)
        }
        if (subsIds.isNotEmpty()) {
            queryProductsByType(subsIds, BillingClient.ProductType.SUBS)
        }
    }

    override fun launchPurchase(
        activity: Activity,
        id: String,
        skuType: String,
        obfuscatedAccountId: String?,
    ) {
        CoroutineScope(Job()).launch {
            PurchaseStateStream.eventFlow.emit(PaymentState.PaymentLoading)
        }

        val productDetails = myItems.firstOrNull { it.productId == id }
        if (productDetails == null) {
            publishPaymentError(
                item = null,
                responseCode = InternalResponseCode.ITEM_UNAVAILABLE,
            )
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    if (skuType == BillingClient.ProductType.SUBS) {
                        setOfferToken(
                            productDetails.subscriptionOfferDetails
                                ?.firstOrNull()
                                ?.offerToken
                                .orEmpty(),
                        )
                    }
                }
                .build(),
        )

        val billingFlowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .apply {
                    obfuscatedAccountId?.let { setObfuscatedAccountId(it) }
                }
                .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun launchAppUpdateDialog(context: Context) {
        // Google Play flavor does not use Aptoide in-game update dialogs.
    }

    private val billingClientStateListener: BillingClientStateListener
        get() = object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        Log.i(LOG_TAG, "Google Billing setup successful.")
                        connectionState.value = true
                        setupRTDNListener()
                        queryPurchases()
                        queryActiveSubscriptions()
                        queryProducts(Skus.INAPPS + Skus.SUBS)
                    }

                    else -> {
                        Log.i(
                            LOG_TAG,
                            "Problem setting up Google Billing: ${billingResult.responseCode}",
                        )
                        connectionState.value = false
                        attemptsPrice.value = null
                        purchasableItems.clear()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.i(LOG_TAG, "Google Billing disconnected")
                connectionState.value = false
                attemptsPrice.value = null
                purchasableItems.clear()
            }
        }

    private val purchasesUpdatedListener: PurchasesUpdatedListener
        get() = PurchasesUpdatedListener { billingResult, updatedPurchases ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (updatedPurchases.isNullOrEmpty()) {
                        publishPaymentError(
                            item = null,
                            responseCode = toInternalResponseCode(billingResult.responseCode),
                        )
                        return@PurchasesUpdatedListener
                    }

                    updatedPurchases.forEach { purchase ->
                        purchases.add(purchase)
                        val product = purchase.products.firstOrNull()
                        if (isSubscriptionTypeProduct(product) || isNonConsumableProduct(product)) {
                            validateAndAcknowledgePurchase(purchase)
                        } else {
                            validateAndConsumePurchase(purchase)
                        }
                    }
                }

                else -> {
                    publishPaymentError(
                        item = null,
                        responseCode = toInternalResponseCode(billingResult.responseCode),
                    )
                    Log.d(
                        LOG_TAG,
                        "Purchase update failed: ${billingResult.responseCode} (${billingResult.debugMessage})",
                    )
                }
            }
        }

    private val consumeResponseListener: ConsumeResponseListener
        get() = ConsumeResponseListener { billingResult, purchaseToken ->
            Log.d(
                LOG_TAG,
                "Consume finished. Purchase: $purchaseToken, result: $billingResult",
            )
        }

    private val acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener
        get() = AcknowledgePurchaseResponseListener { billingResult ->
            Log.d(LOG_TAG, "Acknowledge finished. Result: $billingResult")
        }

    private fun setupRTDNListener() {
        if (!isRTDNConnectionEstablished) {
            webSocketClient.connectToRTDNApi(rtdnListener)
            isRTDNConnectionEstablished = true
        }
    }

    private fun onRemoveSubscription(sku: String) {
        paymentsResultManager.removeExpiredSubscription(sku)
    }

    private fun validateAndConsumePurchase(
        purchase: Purchase,
        skipValidation: Boolean = false,
    ) {
        CoroutineScope(Job()).launch {
            val product = purchase.products.first()
            val purchaseToken = purchase.purchaseToken
            val isPurchaseValid =
                skipValidation || BuildConfig.DEBUG || isPurchaseValid(product, purchaseToken)

            if (isPurchaseValid) {
                billingClient.consumeAsync(
                    ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build(),
                    consumeResponseListener,
                )
                processSuccessfulPurchase(purchase)
            } else {
                publishPaymentError(
                    item = Item.fromSku(product),
                    responseCode = InternalResponseCode.ERROR,
                )
                Log.e(LOG_TAG, "Server-side purchase validation failed.")
            }
        }
    }

    private fun validateAndAcknowledgePurchase(
        purchase: Purchase,
        skipValidation: Boolean = false,
    ) {
        CoroutineScope(Job()).launch {
            val product = purchase.products.first()
            val purchaseToken = purchase.purchaseToken
            val isPurchaseValid =
                skipValidation || BuildConfig.DEBUG || isPurchaseValid(product, purchaseToken)

            if (isPurchaseValid) {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build(),
                    acknowledgePurchaseResponseListener,
                )
                processSuccessfulPurchase(purchase)
            } else {
                publishPaymentError(
                    item = Item.fromSku(product),
                    responseCode = InternalResponseCode.ERROR,
                )
                Log.e(LOG_TAG, "Server-side purchase validation failed.")
            }
        }
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        ) { billingResult, existingPurchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                existingPurchases.forEach { purchase ->
                    purchases.add(purchase)
                    validateAndConsumePurchase(purchase)
                }
            }
        }
    }

    private fun queryActiveSubscriptions() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ) { billingResult, existingPurchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                existingPurchases.forEach { purchase ->
                    purchases.add(purchase)
                    validateAndAcknowledgePurchase(purchase)
                }
                processExpiredPurchases(existingPurchases)
            }
        }
    }

    private fun queryProductsByType(ids: List<String>, skuType: String) {
        if (ids.isEmpty()) return

        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ids.map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it)
                            .setProductType(skuType)
                            .build()
                    },
                )
                .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, details ->
            processProductDetailsResult(billingResult, details, skuType)
        }
    }

    private fun processProductDetailsResult(
        billingResult: BillingResult,
        detailsResult: QueryProductDetailsResult,
        skuType: String,
    ) {
        Log.d(
            LOG_TAG,
            "Product details response ${billingResult.responseCode}: ${billingResult.debugMessage}",
        )
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return

        detailsResult.productDetailsList.forEach { details ->
            if (purchasableItems.any { it.sku == details.productId }) return@forEach

            purchasableItems.add(
                InternalSkuDetails(
                    sku = details.productId,
                    skuType = InternalSkuType.entries.first { skuType.equals(it.value, true) },
                    title = details.title,
                    price = getPriceFromProduct(details, skuType),
                ),
            )
            myItems.add(details)
            if (details.productId == Item.ATTEMPTS_SKU) {
                attemptsPrice.value = details.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    private fun processSuccessfulPurchase(purchase: Purchase) {
        val productId = purchase.products.first()
        val productDetails = myItems.find { it.productId == productId }

        productDetails?.let {
            val revenue = it.oneTimePurchaseOfferDetails?.priceAmountMicros?.div(1_000_000.0)
                ?: it.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.priceAmountMicros
                    ?.div(1_000_000.0)
                ?: 0.0
            val currency = it.oneTimePurchaseOfferDetails?.priceCurrencyCode
                ?: it.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.priceCurrencyCode
                ?: "USD"

            analyticsManager.logPurchaseEvent(
                revenue = revenue,
                currency = currency,
                productId = productId,
                productType = it.productType,
            )
        }

        paymentsResultManager.processSuccessfulResult(
            InternalPurchase(purchase.products.first()),
        )
    }

    private fun processExpiredPurchases(existingPurchases: List<Purchase>) {
        paymentsResultManager.processExpiredSubscriptions(
            existingPurchases.map { it.products.first() },
        )
    }

    private fun getPriceFromProduct(details: ProductDetails, skuType: String): String {
        return if (skuType == BillingClient.ProductType.SUBS) {
            details.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
                ?.formattedPrice
                .orEmpty()
        } else {
            details.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty()
        }
    }

    private suspend fun isPurchaseValid(sku: String, token: String): Boolean =
        purchaseValidatorRepository.isPurchaseValid(sku, token).getOrDefault(false)

    private fun isSubscriptionTypeProduct(product: String?): Boolean {
        return myItems.firstOrNull { it.productId == product }?.productType ==
            BillingClient.ProductType.SUBS
    }

    private fun isNonConsumableProduct(product: String?): Boolean {
        return product == Item.NON_CONSUMABLE_ATTEMPTS_SKU
    }

    private fun toInternalResponseCode(code: Int): InternalResponseCode =
        InternalResponseCode.entries.find { it.value == code } ?: InternalResponseCode.ERROR

    private fun publishPaymentError(item: Item?, responseCode: InternalResponseCode) {
        CoroutineScope(Job()).launch {
            PurchaseStateStream.publish(PaymentState.PaymentError(item, responseCode))
        }
    }

    companion object {
        private const val LOG_TAG = "GoogleBillingProvider"
    }
}
