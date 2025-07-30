package com.aptoide.diceroll.sdk.payments.data.models

import com.aptoide.diceroll.sdk.payments.data.models.InternalResponseCode as ResponseCode

interface PaymentState {
    data object PaymentIdle : PaymentState
    data object PaymentLoading : PaymentState
    data class PaymentSuccess(val item: Item) : PaymentState
    data class PaymentError(val item: Item?, val responseCode: ResponseCode) : PaymentState
}
