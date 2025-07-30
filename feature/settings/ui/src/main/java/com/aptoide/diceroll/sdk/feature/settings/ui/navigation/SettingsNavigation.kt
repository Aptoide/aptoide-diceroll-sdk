package com.aptoide.diceroll.sdk.feature.settings.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.aptoide.diceroll.sdk.core.navigation.buildDestinationRoute
import com.aptoide.diceroll.sdk.core.navigation.destinations.Destinations
import com.aptoide.diceroll.sdk.core.navigation.navigateToDestination
import com.aptoide.diceroll.sdk.feature.settings.ui.SettingsRoute

fun NavController.navigateToSettingsScreen(navOptions: NavOptions) {
    this.navigateToDestination(
        destination = Destinations.Screen.Settings,
        navOptions = navOptions
    )
}

fun NavGraphBuilder.settingsRoute() {
    this.buildDestinationRoute(
        destination = Destinations.Screen.Settings,
    ) {
        SettingsRoute()
    }
}
