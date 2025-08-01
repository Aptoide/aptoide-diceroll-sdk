package com.aptoide.diceroll.sdk.feature.payments.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aptoide.diceroll.sdk.core.ui.design.theme.DiceRollTheme
import com.aptoide.diceroll.sdk.feature.payments.ui.widgets.ErrorState
import com.aptoide.diceroll.sdk.feature.payments.ui.widgets.LoadingState
import com.aptoide.diceroll.sdk.feature.payments.ui.widgets.PaymentDialog
import com.aptoide.diceroll.sdk.feature.payments.ui.widgets.SuccessState
import com.aptoide.diceroll.sdk.payments.data.models.Item.GoldDice
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState.PaymentError
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState.PaymentIdle
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState.PaymentLoading
import com.aptoide.diceroll.sdk.payments.data.models.PaymentState.PaymentSuccess

@Composable
fun PaymentScreen(
    paymentState: PaymentState,
    onDismiss: () -> Unit
) {
    PaymentDialog(onDismiss) {
        when (paymentState) {
            is PaymentIdle -> Unit
            is PaymentLoading -> LoadingState()
            is PaymentError -> ErrorState(paymentState.item, paymentState.responseCode, onDismiss)
            is PaymentSuccess -> SuccessState(paymentState.item, onDismiss)
        }
    }
}

@Preview
@Composable
private fun PreviewStatsContent() {
    DiceRollTheme(darkTheme = false) {
        PaymentScreen(PaymentSuccess(GoldDice)) {}
    }
}
