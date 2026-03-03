package com.aptoide.diceroll.sdk.core.analytics

import android.content.Context
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib

class AppsFlyerAnalyticsRepository(private val context: Context) {

    fun startAnalytics(userId: String) {
        AppsFlyerLib.getInstance().init(BuildConfig.APPSFLYER_API_KEY, null, context)
        AppsFlyerLib.getInstance().waitForCustomerUserId(true)
        AppsFlyerLib.getInstance().start(context)
        AppsFlyerLib.getInstance().setCustomerIdAndLogSession(userId, context)
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
