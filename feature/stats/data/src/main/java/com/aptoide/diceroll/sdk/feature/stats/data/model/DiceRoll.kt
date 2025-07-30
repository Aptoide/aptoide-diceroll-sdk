package com.aptoide.diceroll.sdk.feature.stats.data.model

data class DiceRoll(
  val id: Int?,
  val rollWin: Boolean,
  val guessNumber: Int,
  val resultNumber: Int,
  val attemptsLeft: Int,
)
