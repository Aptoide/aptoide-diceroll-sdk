package com.aptoide.diceroll.sdk

import android.app.Application
import com.aptoide.diceroll.sdk.core.analytics.AnalyticsManager
import com.aptoide.diceroll.sdk.feature.settings.data.usecases.GetUserUseCase
import com.aptoide.diceroll.sdk.payments.billing.SdkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var sdkManager: SdkManager

    @Inject
    lateinit var getUserUseCase: GetUserUseCase

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onCreate() {
        super.onCreate()

        initiateAnalytics()
        initiateBillingSDK()
    }

    private fun initiateAnalytics() {
        analyticsManager.startAnalytics(getUserUseCase().uuid)
    }

    private fun initiateBillingSDK() {
        sdkManager.setupSdkConnection(this)
    }
}
