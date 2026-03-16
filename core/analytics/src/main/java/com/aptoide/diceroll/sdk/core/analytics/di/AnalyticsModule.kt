package com.aptoide.diceroll.sdk.core.analytics.di

import android.content.Context
import com.aptoide.diceroll.sdk.core.analytics.data.usecases.SaveUserConsentPrefsUseCase
import com.aptoide.diceroll.sdk.core.analytics.managers.AnalyticsManager
import com.aptoide.diceroll.sdk.core.analytics.managers.ConsentManager
import com.aptoide.diceroll.sdk.feature.settings.data.usecases.GetUserUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsManager(
        @ApplicationContext context: Context,
        consentManager: ConsentManager,
        saveUserConsentPrefsUseCase: SaveUserConsentPrefsUseCase,
        getUserUseCase: GetUserUseCase
    ): AnalyticsManager {
        return AnalyticsManager(
            context,
            consentManager,
            saveUserConsentPrefsUseCase,
            getUserUseCase
        )
    }
}
