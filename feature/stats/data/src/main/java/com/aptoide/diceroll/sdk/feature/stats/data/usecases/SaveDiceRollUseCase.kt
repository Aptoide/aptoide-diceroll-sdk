package com.aptoide.diceroll.sdk.feature.stats.data.usecases

import com.aptoide.diceroll.sdk.feature.stats.data.model.DiceRoll
import com.aptoide.diceroll.sdk.feature.stats.data.repository.DiceRollRepository
import javax.inject.Inject

class SaveDiceRollUseCase @Inject constructor(private val diceRollRepository: DiceRollRepository) {

  suspend operator fun invoke(diceRoll: DiceRoll) = diceRollRepository.saveDiceRoll(diceRoll)
}
