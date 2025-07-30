package com.aptoide.diceroll.sdk

import android.app.Application
import com.aptoide.diceroll.sdk.payments.billing.SdkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var sdkManager: SdkManager

    override fun onCreate() {
        super.onCreate()

        sdkManager.setupSdkConnection(this)
    }
}
