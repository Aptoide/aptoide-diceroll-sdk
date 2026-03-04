package com.aptoide.diceroll.sdk.core.analytics.data.usecases

import com.aptoide.diceroll.sdk.core.analytics.data.UserConsentDataSource
import com.aptoide.diceroll.sdk.core.analytics.data.model.UserConsentPrefs
import javax.inject.Inject

class SaveUserConsentPrefsUseCase @Inject constructor(private val datastore: UserConsentDataSource) {

    suspend operator fun invoke(userConsentPrefs: UserConsentPrefs) =
        datastore.saveUserConsentPrefs(userConsentPrefs)
}
