package com.hamdel.ai.data.billing

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.hamdel.ai.BuildConfig
import com.hamdel.ai.data.remote.PurchaseVerificationClient
import com.hamdel.ai.data.settings.HamdelPreferences
import ir.myket.billingclient.IabHelper
import ir.myket.billingclient.util.IabResult
import ir.myket.billingclient.util.Inventory
import ir.myket.billingclient.util.Purchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class SubscriptionPlan { Free, Monthly, Yearly }

data class SubscriptionState(
    val plan: SubscriptionPlan = SubscriptionPlan.Free,
    val freeCreditsRemaining: Int = 12,
    val billingReady: Boolean = false,
    val status: String = "در حال اتصال به پرداخت...",
    /** Set only once the purchase-verification server has confirmed the entitlement. Null for Free. */
    val expiresAtMillis: Long? = null
) {
    val hasPremiumAccess: Boolean
        get() = plan != SubscriptionPlan.Free && (expiresAtMillis == null || expiresAtMillis > System.currentTimeMillis())
}

/**
 * Store-neutral client billing. Product entitlement is refreshed from the store on every launch;
 * every purchase is additionally sent to our own server (see server/README.md) which verifies the
 * purchase token directly against Bazaar/Myket and is the authoritative source of truth — this is
 * required by both stores' own security guidance and is the only way Myket entitlements (which are
 * plain consumables, not real renewing subscriptions) survive being consumed/restarted.
 */
