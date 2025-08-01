package com.aptoide.diceroll.sdk.payments.billing.data.respository

import android.content.Context
import com.aptoide.diceroll.sdk.core.network.modules.api.PurchaseValidatorApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PurchaseValidatorRepository @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val purchaseValidatorApi: PurchaseValidatorApi,
) {

    suspend fun isPurchaseValid(
        sku: String,
        purchaseToken: String
    ): Result<Boolean> =
        runCatching {
            withContext(Dispatchers.IO) {
                val result = purchaseValidatorApi.getPurchaseValidationState(
                    packageName = context.packageName,
                    sku = sku,
                    purchaseToken = purchaseToken
                )

                result.body().equals("true")
            }
        }
}
