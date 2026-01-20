package com.aptoide.diceroll.sdk.core.utils

import android.content.Context
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib

class AnalyticsManager(private val context: Context, private val userIdManager: UserIdManager) {

    fun logStartEvent() {
        val eventValues = mutableMapOf<String, Any>()
        eventValues["user_id"] = userIdManager.getUserId()
        AppsFlyerLib.getInstance().logEvent(context, "START", eventValues)
    }

    fun logPurchaseEvent(
        revenue: Double,
        currency: String,
        productId: String,
        productType: String
    ) {
        val eventValues = mutableMapOf<String, Any>()
        eventValues[AFInAppEventParameterName.REVENUE] = revenue
        eventValues[AFInAppEventParameterName.CURRENCY] = currency
        eventValues[AFInAppEventParameterName.CONTENT_ID] = productId
        eventValues[AFInAppEventParameterName.CONTENT_TYPE] = productType
        AppsFlyerLib.getInstance().logEvent(context, AFInAppEventType.PURCHASE, eventValues)
    }
}