class SubscriptionManager(
    private val activity: ComponentActivity,
    private val preferences: HamdelPreferences,
    private val verificationClient: PurchaseVerificationClient
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

    fun restorePurchases() {
        refreshInventory()
        refreshFromServer()
    }

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
                    refreshFromServer()
                } else {
                    setStatus("اتصال به ${storeTitle()} ناموفق بود: ${result.message}")
                }
            }
        }
    }

    /**
     * Asks our own server (authoritative — it has actually verified the purchase token with the
     * store) what this install's current entitlement is. If the server isn't configured
     * (HAMDEL_SERVER_BASE_URL unset) this silently does nothing and the client falls back to the
     * store's own inventory query above.
     */
    private fun refreshFromServer() {
        activity.lifecycleScope.launch {
            val entitlement = verificationClient.fetchEntitlement(preferences.installId) ?: return@launch
            val plan = when (entitlement.plan) {
                "yearly" -> SubscriptionPlan.Yearly
                "monthly" -> SubscriptionPlan.Monthly
                else -> SubscriptionPlan.Free
            }
            if (plan == SubscriptionPlan.Free) return@launch
            _state.value = _state.value.copy(
                plan = plan,
                billingReady = true,
                expiresAtMillis = entitlement.expiresAt,
                status = "دسترسی ویژه توسط سرور تایید شده است."
            )
        }
    }

    private fun refreshInventory() {
        val client = helper ?: return
        runCatching {
            // Version 1.19 of the official multi-store client queries owned in-app and
            // subscription products together; it accepts one optional SKU detail list.
            client.queryInventoryAsync(true, listOf(monthlySku, yearlySku), inventoryListener)
        }.onFailure { setStatus("بازیابی خریدها انجام نشد: ${it.message ?: "خطای نامشخص"}") }
    }

    private val inventoryListener = IabHelper.QueryInventoryFinishedListener { result: IabResult, inventory: Inventory ->
        if (!result.isSuccess) {
            setStatus("بازیابی خریدها ناموفق بود: ${result.message}")
        } else {
            val yearlyPurchase = inventory.getPurchase(yearlySku)
            val monthlyPurchase = inventory.getPurchase(monthlySku)
            val purchase = yearlyPurchase ?: monthlyPurchase
            val plan = when {
                yearlyPurchase != null -> SubscriptionPlan.Yearly
                monthlyPurchase != null -> SubscriptionPlan.Monthly
                else -> SubscriptionPlan.Free
            }
            _state.value = _state.value.copy(
                plan = plan,
                billingReady = true,
                freeCreditsRemaining = preferences.freeAiCredits,
                status = if (plan == SubscriptionPlan.Free) "خرید فعالی پیدا نشد." else "دسترسی ویژه فعال است."
            )
            // The store says we own it; ask our server to actually verify the token so the
            // entitlement (with a real expiry) is recorded server-side too.
            if (purchase != null) verifyWithServer(plan, purchase.sku, purchase.token, purchase)
        }
    }

    private val purchaseListener = IabHelper.OnIabPurchaseFinishedListener { result: IabResult, purchase: Purchase? ->
        when {
            result.isFailure && result.response == IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED -> setStatus("پرداخت لغو شد.")
            result.isFailure -> setStatus("پرداخت ناموفق بود: ${result.message}")
            purchase == null -> setStatus("اطلاعات خرید دریافت نشد.")
            else -> {
                val plan = planForSku(purchase.sku)
                _state.value = _state.value.copy(plan = plan, billingReady = true, status = "پرداخت در حال تایید سمت سرور است...")
                verifyWithServer(plan, purchase.sku, purchase.token, purchase)
            }
        }
    }

    /** Sends the purchase token to our server for real verification against Bazaar/Myket. */
    private fun verifyWithServer(
        fallbackPlan: SubscriptionPlan,
        sku: String,
        purchaseToken: String,
        purchase: Purchase? = null
    ) {
        if (!verificationClient.isConfigured) {
            // No server configured yet (e.g. local debug build) — keep the client-only signal
            // but make clear this hasn't actually been verified anywhere.
            setStatus(
                if (BuildConfig.STORE_ID == "myket") "خرید تایید شد. اعتبار زمانی مایکت باید در سرور اعتبارسنجی شود."
                else "خرید تایید شد، اما سرور تایید پیکربندی نشده است."
            )
            return
        }
        activity.lifecycleScope.launch {
            val entitlement = verificationClient.verifyPurchase(
                installId = preferences.installId,
                store = BuildConfig.STORE_ID,
                sku = sku,
                purchaseToken = purchaseToken
            )
            if (entitlement == null) {
                setStatus("تایید خرید توسط سرور ناموفق بود. لطفاً بعداً «بازیابی خریدها» را بزنید.")
                return@launch
            }
            _state.value = _state.value.copy(
                plan = planForServerValue(entitlement.plan, fallbackPlan),
                billingReady = true,
                expiresAtMillis = entitlement.expiresAt,
                status = "پرداخت تایید شد و دسترسی ویژه فعال است."
            )
            // Myket has no subscription product. Its time passes are consumables, so release the
            // store ownership only after our server has verified and saved this exact token.
            if (BuildConfig.STORE_ID == "myket" && purchase != null) consumeMyketPurchase(purchase)
        }
    }

    private fun consumeMyketPurchase(purchase: Purchase) {
        val client = helper ?: return
        runCatching {
            client.consumeAsync(purchase) { _, result ->
                if (!result.isSuccess) {
                    setStatus("دسترسی ویژه فعال شد، اما مصرف خرید مایکت ناموفق بود: ${result.message}")
                }
            }
        }.onFailure {
            setStatus("دسترسی ویژه فعال شد، اما مصرف خرید مایکت انجام نشد: ${it.message ?: "خطای نامشخص"}")
        }
    }

    private fun skuFor(plan: SubscriptionPlan): String? = when (plan) {
        SubscriptionPlan.Monthly -> monthlySku
        SubscriptionPlan.Yearly -> yearlySku
        SubscriptionPlan.Free -> null
    }

    private fun planForSku(sku: String): SubscriptionPlan = if (sku == yearlySku) SubscriptionPlan.Yearly else SubscriptionPlan.Monthly
    private fun planForServerValue(value: String, fallback: SubscriptionPlan): SubscriptionPlan = when (value) {
        "yearly" -> SubscriptionPlan.Yearly
        "monthly" -> SubscriptionPlan.Monthly
        else -> fallback
    }
    private fun storeTitle(): String = if (BuildConfig.STORE_ID == "bazaar") "بازار" else "مایکت"
    private fun setStatus(message: String) { _state.value = _state.value.copy(status = message, freeCreditsRemaining = preferences.freeAiCredits) }
    private val monthlySku: String get() = BuildConfig.MONTHLY_SKU
    private val yearlySku: String get() = BuildConfig.YEARLY_SKU
}
