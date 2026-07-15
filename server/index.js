// Hamdel purchase-verification server
//
// Verifies Cafe Bazaar / Myket in-app purchases server-side (never trust a purchase token
// that only the client has checked) and keeps a small persistent record of which install has
// an active subscription/time-pass. See مستندات بازار/بررسی وضعیت اشتراک.txt and
// مستندات مایکت/استفاده از API صحت سنجی خرید.txt for the underlying store APIs.
//
// Run locally:   HAMDEL_SERVER_API_KEY=... BAZAAR_API_TOKEN=... MYKET_ACCESS_TOKEN=... node index.js
// Deploy target:  Liara (or any Node 18+ host reachable from Iran), same approach as Maliar Pro.

const express = require('express');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8080;
const PACKAGE_NAME = process.env.HAMDEL_PACKAGE_NAME || 'com.hamdel.ai';

// Shared secret the Android app must send; keeps randoms from burning your Bazaar/Myket
// API quota through your own server.
const SERVER_API_KEY = process.env.HAMDEL_SERVER_API_KEY || '';

// Cafe Bazaar: static per-app token from پیشخان بازار > API پیشخان بازار > دریافت توکن جدید.
const BAZAAR_API_TOKEN = process.env.BAZAAR_API_TOKEN || '';
// Myket: X-Access-Token from پنل توسعه‌دهندگان مایکت > محصولات درون‌برنامه‌ای.
const MYKET_ACCESS_TOKEN = process.env.MYKET_ACCESS_TOKEN || '';

const BAZAAR_MONTHLY_SKU = process.env.BAZAAR_MONTHLY_SKU || 'com.hamdel.ai';
const BAZAAR_YEARLY_SKU = process.env.BAZAAR_YEARLY_SKU || 'hamdel_yearly';
const MYKET_MONTHLY_SKU = process.env.MYKET_MONTHLY_SKU || 'com.hamdel.ai';
const MYKET_YEARLY_SKU = process.env.MYKET_YEARLY_SKU || 'hamdel_yearly';
const PLAN_DURATION_MS = {
  [BAZAAR_MONTHLY_SKU]: 30 * 24 * 60 * 60 * 1000,
  [BAZAAR_YEARLY_SKU]: 365 * 24 * 60 * 60 * 1000,
  [MYKET_MONTHLY_SKU]: 30 * 24 * 60 * 60 * 1000,
  [MYKET_YEARLY_SKU]: 365 * 24 * 60 * 60 * 1000,
};

const DATA_FILE = path.join(__dirname, 'data', 'entitlements.json');

function loadEntitlements() {
  try {
    return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
  } catch {
    return {};
  }
}

function saveEntitlements(entitlements) {
  fs.mkdirSync(path.dirname(DATA_FILE), { recursive: true });
  fs.writeFileSync(DATA_FILE, JSON.stringify(entitlements, null, 2), 'utf8');
}

function planForSku(store, sku) {
  if (sku === (store === 'bazaar' ? BAZAAR_YEARLY_SKU : MYKET_YEARLY_SKU)) return 'yearly';
  if (sku === (store === 'bazaar' ? BAZAAR_MONTHLY_SKU : MYKET_MONTHLY_SKU)) return 'monthly';
  return null;
}

function requireServerKey(req, res, next) {
  if (!SERVER_API_KEY) {
    return res.status(500).json({ error: 'server_misconfigured', message: 'HAMDEL_SERVER_API_KEY is not set' });
  }
  if (req.get('X-Server-Key') !== SERVER_API_KEY) {
    return res.status(401).json({ error: 'unauthorized' });
  }
  next();
}

async function verifyBazaarSubscription(sku, purchaseToken) {
  if (!BAZAAR_API_TOKEN) throw new Error('BAZAAR_API_TOKEN is not configured');
  const url = `https://pardakht.cafebazaar.ir/devapi/v2/api/applications/${PACKAGE_NAME}/subscriptions/${sku}/purchases/${purchaseToken}`;
  const response = await fetch(url, {
    headers: { 'CAFEBAZAAR-PISHKHAN-API-SECRET': BAZAAR_API_TOKEN },
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok || body.error === 'not_found') {
    return { valid: false, reason: body.error_description || body.error || `HTTP ${response.status}` };
  }
  const expiresAt = Number(body.validUntilTimestampMsec);
  const valid = Number.isFinite(expiresAt) && expiresAt > Date.now();
  return { valid, expiresAt: Number.isFinite(expiresAt) ? expiresAt : null };
}

async function verifyMyketPurchase(sku, purchaseToken) {
  if (!MYKET_ACCESS_TOKEN) throw new Error('MYKET_ACCESS_TOKEN is not configured');
  const url = `https://developer.myket.ir/api/partners/applications/${PACKAGE_NAME}/purchases/products/${sku}/verify`;
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'X-Access-Token': MYKET_ACCESS_TOKEN,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ tokenId: purchaseToken }),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    return { valid: false, reason: body.translatedMessage || body.messageCode || `HTTP ${response.status}` };
  }
  // Myket has no auto-renewing subscriptions: these SKUs are sold as consumable "time
  // passes", so on a verified purchase we grant a fixed-length entitlement ourselves.
  const valid = body.purchaseState === 0;
  return { valid, durationMs: PLAN_DURATION_MS[sku] || PLAN_DURATION_MS[MYKET_MONTHLY_SKU] };
}

const app = express();
app.use(express.json());

app.get('/health', (_req, res) => res.json({ ok: true }));

app.post('/api/verify-purchase', requireServerKey, async (req, res) => {
  const { installId, store, sku, purchaseToken } = req.body || {};
  const plan = planForSku(store, sku);
  if (!installId || !purchaseToken || !plan || (store !== 'bazaar' && store !== 'myket')) {
    return res.status(400).json({ error: 'invalid_request' });
  }

  try {
    const result = store === 'bazaar'
      ? await verifyBazaarSubscription(sku, purchaseToken)
      : await verifyMyketPurchase(sku, purchaseToken);

    if (!result.valid) {
      return res.json({ valid: false, plan: 'free' });
    }

    const entitlements = loadEntitlements();
    const existing = entitlements[installId];
    let expiresAt = result.expiresAt;
    if (store === 'myket') {
      // Retrying a verified consumable must not extend it again. A new purchase token extends
      // from any remaining active time so an early renewal is not lost.
      expiresAt = existing?.store === 'myket' && existing.purchaseToken === purchaseToken
        ? existing.expiresAt
        : Math.max(Date.now(), Number(existing?.expiresAt) || 0) + result.durationMs;
    }
    entitlements[installId] = {
      plan,
      store,
      sku,
      purchaseToken,
      expiresAt,
      verifiedAt: Date.now(),
    };
    saveEntitlements(entitlements);

    return res.json({ valid: true, plan, expiresAt });
  } catch (err) {
    return res.status(502).json({ error: 'store_verification_failed', message: err.message });
  }
});

app.get('/api/entitlement/:installId', requireServerKey, (req, res) => {
  const entitlements = loadEntitlements();
  const record = entitlements[req.params.installId];
  if (!record || (record.expiresAt && record.expiresAt < Date.now())) {
    return res.json({ plan: 'free' });
  }
  return res.json({ plan: record.plan, expiresAt: record.expiresAt, store: record.store });
});

app.listen(PORT, () => {
  console.log(`Hamdel purchase server listening on :${PORT}`);
});
