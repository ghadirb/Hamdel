// Hamdel purchase-verification server — Google Apps Script Web App version (free, no hosting cost).
//
// Confirmed reachable from Apps Script's own test: Cafe Bazaar (pardakht.cafebazaar.ir) and
// Myket (developer.myket.ir) both answer with normal HTTP auth errors (403 / 401) rather than
// being network-blocked — unlike Zarinpal, which blocks non-Iranian IPs. So this can run here
// for free instead of on a paid Iranian host like Liara.
//
// SETUP
// 1) Project Settings (gear icon) > Script properties > add:
//      HAMDEL_SERVER_API_KEY  = <any long random string; must match server.properties in the app>
//      BAZAAR_API_TOKEN       = <from پیشخان بازار > برنامه‌تان > API پیشخان بازار > دریافت توکن جدید>
//      MYKET_ACCESS_TOKEN     = <from developer.myket.ir > برنامه‌تان > محصولات درون‌برنامه‌ای>
// 2) Deploy > New deployment > type: Web app > Execute as: Me > Who has access: Anyone.
// 3) Copy the resulting .../exec URL into the app's server.properties as HAMDEL_SERVER_BASE_URL.
//
// LIMITATION: entitlements are stored in PropertiesService, which caps out around ~500 keys /
// 9KB each. Fine for a small number of installs; if Hamdel grows past that, migrate to the
// server/index.js (Node/Express) version in this same folder's parent, deployed on Liara or any
// Node host, which uses a real file/DB instead.

var PACKAGE_NAME = 'com.hamdel.ai';
// Product IDs are scoped to a store panel, so the same identifiers can safely be
// registered in both Bazaar and Myket. Myket treats them as consumable time passes.
var BAZAAR_MONTHLY_SKU = 'com.hamdel.ai';
var BAZAAR_YEARLY_SKU = 'hamdel_yearly';
var MYKET_MONTHLY_SKU = 'com.hamdel.ai';
var MYKET_YEARLY_SKU = 'hamdel_yearly';
var PLAN_DURATION_MS = {};
PLAN_DURATION_MS[BAZAAR_MONTHLY_SKU] = 30 * 24 * 60 * 60 * 1000;
PLAN_DURATION_MS[BAZAAR_YEARLY_SKU] = 365 * 24 * 60 * 60 * 1000;
PLAN_DURATION_MS[MYKET_MONTHLY_SKU] = 30 * 24 * 60 * 60 * 1000;
PLAN_DURATION_MS[MYKET_YEARLY_SKU] = 365 * 24 * 60 * 60 * 1000;

function getConfig(key) {
  return PropertiesService.getScriptProperties().getProperty(key) || '';
}

function planForSku(store, sku) {
  if (sku === (store === 'bazaar' ? BAZAAR_YEARLY_SKU : MYKET_YEARLY_SKU)) return 'yearly';
  if (sku === (store === 'bazaar' ? BAZAAR_MONTHLY_SKU : MYKET_MONTHLY_SKU)) return 'monthly';
  return null;
}

