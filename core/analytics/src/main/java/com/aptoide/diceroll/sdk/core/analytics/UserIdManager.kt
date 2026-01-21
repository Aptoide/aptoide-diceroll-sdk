package com.aptoide.diceroll.sdk.core.analytics

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

class UserIdManager(context: Context) {

    private val masterKeyAlias =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

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
            Log.e("USERID", "getUserId: first UUID generated: $userId")
            sharedPreferences.edit { putString(USER_ID_KEY, userId) }
        } else {
            Log.e("USERID", "getUserId: saved UUID: $userId")
        }
        return userId
    }

    companion object {
        private const val USER_ID_KEY = "user_id"
    }
}
