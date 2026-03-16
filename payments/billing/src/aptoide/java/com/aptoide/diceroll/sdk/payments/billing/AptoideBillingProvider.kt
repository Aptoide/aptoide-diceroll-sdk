package com.aptoide.diceroll.sdk.payments.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
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
import com.aptoide.sdk.billing.AptoideBillingClient
import com.aptoide.sdk.billing.BillingFlowParams
import com.aptoide.sdk.billing.BillingResult
import com.aptoide.sdk.billing.ConsumeParams
import com.aptoide.sdk.billing.ProductDetails
import com.aptoide.sdk.billing.Purchase
import com.aptoide.sdk.billing.PurchasesUpdatedListener
import com.aptoide.sdk.billing.QueryProductDetailsParams
import com.aptoide.sdk.billing.QueryProductDetailsResult
import com.aptoide.sdk.billing.QueryPurchasesParams
import com.aptoide.sdk.billing.listeners.AptoideBillingClientStateListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

class AptoideBillingProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val purchaseValidatorRepository: PurchaseValidatorRepository,
    getMessageFromRTDNResponseUseCase: GetMessageFromRTDNResponseUseCase,
    notificationHandler: NotificationHandler,
    private val webSocketClient: RTDNWebSocketClient,
    private val paymentsResultManager: PaymentsResultManager,
    private val analyticsManager: AnalyticsManager,
) : IBillingProvider {
    private lateinit var billingClient: AptoideBillingClient

    override val connectionState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val attemptsPrice: MutableStateFlow<String?> = MutableStateFlow(null)
    override val purchasableItems: MutableList<InternalSkuDetails> = mutableStateListOf()
    override val purchaseStates: Flow<PaymentState> =
        PurchaseStateStream.eventFlow.filterIsInstance<PaymentState>()

    private val myItems: MutableList<ProductDetails> = mutableStateListOf()
    private val purchases: MutableList<Purchase> = mutableListOf()
    private var isRTDNConnectionEstablished = false

    private val rtdnListener = RTDNMessageListenerImpl(
        notificationHandler,
        getMessageFromRTDNResponseUseCase,
        ::onRemoveSubscription,
    )

    override fun initialize(context: Context) {
        val publicKey = BuildConfig.APTOIDE_APP_PUBLIC_KEY
        if (publicKey.isBlank()) {
            Log.e(LOG_TAG, "Missing APTOIDE_APP_PUBLIC_KEY - billing client not started.")
            connectionState.value = false
            publishPaymentError(
                item = null,
                responseCode = InternalResponseCode.DEVELOPER_ERROR,
            )
            return
        }

        // The Aptoide SDK requires the app public key for purchase signature checks.
        billingClient = AptoideBillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .setPublicKey(publicKey)
            .build()
        billingClient.startConnection(billingClientStateListener)
    }

    override fun queryProducts(ids: List<String>) {
        val inappIds = ids.filter { Skus.INAPPS.contains(it) }
        val subsIds = ids.filter { Skus.SUBS.contains(it) }

        if (inappIds.isNotEmpty()) {
            queryProductsByType(inappIds, AptoideBillingClient.ProductType.INAPP)
        }
        if (subsIds.isNotEmpty()) {
            queryProductsByType(subsIds, AptoideBillingClient.ProductType.SUBS)
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

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()

        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))

        obfuscatedAccountId?.let {
            billingFlowParamsBuilder
                .setDeveloperPayload(it)
                .setObfuscatedAccountId(it)
        }

        val result = billingClient.launchBillingFlow(activity, billingFlowParamsBuilder.build())
        if (result.responseCode != AptoideBillingClient.BillingResponseCode.OK) {
            publishPaymentError(
                item = Item.fromSku(id),
                responseCode = toInternalResponseCode(result.responseCode),
            )
        }
    }

    override fun launchAppUpdateDialog(context: Context) {
        if (::billingClient.isInitialized) {
            billingClient.launchAppUpdateDialog(context)
        }
    }

    private val billingClientStateListener: AptoideBillingClientStateListener
        get() = object : AptoideBillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    AptoideBillingClient.BillingResponseCode.OK -> {
                        Log.i(LOG_TAG, "Aptoide Billing setup successful.")
                        connectionState.value = true
                        setupRTDNListener()
                        queryPurchases()
                        queryActiveSubscriptions()
                        queryProducts(Skus.INAPPS + Skus.SUBS)
                    }

                    else -> {
                        Log.i(
                            LOG_TAG,
                            "Problem setting up Aptoide Billing: ${billingResult.responseCode}",
                        )
                        connectionState.value = false
                        attemptsPrice.value = null
                        purchasableItems.clear()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.i(LOG_TAG, "Aptoide Billing disconnected")
                connectionState.value = false
                attemptsPrice.value = null
                purchasableItems.clear()
            }
        }

    private val purchasesUpdatedListener: PurchasesUpdatedListener
        get() = PurchasesUpdatedListener { billingResult, updatedPurchases ->
            when (billingResult.responseCode) {
                AptoideBillingClient.BillingResponseCode.OK -> {
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
                            validateSubscriptionPurchase(purchase)
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
                ) { _, _ -> }
                processSuccessfulPurchase(purchase)
            } else {
                publishPaymentError(
                    item = Item.fromSku(product),
                    responseCode = InternalResponseCode.ERROR,
                )
            }
        }
    }

    private fun validateSubscriptionPurchase(
        purchase: Purchase,
        skipValidation: Boolean = false,
    ) {
        CoroutineScope(Job()).launch {
            val product = purchase.products.first()
            val purchaseToken = purchase.purchaseToken
            val isPurchaseValid =
                skipValidation || BuildConfig.DEBUG || isPurchaseValid(product, purchaseToken)

            if (isPurchaseValid) {
                processSuccessfulPurchase(purchase)
            } else {
                publishPaymentError(
                    item = Item.fromSku(product),
                    responseCode = InternalResponseCode.ERROR,
                )
            }
        }
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(AptoideBillingClient.ProductType.INAPP)
                .build(),
        ) { billingResult, existingPurchases ->
            if (billingResult.responseCode == AptoideBillingClient.BillingResponseCode.OK) {
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
                .setProductType(AptoideBillingClient.ProductType.SUBS)
                .build(),
        ) { billingResult, existingPurchases ->
            if (billingResult.responseCode == AptoideBillingClient.BillingResponseCode.OK) {
                existingPurchases.forEach { purchase ->
                    purchases.add(purchase)
                    validateSubscriptionPurchase(purchase)
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
        if (billingResult.responseCode != AptoideBillingClient.BillingResponseCode.OK) return

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
        return if (skuType == AptoideBillingClient.ProductType.SUBS) {
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
            AptoideBillingClient.ProductType.SUBS
    }

    private fun isNonConsumableProduct(product: String?): Boolean {
        return product == Item.NON_CONSUMABLE_ATTEMPTS_SKU
    }

    private fun toInternalResponseCode(code: Int): InternalResponseCode {
        return InternalResponseCode.entries.find { it.value == code } ?: when (code) {
            AptoideBillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                InternalResponseCode.BILLING_UNAVAILABLE

            AptoideBillingClient.BillingResponseCode.TOO_MANY_REQUESTS ->
                InternalResponseCode.SERVICE_UNAVAILABLE

            else -> InternalResponseCode.ERROR
        }
    }

    private fun publishPaymentError(item: Item?, responseCode: InternalResponseCode) {
        CoroutineScope(Job()).launch {
            PurchaseStateStream.publish(PaymentState.PaymentError(item, responseCode))
        }
    }

    companion object {
        private const val LOG_TAG = "AptoideBillingProvider"
    }
}
