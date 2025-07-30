package com.aptoide.diceroll.sdk.feature.stats.data.usecases

import com.aptoide.diceroll.sdk.feature.stats.data.model.DiceRoll
import com.aptoide.diceroll.sdk.feature.stats.data.repository.DiceRollRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDiceRollsUseCase @Inject constructor(private val diceRollRepository: DiceRollRepository) {

  operator fun invoke(): Flow<List<DiceRoll>> = diceRollRepository.getBdDiceRolls()
}
