package com.aptoide.diceroll.sdk

import android.app.Application
import com.aptoide.diceroll.sdk.core.utils.AnalyticsManager
import com.aptoide.diceroll.sdk.payments.billing.SdkManager
import com.appsflyer.AppsFlyerLib
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var sdkManager: SdkManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onCreate() {
        super.onCreate()

        // TODO: Replace "YOUR_APPSFLYER_DEV_KEY" with your actual dev key
        AppsFlyerLib.getInstance().init("YOUR_APPSFLYER_DEV_KEY", null, this)
        AppsFlyerLib.getInstance().start(this)

        analyticsManager.logStartEvent()

        sdkManager.setupSdkConnection(this)
    }
}
