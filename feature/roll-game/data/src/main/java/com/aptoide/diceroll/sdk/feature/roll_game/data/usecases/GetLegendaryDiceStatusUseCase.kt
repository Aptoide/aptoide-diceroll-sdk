package com.aptoide.diceroll.sdk.feature.roll_game.data.usecases

import com.aptoide.diceroll.sdk.feature.roll_game.data.LegendaryDiceDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLegendaryDiceStatusUseCase @Inject constructor(private val datastore: LegendaryDiceDataSource) {

    operator fun invoke(): Flow<Boolean> = datastore.getLegendaryDiceStatus()
}
