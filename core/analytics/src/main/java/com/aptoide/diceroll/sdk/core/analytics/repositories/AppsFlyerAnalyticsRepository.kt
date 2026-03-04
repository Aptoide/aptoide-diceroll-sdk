package com.aptoide.diceroll.sdk.core.analytics.repositories

import android.content.Context
import android.util.Log
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerConsent
import com.appsflyer.AppsFlyerLib
import com.aptoide.diceroll.sdk.core.analytics.BuildConfig
import com.aptoide.diceroll.sdk.core.analytics.data.model.ConsentState

class AppsFlyerAnalyticsRepository(private val context: Context) {

    fun startAnalytics(userId: String, isGdprSubject: Boolean) {
        val appsFlyerConsent = if (isGdprSubject) {
            AppsFlyerConsent(
                isUserSubjectToGDPR = true,
                hasConsentForDataUsage = true,
                hasConsentForAdsPersonalization = true,
                hasConsentForAdStorage = true
            )
        } else {
            AppsFlyerConsent(false)
        }
        AppsFlyerLib.getInstance().setConsentData(appsFlyerConsent)
        AppsFlyerLib.getInstance().init(BuildConfig.APPSFLYER_API_KEY, null, context)
        AppsFlyerLib.getInstance().waitForCustomerUserId(true)
        AppsFlyerLib.getInstance().start(context)
        AppsFlyerLib.getInstance().setCustomerIdAndLogSession(userId, context)
    }

    fun onPrivacyPolicyUpdated(consentState: ConsentState, isGdprSubject: Boolean) {
        val accepted = consentState == ConsentState.ACCEPTED

        val appsFlyerConsent = if (isGdprSubject) {
            AppsFlyerConsent(
                isUserSubjectToGDPR = true,
                hasConsentForDataUsage = accepted,
                hasConsentForAdsPersonalization = accepted,
                hasConsentForAdStorage = accepted,
            )
        } else {
            AppsFlyerConsent(false)
        }

        AppsFlyerLib.getInstance().setConsentData(appsFlyerConsent)

        if (!accepted && isGdprSubject) {
            AppsFlyerLib.getInstance().stop(true, context)
            Log.d(TAG, "User revoked consent. SDK Stopped.")
        } else {
            AppsFlyerLib.getInstance().stop(false, context)
            AppsFlyerLib.getInstance().start(context)
            Log.d(TAG, "User accepted consent. SDK Running.")
        }
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

    private companion object {
        const val TAG = "AppsFlyerAnalyticsRepository"
    }
}
