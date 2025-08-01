package com.aptoide.diceroll.sdk.feature.store.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aptoide.diceroll.sdk.feature.roll_game.data.usecases.GetGoldenDiceStatusUseCase
import com.aptoide.diceroll.sdk.feature.roll_game.data.usecases.GetTrialDiceStatusUseCase
import com.aptoide.diceroll.sdk.feature.settings.data.usecases.GetUserUseCase
import com.aptoide.diceroll.sdk.payments.billing.SdkManager
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuDetails
import com.aptoide.diceroll.sdk.payments.data.models.Item
import com.aptoide.diceroll.sdk.payments.data.models.Item.GoldDice
import com.aptoide.diceroll.sdk.payments.data.models.Item.TrialDice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val sdkManager: SdkManager,
    private val getUserUseCase: GetUserUseCase,
    private val getGoldenDiceStatusUseCase: GetGoldenDiceStatusUseCase,
    private val getTrialDiceStatusUseCase: GetTrialDiceStatusUseCase,
) : ViewModel() {

    internal val purchasableItems: List<InternalSkuDetails> get() = sdkManager._purchasableItems

    fun launchBillingSdkFlow(context: Activity, item: Item) {
        sdkManager.startPayment(
            context,
            item.sku,
            item.type,
            getUserUseCase().uuid
        )
    }

    fun getSubscriptionStateForSKU(skuDetails: InternalSkuDetails): StateFlow<Boolean> {
        when (skuDetails.sku) {
            GoldDice.sku -> return getGoldenDiceStatusUseCase()
                .map {
                    it
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = false
                )

            TrialDice.sku -> return getTrialDiceStatusUseCase()
                .map {
                    it
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = false
                )
        }
        return MutableStateFlow(false)
    }
}
