package com.aptoide.diceroll.sdk.feature.roll_game.data.streams

import com.aptoide.diceroll.sdk.core.utils.EventBusInterface
import kotlinx.coroutines.flow.MutableSharedFlow

object DiceSelectionDialogVisibilityStateStream : EventBusInterface {

    override var eventFlow = MutableSharedFlow<Any>()
}
