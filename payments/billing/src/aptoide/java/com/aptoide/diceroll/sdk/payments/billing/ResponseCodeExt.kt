package com.aptoide.diceroll.sdk.payments.billing

import com.aptoide.sdk.billing.ResponseCode

fun Int.toResponseCode(): ResponseCode {
    for (code in ResponseCode.values()) {
        if (code.value == this) {
            return code
        }
    }
    throw IllegalArgumentException("Invalid ResponseCode value: $this")
}
