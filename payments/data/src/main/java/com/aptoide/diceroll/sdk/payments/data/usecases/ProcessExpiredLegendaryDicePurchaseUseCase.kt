package com.aptoide.diceroll.sdk.payments.data.usecases

import com.aptoide.diceroll.sdk.feature.roll_game.data.SubscriptionsDataSource
import com.aptoide.diceroll.sdk.feature.roll_game.data.model.Subscription.RAINBOW_DICE
import com.aptoide.diceroll.sdk.feature.roll_game.data.usecases.UpdateLegendaryDiceStatusUseCase
import javax.inject.Inject

/**
 * Revokes the Legendary Dice (rainbow skin) entitlement when its non-consumable purchase is no
 * longer owned — e.g. the user signed out of Aptoide services or the purchase was refunded, so it
 * is no longer returned by `queryPurchasesAsync(INAPP)`. Mirrors
 * [ProcessExpiredGoldenDicePurchaseUseCase] for the INAPP non-consumable case.
 */
class ProcessExpiredLegendaryDicePurchaseUseCase @Inject constructor(
    private val updateLegendaryDiceStatusUseCase: UpdateLegendaryDiceStatusUseCase,
    private val subscriptionsDataSource: SubscriptionsDataSource,
) {

    suspend operator fun invoke() {
        updateLegendaryDiceStatusUseCase(false)
        subscriptionsDataSource.processExpiredSubscription(RAINBOW_DICE)
    }
}