function jsonOutput(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

// Apps Script web apps expose a single URL and can't read custom HTTP headers, so the shared
// secret and the "which operation" flag both travel inside the POST body instead.
function doPost(e) {
  var body;
  try {
    body = JSON.parse(e.postData.contents);
  } catch (err) {
    return jsonOutput({ error: 'invalid_json' });
  }

  var expectedKey = getConfig('HAMDEL_SERVER_API_KEY');
  if (!expectedKey || body.serverKey !== expectedKey) {
    return jsonOutput({ error: 'unauthorized' });
  }

  if (body.action === 'verify') return handleVerify(body);
  if (body.action === 'entitlement') return handleEntitlement(body);
  return jsonOutput({ error: 'unknown_action' });
}

function handleVerify(body) {
  var installId = body.installId, store = body.store, sku = body.sku, purchaseToken = body.purchaseToken;
  var plan = planForSku(store, sku);
  if (!installId || !purchaseToken || !plan || (store !== 'bazaar' && store !== 'myket')) {
    return jsonOutput({ error: 'invalid_request' });
  }

  var result;
  try {
    result = store === 'bazaar' ? verifyBazaar(sku, purchaseToken) : verifyMyket(sku, purchaseToken);
  } catch (err) {
    return jsonOutput({ error: 'store_verification_failed', message: String(err) });
  }

  if (!result.valid) return jsonOutput({ valid: false, plan: 'free' });

  var existing = loadEntitlement(installId);
  var expiresAt = result.expiresAt;
  if (store === 'myket') {
    // A consumable token can be retried when the app resumes or network fails. Grant it once:
    // the same token keeps its original expiry; a genuinely new token extends from active time.
    if (existing && existing.store === 'myket' && existing.purchaseToken === purchaseToken) {
      expiresAt = existing.expiresAt;
    } else {
      expiresAt = Math.max(Date.now(), Number(existing && existing.expiresAt) || 0) + result.durationMs;
    }
  }
  saveEntitlement(installId, {
    plan: plan,
    store: store,
    sku: sku,
    purchaseToken: purchaseToken,
    expiresAt: expiresAt,
    verifiedAt: Date.now()
  });
  return jsonOutput({ valid: true, plan: plan, expiresAt: expiresAt });
}

function handleEntitlement(body) {
  var record = loadEntitlement(body.installId);
  if (!record || (record.expiresAt && record.expiresAt < Date.now())) {
    return jsonOutput({ plan: 'free' });
  }
  return jsonOutput({ plan: record.plan, expiresAt: record.expiresAt, store: record.store });
}

function verifyBazaar(sku, purchaseToken) {
  var token = getConfig('BAZAAR_API_TOKEN');
  var url = 'https://pardakht.cafebazaar.ir/devapi/v2/api/applications/' + PACKAGE_NAME +
    '/subscriptions/' + sku + '/purchases/' + purchaseToken;
  var res = UrlFetchApp.fetch(url, {
    method: 'get',
    headers: { 'CAFEBAZAAR-PISHKHAN-API-SECRET': token },
    muteHttpExceptions: true
  });
  var json = JSON.parse(res.getContentText() || '{}');
  if (res.getResponseCode() !== 200 || json.error) return { valid: false, expiresAt: null };
  var expiresAt = Number(json.validUntilTimestampMsec);
  return { valid: expiresAt > Date.now(), expiresAt: expiresAt };
}

function verifyMyket(sku, purchaseToken) {
  var token = getConfig('MYKET_ACCESS_TOKEN');
  var url = 'https://developer.myket.ir/api/partners/applications/' + PACKAGE_NAME +
    '/purchases/products/' + sku + '/verify';
  var res = UrlFetchApp.fetch(url, {
    method: 'post',
    headers: { 'X-Access-Token': token, 'Content-Type': 'application/json' },
    payload: JSON.stringify({ tokenId: purchaseToken }),
    muteHttpExceptions: true
  });
  var json = JSON.parse(res.getContentText() || '{}');
  if (res.getResponseCode() !== 200) return { valid: false, expiresAt: null };
  var valid = json.purchaseState === 0;
  return { valid: valid, durationMs: PLAN_DURATION_MS[sku] || PLAN_DURATION_MS[MYKET_MONTHLY_SKU] };
}

function saveEntitlement(installId, record) {
  PropertiesService.getScriptProperties().setProperty('ent_' + installId, JSON.stringify(record));
}

function loadEntitlement(installId) {
  var raw = PropertiesService.getScriptProperties().getProperty('ent_' + installId);
  return raw ? JSON.parse(raw) : null;
}

// Quick manual smoke test (same idea as the one you already ran) — select this function in the
// Apps Script editor's function dropdown and hit Run to sanity-check config without the app.
function manualSmokeTest() {
  Logger.log(JSON.stringify(verifyBazaar(BAZAAR_MONTHLY_SKU, 'test-token')));
  Logger.log(JSON.stringify(verifyMyket(MYKET_MONTHLY_SKU, 'test-token')));
}
