package com.aptoide.diceroll.sdk.payments.data.usecases

import com.aptoide.diceroll.sdk.feature.roll_game.data.SubscriptionsDataSource
import com.aptoide.diceroll.sdk.feature.roll_game.data.model.Subscription.GOLDEN_DICE
import com.aptoide.diceroll.sdk.feature.roll_game.data.usecases.GetGoldenDiceStatusUseCase
import com.aptoide.diceroll.sdk.feature.roll_game.data.usecases.UpdateGoldenDiceStatusUseCase
import com.aptoide.diceroll.sdk.payments.data.models.Item.GoldDice
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState.PaymentSuccess
import com.aptoide.diceroll.sdk.payments.data.streams.PurchaseStateStream
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ProcessSuccessfulGoldenDicePurchaseUseCase @Inject constructor(
    private val getGoldenDiceStatusUseCase: GetGoldenDiceStatusUseCase,
    private val updateGoldenDiceStatusUseCase: UpdateGoldenDiceStatusUseCase,
    private val subscriptionsDataSource: SubscriptionsDataSource,
) {

    suspend operator fun invoke() {
        if (getGoldenDiceStatusUseCase().firstOrNull() != true) {
            PurchaseStateStream.publish(PaymentSuccess(GoldDice))
            updateGoldenDiceStatusUseCase(true)
            subscriptionsDataSource.saveSelectedSubscription(GOLDEN_DICE)
        }
    }
}
