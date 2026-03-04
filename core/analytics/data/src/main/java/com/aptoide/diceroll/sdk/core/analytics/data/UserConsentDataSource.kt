package com.aptoide.diceroll.sdk.core.analytics.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aptoide.diceroll.sdk.core.analytics.data.model.ConsentState
import com.aptoide.diceroll.sdk.core.analytics.data.model.UserConsentPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserConsentDataSource @Inject constructor(
    private val preferences: DataStore<Preferences>,
) {

    /**
     * Updates the User Consent preferences.
     */
    suspend fun saveUserConsentPrefs(userConsentPrefs: UserConsentPrefs) {
        withContext(Dispatchers.IO) {
            preferences.edit { prefs ->
                prefs[USER_CONSENT_STATE] = userConsentPrefs.userConsentState.name
            }
        }
    }

    /**
     * Stream of User Consent preferences [UserConsentPrefs].
     */
    fun getUserConsentPrefs(): Flow<UserConsentPrefs> {
        return preferences.data.map { prefs ->
            UserConsentPrefs(
                ConsentState.valueOf(
                    prefs[USER_CONSENT_STATE] ?: ConsentState.UNKNOWN.name
                )
            )
        }.distinctUntilChanged()
    }

    companion object {
        val USER_CONSENT_STATE = stringPreferencesKey("user_consent_state")
    }
}
