package com.aptoide.diceroll.sdk.payments.data.usecases

import com.aptoide.diceroll.sdk.feature.roll_game.data.SubscriptionsDataSource
import com.aptoide.diceroll.sdk.feature.roll_game.data.model.Subscription.RAINBOW_DICE
import com.aptoide.diceroll.sdk.feature.roll_game.data.usecases.GetLegendaryDiceStatusUseCase
import com.aptoide.diceroll.sdk.feature.roll_game.data.usecases.UpdateLegendaryDiceStatusUseCase
import com.aptoide.diceroll.sdk.payments.data.models.Item.LegendaryDice
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState.PaymentSuccess
import com.aptoide.diceroll.sdk.payments.data.streams.PurchaseStateStream
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Finalizes a successful (acknowledged) Legendary Dice non-consumable purchase by
 * unlocking the rainbow skin. Mirrors [ProcessSuccessfulGoldenDicePurchaseUseCase] but for a
 * one-time INAPP non-consumable instead of a subscription. Idempotent: re-running it for an
 * already-owned Legendary Dice is a no-op, so restoring purchases will not spam success events.
 */
class ProcessSuccessfulLegendaryDicePurchaseUseCase @Inject constructor(
    private val getLegendaryDiceStatusUseCase: GetLegendaryDiceStatusUseCase,
    private val updateLegendaryDiceStatusUseCase: UpdateLegendaryDiceStatusUseCase,
    private val subscriptionsDataSource: SubscriptionsDataSource,
) {

    suspend operator fun invoke() {
        if (getLegendaryDiceStatusUseCase().firstOrNull() != true) {
            PurchaseStateStream.publish(PaymentSuccess(LegendaryDice))
            updateLegendaryDiceStatusUseCase(true)
            subscriptionsDataSource.saveSelectedSubscription(RAINBOW_DICE)
        }
    }
}
