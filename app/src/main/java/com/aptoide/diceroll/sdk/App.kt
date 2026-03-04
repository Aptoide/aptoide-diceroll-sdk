package com.aptoide.diceroll.sdk

import android.app.Application
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

        initiateBillingSDK()
    }

    private fun initiateBillingSDK() {
        sdkManager.setupSdkConnection(this)
    }
}
