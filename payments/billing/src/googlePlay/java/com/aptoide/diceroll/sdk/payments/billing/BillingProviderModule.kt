package com.aptoide.diceroll.sdk.payments.billing

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingProviderModule {

    @Provides
    @Singleton
    fun provideBillingProvider(provider: GooglePlayBillingProvider): IBillingProvider = provider
}
