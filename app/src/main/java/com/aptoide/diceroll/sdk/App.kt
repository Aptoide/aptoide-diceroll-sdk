package com.aptoide.diceroll.sdk

import android.app.Application
import com.appsflyer.AppsFlyerLib
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

    override fun onCreate() {
        super.onCreate()

        initiateAppsFlyerSDK()
        initiateBillingSDK()
    }

    private fun initiateAppsFlyerSDK() {
        // TODO: Replace "YOUR_APPSFLYER_DEV_KEY" with your actual dev key
        AppsFlyerLib.getInstance().init("YOUR_APPSFLYER_DEV_KEY", null, applicationContext)
        AppsFlyerLib.getInstance().waitForCustomerUserId(true)
        AppsFlyerLib.getInstance().start(this)
        val userId = getUserUseCase().uuid
        AppsFlyerLib.getInstance().setCustomerIdAndLogSession(userId, this)
    }

    private fun initiateBillingSDK() {
        sdkManager.setupSdkConnection(this)
    }
}
