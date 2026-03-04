package com.aptoide.diceroll.sdk.core.analytics.managers

import android.app.Activity
import android.content.Context
import android.telephony.TelephonyManager
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.aptoide.diceroll.sdk.core.analytics.data.model.ConsentState
import com.aptoide.diceroll.sdk.core.analytics.data.model.UserConsentPrefs
import com.aptoide.diceroll.sdk.core.analytics.data.usecases.GetUserConsentPrefsUseCase
import com.aptoide.diceroll.sdk.core.analytics.data.usecases.SaveUserConsentPrefsUseCase
import com.aptoide.diceroll.sdk.core.analytics.ui.widgets.ConsentBanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

class ConsentManager @Inject constructor(
    private val getUserConsentPrefsUseCase: GetUserConsentPrefsUseCase,
    private val saveUserConsentPrefsUseCase: SaveUserConsentPrefsUseCase,
) {

    fun requestConsent(
        activity: Activity,
        onConsentFinished: (isGdprSubject: Boolean, consentState: ConsentState) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.Main + Job())

        scope.launch {
            val currentPrefs = getUserConsentPrefsUseCase().first()
            val isGdprZone = isGdprSubject(activity)

            if (!isGdprZone) {
                saveUserConsentPrefsUseCase(UserConsentPrefs(ConsentState.ACCEPTED))
                onConsentFinished(false, ConsentState.ACCEPTED)
                return@launch
            }

            if (currentPrefs.userConsentState != ConsentState.UNKNOWN) {
                onConsentFinished(true, currentPrefs.userConsentState)
                return@launch
            }

            showConsentOverlay(activity) { accepted ->
                scope.launch {
                    val state = if (accepted) ConsentState.ACCEPTED else ConsentState.DECLINED
                    val userConsentPrefs = UserConsentPrefs(state)
                    saveUserConsentPrefsUseCase(userConsentPrefs)
                    onConsentFinished(true, state)
                }
            }
        }
    }

    fun isGdprSubject(context: Context): Boolean {
        val gdprCountries = setOf(
            "AT",
            "BE",
            "BG",
            "HR",
            "CY",
            "CZ",
            "DK",
            "EE",
            "FI",
            "FR",
            "DE",
            "GR",
            "HU",
            "IE",
            "IT",
            "LV",
            "LT",
            "LU",
            "MT",
            "NL",
            "PL",
            "PT",
            "RO",
            "SK",
            "SI",
            "ES",
            "SE",
            "GB",
            "IS",
            "LI",
            "NO",
            "CH"
        )
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val country = telephonyManager.simCountryIso.uppercase().ifBlank {
            telephonyManager.networkCountryIso.uppercase().ifBlank {
                Locale.getDefault().country.uppercase()
            }
        }
        return gdprCountries.contains(country)
    }

    private fun showConsentOverlay(activity: Activity, onDecision: (Boolean) -> Unit) {
        val rootLayout = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val composeView = ComposeView(activity).apply {
            setContent {
                ConsentBanner(
                    onAccept = {
                        rootLayout.removeView(this)
                        onDecision(true)
                    },
                    onDecline = {
                        rootLayout.removeView(this)
                        onDecision(false)
                    }
                )
            }
        }
        rootLayout.addView(composeView)
    }
}
