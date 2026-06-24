package com.aptoide.diceroll.sdk.feature.roll_game.data.usecases

import com.aptoide.diceroll.sdk.feature.roll_game.data.LegendaryDiceDataSource
import javax.inject.Inject

class UpdateLegendaryDiceStatusUseCase @Inject constructor(private val datastore: LegendaryDiceDataSource) {

    suspend operator fun invoke(active: Boolean) = datastore.saveLegendaryDiceStatus(active)
}
