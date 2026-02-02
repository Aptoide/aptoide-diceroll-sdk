package com.aptoide.diceroll.sdk.core.analytics

import android.content.Context
import com.kochava.tracker.Tracker
import com.kochava.tracker.events.Event
import com.kochava.tracker.events.EventType
import com.kochava.tracker.events.Events

class KochavaAnalyticsRepository(private val context: Context) {

    fun startAnalytics(userId: String) {
        Tracker.getInstance().startWithAppGuid(context, BuildConfig.KOCHAVA_APP_GUID)
        Events.getInstance().registerDefaultUserId(userId)
    }

    fun logPurchaseEvent(
        revenue: Double,
        currency: String,
        productId: String,
    ) {
        Event.buildWithEventType(EventType.PURCHASE)
            .setName(productId)
            .setPrice(revenue)
            .setCurrency(currency)
            .send()
    }
}
