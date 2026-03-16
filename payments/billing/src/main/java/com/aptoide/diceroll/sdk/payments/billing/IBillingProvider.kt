package com.aptoide.diceroll.sdk.payments.billing

import android.app.Activity
import android.content.Context
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuDetails
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IBillingProvider {
    val connectionState: StateFlow<Boolean>
    val attemptsPrice: StateFlow<String?>
    val purchasableItems: List<InternalSkuDetails>
    val purchaseStates: Flow<PaymentState>

    fun initialize(context: Context)

    fun queryProducts(ids: List<String>)

    fun launchPurchase(
        activity: Activity,
        id: String,
        skuType: String,
        obfuscatedAccountId: String? = null,
    )

    fun launchAppUpdateDialog(context: Context)
}
