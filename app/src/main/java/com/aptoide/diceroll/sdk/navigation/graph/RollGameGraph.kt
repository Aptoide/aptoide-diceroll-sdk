package com.aptoide.diceroll.sdk.navigation.graph

import androidx.navigation.NavGraphBuilder
import com.aptoide.diceroll.sdk.feature.roll_game.ui.navigation.rollGameRoute

internal fun NavGraphBuilder.rollGameGraph() {
    rollGameRoute()
}
