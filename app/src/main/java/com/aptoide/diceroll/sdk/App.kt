package com.aptoide.diceroll.sdk

import android.app.Application
import android.util.Log
import com.appsflyer.AppsFlyerLib
import com.aptoide.diceroll.sdk.core.analytics.AnalyticsManager
import com.aptoide.diceroll.sdk.core.analytics.UserIdManager
import com.aptoide.diceroll.sdk.feature.settings.data.usecases.GetUserUseCase
import com.aptoide.diceroll.sdk.payments.billing.SdkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var sdkManager: SdkManager

    @Inject
    lateinit var userIdManager: UserIdManager

    @Inject
    lateinit var getUserUseCase: GetUserUseCase

    override fun onCreate() {
        super.onCreate()

        // TODO: Replace "YOUR_APPSFLYER_DEV_KEY" with your actual dev key
        AppsFlyerLib.getInstance().init("YOUR_APPSFLYER_DEV_KEY", null, applicationContext)
        AppsFlyerLib.getInstance().waitForCustomerUserId(true)
        AppsFlyerLib.getInstance().start(this)
        val userId = getUserUseCase().uuid
        // TODO: Device which UUID use
        //val userId = userIdManager.getUserId()
        Log.e("USERID", "onCreate: $userId")
        AppsFlyerLib.getInstance().setCustomerIdAndLogSession(userId, this)

        sdkManager.setupSdkConnection(this)
    }
}
