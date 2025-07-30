package com.aptoide.diceroll.sdk.feature.roll_game.data.model

data class SubscriptionPrefs(
    val availableSubscriptions: List<Subscription> = emptyList(),
    val selectedSubscription: Subscription = Subscription.DEFAULT
)

enum class Subscription {
    DEFAULT, GOLDEN_DICE, TRIAL_DICE
}
