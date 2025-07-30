package com.aptoide.diceroll.sdk.feature.store.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.aptoide.diceroll.sdk.core.navigation.buildDestinationRoute
import com.aptoide.diceroll.sdk.core.navigation.destinations.Destinations
import com.aptoide.diceroll.sdk.core.navigation.navigateToDestination
import com.aptoide.diceroll.sdk.feature.store.ui.StoreRoute

fun NavController.navigateToStoreScreen(navOptions: NavOptions) {
    this.navigateToDestination(
        destination = Destinations.Screen.Store,
        navOptions = navOptions
    )
}

fun NavGraphBuilder.storeRoute() {
    this.buildDestinationRoute(
        destination = Destinations.Screen.Store,
    ) {
        StoreRoute()
    }
}
