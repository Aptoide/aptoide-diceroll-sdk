package com.aptoide.diceroll.sdk.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.aptoide.diceroll.sdk.core.ui.design.DiceRollIcons
import com.aptoide.diceroll.sdk.core.ui.design.R

/**
 * Type for the top level destinations in the application. Each of these destinations
 * can contain one or more screens (based on the window size). Navigation from one screen to the
 * next within a single destination will be handled directly in composables.
 */
enum class TopLevelDestination(
    val icon: ImageVector,
    val iconTextId: Int,
) {
    GAME(
        icon = DiceRollIcons.game,
        iconTextId = R.string.roll_game_title,
    ),
    STORE(
        icon = DiceRollIcons.store,
        iconTextId = R.string.store_title,
    ),
    SETTINGS(
        icon = DiceRollIcons.settings,
        iconTextId = R.string.settings_title,
    ),
}
