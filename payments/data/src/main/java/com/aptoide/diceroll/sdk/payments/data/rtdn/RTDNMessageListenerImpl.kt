package com.aptoide.diceroll.sdk.payments.data.rtdn

import android.util.Log
import com.aptoide.diceroll.sdk.core.network.clients.rtdn.RTDNMessageListener
import com.aptoide.diceroll.sdk.core.ui.notifications.NotificationHandler
import com.aptoide.diceroll.sdk.payments.data.usecases.GetMessageFromRTDNResponseUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RTDNMessageListenerImpl(
    private val notificationHandler: NotificationHandler,
    private val getMessageFromRTDNResponseUseCase: GetMessageFromRTDNResponseUseCase,
    private val onRemoveSubscription: (String) -> Unit
) : RTDNMessageListener {

    override fun onMessageReceived(message: String) {
        Log.i(TAG, "Received RTDN message: $message")
        getMessageFromRTDNResponseUseCase(message, onRemoveSubscription)?.let { messageToShow ->
            CoroutineScope(Dispatchers.Main).launch {
                notificationHandler.showPurchaseNotification(messageToShow)
            }
        }
    }

    private companion object {
        val TAG = RTDNMessageListenerImpl::class.simpleName
    }
}
