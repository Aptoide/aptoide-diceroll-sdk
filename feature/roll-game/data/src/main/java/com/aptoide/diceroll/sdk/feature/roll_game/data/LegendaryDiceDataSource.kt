package com.aptoide.diceroll.sdk.feature.roll_game.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LegendaryDiceDataSource @Inject constructor(
    private val preferences: DataStore<Preferences>,
) {

    /**
     * Updates the ownership status of the Legendary Dice non-consumable.
     */
    suspend fun saveLegendaryDiceStatus(active: Boolean) {
        withContext(Dispatchers.IO) {
            preferences.edit { prefs ->
                prefs[LEGENDARY_DICE_ACTIVE] = active
            }
        }
    }

    /**
     * Stream of Legendary Dice ownership status [Boolean].
     */
    fun getLegendaryDiceStatus(): Flow<Boolean> {
        return preferences.data.map { prefs ->
            prefs[LEGENDARY_DICE_ACTIVE] ?: false
        }.distinctUntilChanged()
    }

    companion object {
        val LEGENDARY_DICE_ACTIVE = booleanPreferencesKey("legendary_dice_active")
    }
}
