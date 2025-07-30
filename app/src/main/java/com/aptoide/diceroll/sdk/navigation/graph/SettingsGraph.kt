package com.aptoide.diceroll.sdk.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.aptoide.diceroll.sdk.feature.settings.ui.navigation.settingsRoute

internal fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    settingsRoute()
}
