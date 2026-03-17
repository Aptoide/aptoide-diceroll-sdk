package com.aptoide.diceroll.sdk.payments.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aptoide.sdk.billing.AptoideBillingClient
import com.aptoide.sdk.billing.BillingFlowParams
import com.aptoide.sdk.billing.BillingResult
import com.aptoide.sdk.billing.ConsumeParams
import com.aptoide.sdk.billing.ProductDetails
import com.aptoide.sdk.billing.ProductDetailsResponseListener
import com.aptoide.sdk.billing.Purchase
import com.aptoide.sdk.billing.PurchasesResponseListener
import com.aptoide.sdk.billing.PurchasesUpdatedListener
import com.aptoide.sdk.billing.QueryProductDetailsParams
import com.aptoide.sdk.billing.QueryProductDetailsResult
import com.aptoide.sdk.billing.QueryPurchasesParams
import com.aptoide.sdk.billing.listeners.AptoideBillingClientStateListener
import com.aptoide.sdk.billing.listeners.ConsumeResponseListener
import com.aptoide.diceroll.sdk.payments.billing.repository.PurchaseValidatorRepository
import com.aptoide.diceroll.sdk.payments.data.models.InternalResponseCode
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuDetails
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuType
import com.aptoide.diceroll.sdk.payments.data.models.Item
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState
import com.aptoide.diceroll.sdk.payments.data.streams.PurchaseStateStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the Aptoide Billing SDK integration.
 *
 * This class initializes the Aptoide Billing Client, sets up
 * listeners for billing events, and provides methods to interact
 * with the billing service.
 *
 * It serves as a mirror of the googlePlay flavor's SdkManager with
 * identical class name, method signatures, and public-facing return types.
 * The Aptoide-specific types are encapsulated here and do not leak to
 * the UI or ViewModel layer.
 */
interface SdkManager {
    /**
     * The Aptoide Billing Client instance.
     */
    val billingClient: AptoideBillingClient

    val _connectionState: MutableStateFlow<Boolean>

    val _attemptsPrice: MutableStateFlow<String?>

    val _purchasableItems: MutableList<InternalSkuDetails>

    val _purchases: ArrayList<Purchase>

    val _purchaseValidatorRepository: PurchaseValidatorRepository

    val _myItems: MutableList<ProductDetails>

    /**
     * Method to start the Setup of the SDK.
     */
    fun setupSdkConnection(context: Context)

    /**
     * Method to start the Listener of the RTDN Api.
     */
    fun setupRTDNListener()

    /**
     * Process the result of a Successful Purchase
     */
    fun processSuccessfulPurchase(purchase: Purchase)

    /**
     * Process the expired Subscriptions
     */
    fun processExpiredPurchases(purchases: List<Purchase>)

