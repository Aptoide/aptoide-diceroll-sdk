package com.aptoide.diceroll.sdk.feature.roll_game.data.usecases

import com.aptoide.diceroll.sdk.feature.roll_game.data.GoldenDiceDataSource
import javax.inject.Inject

class UpdateGoldenDiceStatusUseCase @Inject constructor(private val datastore: GoldenDiceDataSource) {

    suspend operator fun invoke(active: Boolean) = datastore.saveGoldenDiceStatus(active)
}
