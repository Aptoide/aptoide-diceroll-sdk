package com.aptoide.diceroll.sdk.payments.data.streams

import com.aptoide.diceroll.sdk.core.utils.EventBusInterface
import kotlinx.coroutines.flow.MutableSharedFlow

object PurchaseStateStream : EventBusInterface {

    override var eventFlow = MutableSharedFlow<Any>()
}
