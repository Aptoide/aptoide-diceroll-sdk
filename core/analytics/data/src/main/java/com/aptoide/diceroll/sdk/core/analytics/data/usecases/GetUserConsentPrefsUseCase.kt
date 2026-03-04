package com.aptoide.diceroll.sdk.core.analytics.data.usecases

import com.aptoide.diceroll.sdk.core.analytics.data.UserConsentDataSource
import com.aptoide.diceroll.sdk.core.analytics.data.model.UserConsentPrefs
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserConsentPrefsUseCase @Inject constructor(private val datastore: UserConsentDataSource) {

    operator fun invoke(): Flow<UserConsentPrefs> = datastore.getUserConsentPrefs()
}
