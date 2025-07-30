package com.aptoide.diceroll.sdk.core.network.clients.rtdn

interface RTDNMessageListener {
    fun onMessageReceived(message: String)
}
