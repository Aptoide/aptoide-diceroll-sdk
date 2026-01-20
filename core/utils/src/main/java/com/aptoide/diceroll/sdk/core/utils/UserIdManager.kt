package com.aptoide.diceroll.sdk.core.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.UUID
import androidx.core.content.edit
import androidx.security.crypto.MasterKey

class UserIdManager(context: Context) {

    private val masterKeyAlias = MasterKey.Builder(context).build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "user_prefs",
        masterKeyAlias,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getUserId(): String {
        var userId = sharedPreferences.getString(USER_ID_KEY, null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            sharedPreferences.edit { putString(USER_ID_KEY, userId) }
        }
        return userId
    }

    companion object {
        private const val USER_ID_KEY = "user_id"
    }
}
