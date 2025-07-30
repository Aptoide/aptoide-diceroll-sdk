package com.aptoide.diceroll.sdk.feature.roll_game.data.usecases

import com.aptoide.diceroll.sdk.feature.roll_game.data.SubscriptionsDataSource
import com.aptoide.diceroll.sdk.feature.roll_game.data.model.SubscriptionPrefs
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSubscriptionsPreferencesUseCase @Inject constructor(private val datastore: SubscriptionsDataSource) {

    operator fun invoke(): Flow<SubscriptionPrefs> = datastore.getSubscriptionPreferences()
}
