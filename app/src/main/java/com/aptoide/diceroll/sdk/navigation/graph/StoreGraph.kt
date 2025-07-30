package com.aptoide.diceroll.sdk.navigation.graph

import androidx.navigation.NavGraphBuilder
import com.aptoide.diceroll.sdk.feature.store.ui.navigation.storeRoute

internal fun NavGraphBuilder.storeGraph() {
    storeRoute()
}
