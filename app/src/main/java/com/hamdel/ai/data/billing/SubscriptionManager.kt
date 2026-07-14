package com.hamdel.ai.data.billing

import android.app.Activity
import androidx.activity.ComponentActivity
import com.hamdel.ai.BuildConfig
import com.hamdel.ai.data.settings.HamdelPreferences
import ir.myket.billingclient.IabHelper
import ir.myket.billingclient.util.IabResult
import ir.myket.billingclient.util.Inventory
import ir.myket.billingclient.util.Purchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

enum class SubscriptionPlan { Free, Monthly, Yearly }

data class SubscriptionState(
    val plan: SubscriptionPlan = SubscriptionPlan.Free,
    val freeCreditsRemaining: Int = 12,
    val billingReady: Boolean = false,
    val status: String = "در حال اتصال به پرداخت..."
) {
    val hasPremiumAccess: Boolean get() = plan != SubscriptionPlan.Free
}

/**
 * Store-neutral client billing. Product entitlement is refreshed from the store on every launch;
 * release deployments should additionally verify purchase tokens on a trusted backend.
 */
class SubscriptionManager(
    private val activity: ComponentActivity,
    private val preferences: HamdelPreferences
) {
    private val _state = MutableStateFlow(
        SubscriptionState(freeCreditsRemaining = preferences.freeAiCredits)
    )
    val state: StateFlow<SubscriptionState> = _state

    private var helper: IabHelper? = null

    init {
        connect()
    }

    fun canUseAi(): Boolean = state.value.hasPremiumAccess || preferences.freeAiCredits > 0

    /** Only call after an online request completed successfully. */
    fun recordSuccessfulAiUse(units: Int = 1) {
        if (state.value.hasPremiumAccess) return
        preferences.freeAiCredits = (preferences.freeAiCredits - units).coerceAtLeast(0)
        _state.value = _state.value.copy(freeCreditsRemaining = preferences.freeAiCredits)
    }

    fun purchase(plan: SubscriptionPlan) {
        val client = helper
        if (client == null || !state.value.billingReady) {
            setStatus("پرداخت هنوز به فروشگاه متصل نشده است.")
            return
        }
        val sku = skuFor(plan)
        if (sku == null) return
        val itemType = if (BuildConfig.STORE_ID == "bazaar") IabHelper.ITEM_TYPE_SUBS else IabHelper.ITEM_TYPE_INAPP
        val payload = "hamdel:${BuildConfig.STORE_ID}:$sku:${UUID.randomUUID()}"
        runCatching {
            client.launchPurchaseFlow(activity, sku, itemType, purchaseListener, payload)
        }.onFailure { setStatus("شروع پرداخت ناموفق بود: ${it.message ?: "خطای نامشخص"}") }
    }

    fun restorePurchases() = refreshInventory()

    fun dispose() {
        helper?.dispose()
        helper = null
    }

    private fun connect() {
        if (BuildConfig.IAB_PUBLIC_KEY.isBlank()) {
            setStatus("کلید عمومی پرداخت ${storeTitle()} هنوز در نسخه انتشار تنظیم نشده است.")
            return
        }
        helper = IabHelper(activity, BuildConfig.IAB_PUBLIC_KEY).also { client ->
            client.enableDebugLogging(BuildConfig.DEBUG)
            client.startSetup { result ->
                if (result.isSuccess) {
                    _state.value = _state.value.copy(billingReady = true, status = "پرداخت ${storeTitle()} آماده است.")
                    refreshInventory()
                } else {
                    setStatus("اتصال به ${storeTitle()} ناموفق بود: ${result.message}")
                }
            }
        }
    }

    private fun refreshInventory() {
        val client = helper ?: return
        runCatching {
            // Version 1.19 of the official multi-store client queries owned in-app and
            // subscription products together; it accepts one optional SKU detail list.
            client.queryInventoryAsync(true, listOf(MONTHLY_SKU, YEARLY_SKU), inventoryListener)
        }.onFailure { setStatus("بازیابی خریدها انجام نشد: ${it.message ?: "خطای نامشخص"}") }
    }

    private val inventoryListener = IabHelper.QueryInventoryFinishedListener { result: IabResult, inventory: Inventory ->
        if (!result.isSuccess) {
            setStatus("بازیابی خریدها ناموفق بود: ${result.message}")
        } else {
            val plan = when {
                inventory.getPurchase(YEARLY_SKU) != null -> SubscriptionPlan.Yearly
                inventory.getPurchase(MONTHLY_SKU) != null -> SubscriptionPlan.Monthly
                else -> SubscriptionPlan.Free
            }
            _state.value = _state.value.copy(
                plan = plan,
                billingReady = true,
                freeCreditsRemaining = preferences.freeAiCredits,
                status = if (plan == SubscriptionPlan.Free) "خرید فعالی پیدا نشد." else "دسترسی ویژه فعال است."
            )
        }
    }

    private val purchaseListener = IabHelper.OnIabPurchaseFinishedListener { result: IabResult, purchase: Purchase? ->
        when {
            result.isFailure && result.response == IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED -> setStatus("پرداخت لغو شد.")
            result.isFailure -> setStatus("پرداخت ناموفق بود: ${result.message}")
            purchase == null -> setStatus("اطلاعات خرید دریافت نشد.")
            else -> {
                val plan = planForSku(purchase.sku)
                _state.value = _state.value.copy(plan = plan, billingReady = true, status = "پرداخت تایید شد و دسترسی ویژه فعال است.")
                // Myket has no auto-renewing subscriptions. These two products are time passes;
                // a production backend must grant the timed entitlement before consuming the token.
                if (BuildConfig.STORE_ID == "myket") setStatus("خرید تایید شد. اعتبار زمانی مایکت باید در سرور اعتبارسنجی شود.")
            }
        }
    }

    private fun skuFor(plan: SubscriptionPlan): String? = when (plan) {
        SubscriptionPlan.Monthly -> MONTHLY_SKU
        SubscriptionPlan.Yearly -> YEARLY_SKU
        SubscriptionPlan.Free -> null
    }

    private fun planForSku(sku: String): SubscriptionPlan = if (sku == YEARLY_SKU) SubscriptionPlan.Yearly else SubscriptionPlan.Monthly
    private fun storeTitle(): String = if (BuildConfig.STORE_ID == "bazaar") "بازار" else "مایکت"
    private fun setStatus(message: String) { _state.value = _state.value.copy(status = message, freeCreditsRemaining = preferences.freeAiCredits) }

    companion object {
        const val MONTHLY_SKU = "hamdel_premium_monthly"
        const val YEARLY_SKU = "hamdel_premium_yearly"
    }
}
