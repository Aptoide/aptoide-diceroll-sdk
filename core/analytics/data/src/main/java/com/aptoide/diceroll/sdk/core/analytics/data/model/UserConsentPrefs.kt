package com.aptoide.diceroll.sdk.core.analytics.data.model

data class UserConsentPrefs(
    val userConsentState: ConsentState
)

enum class ConsentState {
    ACCEPTED,
    DECLINED,
    UNKNOWN
}