    /**
     * Listener for Aptoide Billing Client state changes.
     *
     * This listener handles events related to the connection state
     * of the Aptoide billing client and has two methods to act on connection and
     * disconnection events.
     *
     * @param billingResult The response code from the billing client
     */
    val billingClientStateListener: AptoideBillingClientStateListener
        get() = object : AptoideBillingClientStateListener {
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

    /**
     * Listener that gets called when purchases are updated.
     *
     * This listener handles the response codes and purchase data
     * from the billing client after a purchase flow completes.
     *
     * Unlike the Google Play version, purchases is a non-nullable List.
     *
     * @param billingResult The [BillingResult] from the billing client
     * @param purchases The list of Purchase objects with the purchase data
     */
    val purchasesUpdatedListener: PurchasesUpdatedListener
        get() = PurchasesUpdatedListener { billingResult: BillingResult, purchases: List<Purchase>? ->
            when (billingResult.responseCode) {
                AptoideBillingClient.BillingResponseCode.OK -> {
                    if (!purchases.isNullOrEmpty()) {
                        for (purchase in purchases) {
                            _purchases.add(purchase)
                            Log.i(
                                LOG_TAG, "PurchasesUpdatedListener: purchase data:" +
                                    "\nsku: ${purchase.products.first()}" +
                                    "\npackageName: ${purchase.packageName}" +
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

    /**
     * Listener for handling consume purchase responses.
     *
     * @param billingResult The [BillingResult] from consuming purchase
     * @param purchaseToken The token of the consumed purchase
     */
    val consumeResponseListener: ConsumeResponseListener
        get() = ConsumeResponseListener { billingResult, purchaseToken ->
            Log.d(
                LOG_TAG,
                "ConsumeResponseListener: Consumption finished. Purchase: $purchaseToken, result: $billingResult"
            )
        }

    /**
     * Starts the payment flow for the given SKU.
     *
     * Unlike the Google Play version, there is no offerToken for subscriptions
     * in the Aptoide SDK — BillingFlowParams.ProductDetailsParams does not
     * expose an offerToken field.
     *
     * @param sku The SKU identifier for the in-app product.
     * @param developerPayload A developer-defined string that will be returned with the purchase data.
     */
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

        // Aptoide SDK does not require an offerToken for subscriptions.
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
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

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun launchAppUpdateDialog(context: Context) {
        billingClient.launchAppUpdateDialog(context)
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

    /**
     * Aptoide does not require a separate acknowledgePurchase step for
     * subscriptions or non-consumable items (unlike Google Play Billing).
     * Purchases are considered finalised automatically by the Aptoide SDK.
     * This method directly processes the successful purchase as a no-op acknowledgement.
     */
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
                // No-op: Aptoide does not require an explicit acknowledgePurchase call.
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
            QueryPurchasesParams.newBuilder()
                .setProductType(AptoideBillingClient.ProductType.INAPP)
                .build(),
            object : PurchasesResponseListener {
                override fun onQueryPurchasesResponse(
                    billingResult: BillingResult,
                    purchases: List<Purchase>
                ) {
                    if (billingResult.responseCode == AptoideBillingClient.BillingResponseCode.OK) {
                        for (purchase in purchases) {
                            _purchases.add(purchase)
                            validateAndConsumePurchase(purchase)
                        }
                    }
                }
            }
        )
    }

    private fun queryActiveSubscriptions() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(AptoideBillingClient.ProductType.SUBS)
                .build(),
            object : PurchasesResponseListener {
                override fun onQueryPurchasesResponse(
                    billingResult: BillingResult,
                    purchases: List<Purchase>
                ) {
                    if (billingResult.responseCode == AptoideBillingClient.BillingResponseCode.OK) {
                        for (purchase in purchases) {
                            _purchases.add(purchase)
                            validateAndAcknowledgePurchase(purchase)
                        }
                        processExpiredPurchases(purchases)
                    }
                }
            }
        )
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

        billingClient.queryProductDetailsAsync(
            queryProductDetailsParams,
            object : ProductDetailsResponseListener {
                override fun onProductDetailsResponse(
                    billingResult: BillingResult,
                    productDetailsResult: QueryProductDetailsResult
                ) {
                    processProductDetailsResult(
                        billingResult,
                        productDetailsResult,
                        AptoideBillingClient.ProductType.INAPP
                    )
                }
            }
        )
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

        billingClient.queryProductDetailsAsync(
            queryProductDetailsParams,
            object : ProductDetailsResponseListener {
                override fun onProductDetailsResponse(
                    billingResult: BillingResult,
                    productDetailsResult: QueryProductDetailsResult
                ) {
                    processProductDetailsResult(
                        billingResult,
                        productDetailsResult,
                        AptoideBillingClient.ProductType.SUBS
                    )
                }
            }
        )
    }

    /**
     * Processes the result of a product details query.
     *
     * @param billingResult The [BillingResult] from the billing client
     * @param productDetailsResult QueryProductDetailsResult containing the products list.
     * @param skuType Type of Product
     */
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
