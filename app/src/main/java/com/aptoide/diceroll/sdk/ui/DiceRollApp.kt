package com.aptoide.diceroll.sdk.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.aptoide.diceroll.sdk.core.ui.widgets.components.DiceRollNavigationBarItem
import com.aptoide.diceroll.sdk.feature.payments.ui.PaymentScreen
import com.aptoide.diceroll.sdk.feature.roll_game.data.model.Subscription
import com.aptoide.diceroll.sdk.feature.roll_game.data.model.SubscriptionPrefs
import com.aptoide.diceroll.sdk.feature.roll_game.ui.widgets.DiceSelectionDialog
import com.aptoide.diceroll.sdk.navigation.DiceRollNavHost
import com.aptoide.diceroll.sdk.navigation.TopLevelDestination
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState.PaymentIdle
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun DiceRollApp(
    paymentState: PaymentState,
    onPaymentDialogDismissed: () -> Unit,
    diceSelectionDialogVisibilityState: Boolean,
    subscriptionPrefs: SubscriptionPrefs,
    onDiceSelectionDialogDismissed: () -> Unit,
    onDiceSelected: (Subscription) -> Unit
) {
    val appState: DiceRollAppState = rememberDiceRollAppState()
    Scaffold(
        bottomBar = {
            DiceRollBottomBar(
                destinations = appState.topLevelDestinations,
                onNavigateToDestination = appState::navigateToTopLevelDestination,
                currentDestination = appState.currentDestination,
            )
        },
    ) { scaffoldPadding ->
        DiceRollNavHost(
            appState.navController,
            appState.bottomSheetNavigator,
            scaffoldPadding = scaffoldPadding,
        )
    }
    if (paymentState != PaymentIdle) {
        PaymentScreen(paymentState) {
            onPaymentDialogDismissed()
        }
    }

    if (diceSelectionDialogVisibilityState) {
        DiceSelectionDialog(subscriptionPrefs, onDiceSelectionDialogDismissed, onDiceSelected)
    }
}

@Composable
fun DiceRollBottomBar(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
) {
    NavigationBar(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        destinations.forEach { destination ->
            DiceRollNavigationBarItem(
                selected = currentDestination.isTopLevelDestinationInHierarchy(destination),
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.iconTextId)) },
            )
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination) =
    this?.hierarchy?.any {
        it.route?.contains(destination.name, true) ?: false
    } ?: false
