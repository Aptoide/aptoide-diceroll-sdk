package com.aptoide.diceroll.sdk

import android.app.Application
import com.aptoide.diceroll.sdk.feature.settings.data.usecases.GetUserUseCase
import com.aptoide.diceroll.sdk.payments.billing.IBillingProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var billingProvider: IBillingProvider

    @Inject
    lateinit var getUserUseCase: GetUserUseCase

    override fun onCreate() {
        super.onCreate()

        initiateBillingSDK()
    }

    private fun initiateBillingSDK() {
        billingProvider.initialize(this)
    }
}
