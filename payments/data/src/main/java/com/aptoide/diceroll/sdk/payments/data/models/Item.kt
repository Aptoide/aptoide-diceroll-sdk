package com.aptoide.diceroll.sdk.payments.data.models

import android.content.Context
import com.aptoide.diceroll.sdk.core.ui.design.R
import com.aptoide.diceroll.sdk.payments.data.models.InternalResponseCode.DEVELOPER_ERROR
import com.aptoide.diceroll.sdk.payments.data.models.InternalResponseCode.SERVICE_UNAVAILABLE
import com.aptoide.diceroll.sdk.payments.data.models.InternalResponseCode.USER_CANCELED
import com.aptoide.diceroll.sdk.payments.data.models.InternalResponseCode as ResponseCode
import com.aptoide.diceroll.sdk.payments.data.models.InternalSkuType as SkuType

/**
 * Payment item in game to represent and match a given SKU.
 */
sealed class Item(
    open val sku: String,
    open val type: String,
) {
    sealed class ConsumableItem(override val sku: String) : Item(sku, SkuType.INAPP.value)

    sealed class SubscriptionItem(override val sku: String) : Item(sku, SkuType.SUBS.value) {
        abstract fun getExpirationMessage(context: Context): String
    }

    data object Attempts : ConsumableItem(ATTEMPTS_SKU) {
        override fun getErrorMessage(context: Context, responseCode: ResponseCode): String =
            when (responseCode) {
                USER_CANCELED -> context.resources.getString(R.string.payment_item_attempts_error_message_user_cancelled)
                SERVICE_UNAVAILABLE -> context.resources.getString(R.string.payment_item_general_error_message_service_unavailable)
                DEVELOPER_ERROR -> context.resources.getString(R.string.payment_item_general_error_message_developer_error)
                else -> context.resources.getString(R.string.payment_item_general_error_message_unknown)
            }

        override fun getRefundMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_attempts_refund_message)

        override fun getSuccessMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_attempts_success_message)
    }

    data object NonConsumableAttempts : ConsumableItem(NON_CONSUMABLE_ATTEMPTS_SKU) {
        override fun getErrorMessage(context: Context, responseCode: ResponseCode): String =
            when (responseCode) {
                USER_CANCELED -> context.resources.getString(R.string.payment_item_non_consumable_attempts_error_message_user_cancelled)
                SERVICE_UNAVAILABLE -> context.resources.getString(R.string.payment_item_general_error_message_service_unavailable)
                DEVELOPER_ERROR -> context.resources.getString(R.string.payment_item_general_error_message_developer_error)
                else -> context.resources.getString(R.string.payment_item_general_error_message_unknown)
            }

        override fun getRefundMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_non_consumable_attempts_refund_message)

        override fun getSuccessMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_non_consumable_attempts_success_message)
    }

    data object GoldDice : SubscriptionItem(GOLD_DICE_SKU) {
        override fun getErrorMessage(context: Context, responseCode: ResponseCode): String =
            when (responseCode) {
                USER_CANCELED -> context.resources.getString(R.string.payment_item_golden_dice_error_message_user_cancelled)
                SERVICE_UNAVAILABLE -> context.resources.getString(R.string.payment_item_general_error_message_service_unavailable)
                DEVELOPER_ERROR -> context.resources.getString(R.string.payment_item_general_error_message_developer_error)
                else -> context.resources.getString(R.string.payment_item_general_error_message_unknown)
            }

        override fun getExpirationMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_golden_dice_expiration_message)

        override fun getRefundMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_golden_dice_refund_message)

        override fun getSuccessMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_golden_dice_success_message)
    }

    data object TestGreenDice : SubscriptionItem(TEST_GREEN_DICE_SKU) {
        override fun getErrorMessage(context: Context, responseCode: ResponseCode): String =
            when (responseCode) {
                USER_CANCELED -> context.resources.getString(R.string.payment_item_golden_dice_error_message_user_cancelled)
                SERVICE_UNAVAILABLE -> context.resources.getString(R.string.payment_item_general_error_message_service_unavailable)
                DEVELOPER_ERROR -> context.resources.getString(R.string.payment_item_general_error_message_developer_error)
                else -> context.resources.getString(R.string.payment_item_general_error_message_unknown)
            }

        override fun getExpirationMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_golden_dice_expiration_message)

        override fun getRefundMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_golden_dice_refund_message)

        override fun getSuccessMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_golden_dice_success_message)
    }

    data object TrialDice : SubscriptionItem(TRIAL_DICE_SKU) {
        override fun getErrorMessage(context: Context, responseCode: ResponseCode): String =
            when (responseCode) {
                USER_CANCELED -> context.resources.getString(R.string.payment_item_golden_dice_error_message_user_cancelled)
                SERVICE_UNAVAILABLE -> context.resources.getString(R.string.payment_item_general_error_message_service_unavailable)
                DEVELOPER_ERROR -> context.resources.getString(R.string.payment_item_general_error_message_developer_error)
                else -> context.resources.getString(R.string.payment_item_general_error_message_unknown)
            }

        override fun getExpirationMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_golden_dice_expiration_message)

        override fun getRefundMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_golden_dice_refund_message)

        override fun getSuccessMessage(context: Context): String =
            context.resources.getString(R.string.payment_item_golden_dice_success_message)
    }

    abstract fun getSuccessMessage(context: Context): String

    abstract fun getErrorMessage(context: Context, responseCode: ResponseCode): String

    abstract fun getRefundMessage(context: Context): String

    open fun getErrorTitle(context: Context, responseCode: ResponseCode): String =
        when (responseCode) {
            USER_CANCELED -> context.resources.getString(R.string.payment_item_general_error_title_user_cancelled)
            else -> context.resources.getString(R.string.payment_item_general_error_title_unknown)
        }

    companion object {
        const val ATTEMPTS_SKU = "attempts"

        const val NON_CONSUMABLE_ATTEMPTS_SKU = "non_consumable_attempts"

        const val GOLD_DICE_SKU = "golden_dice"

        const val TEST_GREEN_DICE_SKU = "test_green_dice"

        const val TRIAL_DICE_SKU = "trial_dice"

        fun fromSku(sku: String): Item? =
            when (sku) {
                ATTEMPTS_SKU -> Attempts
                NON_CONSUMABLE_ATTEMPTS_SKU -> NonConsumableAttempts
                GOLD_DICE_SKU -> GoldDice
                TEST_GREEN_DICE_SKU -> TestGreenDice
                TRIAL_DICE_SKU -> TrialDice
                else -> null
            }

        fun getGeneralErrorMessage(
            context: Context,
            item: Item?,
            responseCode: ResponseCode
        ): String =
            item?.getErrorMessage(context, responseCode) ?: when (responseCode) {
                USER_CANCELED -> context.resources.getString(R.string.payment_item_general_error_message_user_cancelled)
                SERVICE_UNAVAILABLE -> context.resources.getString(R.string.payment_item_general_error_message_service_unavailable)

                DEVELOPER_ERROR -> context.resources.getString(R.string.payment_item_general_error_message_developer_error)
                else -> context.resources.getString(R.string.payment_item_general_error_message_unknown)
            }

        fun getGeneralErrorTitle(
            context: Context,
            item: Item?,
            responseCode: ResponseCode
        ): String =
            item?.getErrorTitle(context, responseCode) ?: if (responseCode == USER_CANCELED) {
                context.resources.getString(R.string.payment_item_general_error_title_user_cancelled)
            } else {
                context.resources.getString(R.string.payment_item_general_error_title_unknown)
            }
    }
}
