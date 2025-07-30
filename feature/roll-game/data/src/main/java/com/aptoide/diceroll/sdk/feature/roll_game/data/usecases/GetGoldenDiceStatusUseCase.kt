package com.aptoide.diceroll.sdk.feature.roll_game.data.usecases

import com.aptoide.diceroll.sdk.feature.roll_game.data.GoldenDiceDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGoldenDiceStatusUseCase @Inject constructor(private val datastore: GoldenDiceDataSource) {

    operator fun invoke(): Flow<Boolean> = datastore.getGoldenDiceStatus()
}
