package com.aptoide.diceroll.sdk.payments.billing

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.aptoide.diceroll.sdk.core.network.clients.rtdn.RTDNWebSocketClient
import com.aptoide.diceroll.sdk.core.ui.notifications.NotificationHandler
import com.aptoide.diceroll.sdk.payments.billing.data.respository.PurchaseValidatorRepository
import com.aptoide.diceroll.sdk.payments.data.PaymentsResultManager
import com.aptoide.diceroll.sdk.payments.data.models.InternalPurchase
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuDetails
import com.aptoide.diceroll.sdk.payments.data.rtdn.RTDNMessageListenerImpl
import com.aptoide.diceroll.sdk.payments.data.usecases.GetMessageFromRTDNResponseUseCase
import com.aptoide.sdk.billing.AptoideBillingClient
import com.aptoide.sdk.billing.ProductDetails
import com.aptoide.sdk.billing.Purchase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * Manages the Aptoide SDK integration for in-app billing.
 *
 * This class initializes the Aptoide billing client, sets up
 * listeners for billing events, and provides methods to interact
 * with the billing service.
 *
 * It serves as a wrapper around the Aptoide SDK to handle all the
 * necessary setup and provide callbacks to the app for billing events
 * in order to simplify the call for it.
 *
 */
class SdkManagerImpl @Inject constructor(
    @ApplicationContext
    val context: Context,
    purchaseValidatorRepository: PurchaseValidatorRepository,
    getMessageFromRTDNResponseUseCase: GetMessageFromRTDNResponseUseCase,
    notificationHandler: NotificationHandler,
    private val webSocketClient: RTDNWebSocketClient,
    private val paymentsResultManager: PaymentsResultManager,
) : SdkManager {

    override lateinit var billingClient: AptoideBillingClient

    override val _connectionState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val _attemptsPrice: MutableStateFlow<String?> = MutableStateFlow(null)

    override val _purchasableItems: MutableList<InternalSkuDetails> =
        mutableStateListOf()

    override val _myItems: MutableList<ProductDetails> = mutableStateListOf()

    override val _purchases: ArrayList<Purchase> = ArrayList()

    override val _purchaseValidatorRepository: PurchaseValidatorRepository =
        purchaseValidatorRepository

    private var isRTDNConnectionEstablished = false

    private val PUBLIC_KEY = BuildConfig.PUBLIC_KEY

    /**
     * Listener for RTDN.
     */
    private val rtdnListener = RTDNMessageListenerImpl(
        notificationHandler,
        getMessageFromRTDNResponseUseCase,
        ::onRemoveSubscription
    )

    override fun setupSdkConnection(context: Context) {
        billingClient = AptoideBillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .setPublicKey(PUBLIC_KEY)
            .build()
        billingClient.startConnection(aptoideBillingClientStateListener)
    }

    override fun processSuccessfulPurchase(purchase: Purchase) {
        paymentsResultManager.processSuccessfulResult(
            InternalPurchase(purchase.products.first())
        )
    }

    override fun processExpiredPurchases(purchases: List<Purchase>) {
        paymentsResultManager.processExpiredSubscriptions(purchases.map { it.products.first() })
    }

    override fun setupRTDNListener() {
        if (!isRTDNConnectionEstablished) {
            webSocketClient.connectToRTDNApi(rtdnListener)
            isRTDNConnectionEstablished = true
        }
    }

    private fun onRemoveSubscription(sku: String) {
        paymentsResultManager.removeExpiredSubscription(sku)
    }
}
