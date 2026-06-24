package com.aptoide.diceroll.sdk.payments.billing

object Skus {
    val INAPPS =
        listOf(
            "attempts",
            "non_consumable_attempts",
            "legendary_dice",
            "non_existent"
        )
    val SUBS =
        listOf(
            "golden_dice",
            "test_green_dice",
            "trial_dice"
        )

    /**
     * One-time INAPP products that must be acknowledged (not consumed) so they stay owned and keep
     * being returned by [queryPurchasesAsync]. A non-consumable that is neither consumed nor
     * acknowledged is auto-refunded by Aptoide.
     */
    val NON_CONSUMABLE_SKUS = setOf("legendary_dice")
}
