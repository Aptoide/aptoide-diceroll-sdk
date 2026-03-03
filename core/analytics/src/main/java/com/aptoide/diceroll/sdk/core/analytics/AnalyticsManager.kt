package com.aptoide.diceroll.sdk.core.analytics

import android.content.Context

class AnalyticsManager(private val context: Context) {

    private val appsFlyerAnalyticsRepository: AppsFlyerAnalyticsRepository by lazy {
        AppsFlyerAnalyticsRepository(context)
    }

    fun startAnalytics(userId: String) {
        appsFlyerAnalyticsRepository.startAnalytics(userId)
    }

    fun logPurchaseEvent(
        revenue: Double,
        currency: String,
        productId: String,
        productType: String
    ) {
        appsFlyerAnalyticsRepository.logPurchaseEvent(revenue, currency, productId, productType)
    }
}
