package com.aptoide.diceroll.sdk.core.analytics.managers

import android.content.Context
import com.aptoide.diceroll.sdk.core.analytics.data.model.ConsentState
import com.aptoide.diceroll.sdk.core.analytics.data.model.UserConsentPrefs
import com.aptoide.diceroll.sdk.core.analytics.data.usecases.SaveUserConsentPrefsUseCase
import com.aptoide.diceroll.sdk.core.analytics.repositories.AppsFlyerAnalyticsRepository
import com.aptoide.diceroll.sdk.feature.settings.data.usecases.GetUserUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class AnalyticsManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val consentManager: ConsentManager,
    private val saveUserConsentPrefsUseCase: SaveUserConsentPrefsUseCase,
    private val getUserUseCase: GetUserUseCase
) {

    private val appsFlyerAnalyticsRepository: AppsFlyerAnalyticsRepository by lazy {
        AppsFlyerAnalyticsRepository(context)
    }

    private var isAnalyticsAllowed = false
    private var analyticsStarted = false

    fun startAnalytics(isGdprSubject: Boolean) {
        val userId = getUserUseCase().uuid
        appsFlyerAnalyticsRepository.startAnalytics(userId, isGdprSubject)
        isAnalyticsAllowed = true
        analyticsStarted = true
    }

    fun onPrivacyPolicyUpdated(consentState: ConsentState) {
        CoroutineScope(Dispatchers.IO).launch {
            saveUserConsentPrefsUseCase(UserConsentPrefs(consentState))
            val isGdprSubject = consentManager.isGdprSubject(context)
            if (!analyticsStarted && consentState == ConsentState.ACCEPTED) {
                startAnalytics(isGdprSubject)
            } else {
                appsFlyerAnalyticsRepository
                    .onPrivacyPolicyUpdated(consentState, isGdprSubject)
                isAnalyticsAllowed = consentState == ConsentState.ACCEPTED
            }
        }
    }

    fun logPurchaseEvent(
        revenue: Double,
        currency: String,
        productId: String,
        productType: String
    ) {
        if (!isAnalyticsAllowed) {
            return
        }
        appsFlyerAnalyticsRepository.logPurchaseEvent(revenue, currency, productId, productType)
    }
}
