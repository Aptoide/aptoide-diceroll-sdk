package com.aptoide.diceroll.sdk.core.analytics

import android.content.Context

class AnalyticsManager(private val context: Context) {

    private val adjustAnalyticsRepository: AdjustAnalyticsRepository by lazy {
        AdjustAnalyticsRepository(context)
    }
    private val appsFlyerAnalyticsRepository: AppsFlyerAnalyticsRepository by lazy {
        AppsFlyerAnalyticsRepository(context)
    }
    private val kochavaAnalyticsRepository: KochavaAnalyticsRepository by lazy {
        KochavaAnalyticsRepository(context)
    }

    fun startAnalytics(userId: String) {
        adjustAnalyticsRepository.startAnalytics(userId)
        appsFlyerAnalyticsRepository.startAnalytics(userId)
        kochavaAnalyticsRepository.startAnalytics(userId)
    }

    fun logPurchaseEvent(
        revenue: Double,
        currency: String,
        productId: String,
        productType: String
    ) {
        adjustAnalyticsRepository.logPurchaseEvent(revenue, currency, productId)
        kochavaAnalyticsRepository.logPurchaseEvent(revenue, currency, productId)
        appsFlyerAnalyticsRepository.logPurchaseEvent(revenue, currency, productId, productType)
    }
}
