package com.aptoide.diceroll.sdk.feature.settings.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aptoide.diceroll.sdk.core.analytics.data.model.ConsentState
import com.aptoide.diceroll.sdk.core.analytics.data.model.UserConsentPrefs
import com.aptoide.diceroll.sdk.core.analytics.data.usecases.GetUserConsentPrefsUseCase
import com.aptoide.diceroll.sdk.core.analytics.managers.AnalyticsManager
import com.aptoide.diceroll.sdk.feature.settings.data.model.ThemeConfig
import com.aptoide.diceroll.sdk.feature.settings.data.model.UserPrefs
import com.aptoide.diceroll.sdk.feature.settings.data.repository.UserPrefsDataSource
import com.aptoide.diceroll.sdk.feature.settings.ui.SettingsUiState.Loading
import com.aptoide.diceroll.sdk.feature.settings.ui.SettingsUiState.Success
import com.aptoide.diceroll.sdk.feature.stats.data.model.DiceRoll
import com.aptoide.diceroll.sdk.feature.stats.data.usecases.GetDiceRollsUseCase
import com.aptoide.diceroll.sdk.payments.billing.SdkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsDataSource: UserPrefsDataSource,
    private val sdkManager: SdkManager,
    getDiceRollsUseCase: GetDiceRollsUseCase,
    getUserConsentPrefsUseCase: GetUserConsentPrefsUseCase,
    private val analyticsManager: AnalyticsManager,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(
            userPrefsDataSource.getUserPrefs(),
            getUserConsentPrefsUseCase(),
            getDiceRollsUseCase()
        ) { userPrefs, userConsentPrefs, diceRollList ->
            Success(userPrefs, userConsentPrefs, diceRollList)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loading,
        )

    fun updateThemeConfig(themeConfig: ThemeConfig) {
        viewModelScope.launch {
            userPrefsDataSource.saveThemeConfig(themeConfig)
        }
    }

    fun launchAppUpdateDialog(context: Context) {
        sdkManager.launchAppUpdateDialog(context)
    }

    fun updateConsent(accepted: Boolean) {
        viewModelScope.launch {
            val state = if (accepted) ConsentState.ACCEPTED else ConsentState.DECLINED
            analyticsManager.onPrivacyPolicyUpdated(state)
        }
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val userPrefs: UserPrefs,
        val userConsentPrefs: UserConsentPrefs,
        val diceRollList: List<DiceRoll>,
    ) : SettingsUiState
}
