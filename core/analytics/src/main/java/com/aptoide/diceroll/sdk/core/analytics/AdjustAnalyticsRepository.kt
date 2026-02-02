package com.aptoide.diceroll.sdk.core.analytics

import android.content.Context
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.LogLevel

class AdjustAnalyticsRepository(private val context: Context) {

    fun startAnalytics(userId: String) {
        val config = AdjustConfig(
            context,
            BuildConfig.ADJUST_APP_TOKEN,
            AdjustConfig.ENVIRONMENT_SANDBOX
        )
        config.setLogLevel(LogLevel.VERBOSE)
        Adjust.initSdk(config)
    }

    fun logPurchaseEvent(
        revenue: Double,
        currency: String,
        productId: String,
    ) {
        val adjustEvent = AdjustEvent("test").apply {
            setRevenue(revenue, currency)
            this.productId = productId
        }

        Adjust.trackEvent(adjustEvent)
    }
}
