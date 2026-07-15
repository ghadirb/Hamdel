# سرور تایید خرید همدل

این پوشه دو پیاده‌سازی معادل دارد؛ هر دو کاملاً همان منطق را انجام می‌دهند (خریدهای بازار/مایکت را سمت سرور، نه فقط سمت کلاینت، تایید می‌کنند — طبق مستندات امنیتی هر دو فروشگاه). فقط یکی از این دو را دیپلوی کنید.

| | Apps Script (`apps-script/Code.gs`) | Node/Express (`index.js`) |
|---|---|---|
| هزینه | رایگان | نیاز به هاست (مثلاً لیارا) |
| مناسب برای | شروع کار، تعداد کاربر کم/متوسط | مقیاس بزرگ‌تر یا نیاز به دیتابیس واقعی |
| ذخیره‌سازی | `PropertiesService` (سقف تقریبی ~۵۰۰ کلید) | فایل JSON (قابل تعویض با دیتابیس واقعی) |
| راه‌اندازی | چند دقیقه، در همان script.google.com | نیاز به یک اکانت هاست Node |

**توصیه: با Apps Script شروع کنید.** تست مستقیم نشان داد که هم `pardakht.cafebazaar.ir` (بازار) و هم `developer.myket.ir` (مایکت) از سرورهای گوگل قابل‌دسترسی‌اند (برخلاف زرین‌پال که IPهای خارج ایران را مسدود می‌کند)، پس نیازی به هاست ایرانی/پولی از همان ابتدا نیست.

## گزینه‌ی ۱ (رایگان): Google Apps Script

1. به [script.google.com](https://script.google.com) بروید → «پروژه جدید».
2. محتوای [`apps-script/Code.gs`](apps-script/Code.gs) را کپی کنید.
3. از منوی تنظیمات پروژه (آیکون چرخ‌دنده) → «Script properties» → این سه مقدار را اضافه کنید:
   - `HAMDEL_SERVER_API_KEY` — هر رشته‌ی تصادفی طولانی؛ باید دقیقاً همان مقداری باشد که در `server.properties` اپ می‌گذارید.
   - `BAZAAR_API_TOKEN` — از «پیشخان بازار > برنامه‌تان > API پیشخان بازار > دریافت توکن جدید».
   - `MYKET_ACCESS_TOKEN` — از پنل مایکت، بخش محصولات درون‌برنامه‌ای.
4. «Deploy» → «New deployment» → نوع: **Web app** → Execute as: **Me** → Who has access: **Anyone** → Deploy.
5. آدرس `.../exec` که به شما داده می‌شود را در `server.properties` اپ به عنوان `HAMDEL_SERVER_BASE_URL` بگذارید.
6. برای تست سریع بدون اپ: در ادیتور Apps Script، تابع `manualSmokeTest` را از منوی کشویی انتخاب و اجرا کنید؛ در Logs باید پاسخ‌های ۴۰۱/۴۰۳ (نه خطای شبکه) ببینید.

اگر تعداد کاربران فعال به چند صد نفر رسید و به سقف `PropertiesService` خوردید، بدون تغییر منطق برنامه، کافیست به گزینه‌ی ۲ مهاجرت کنید.

## گزینه‌ی ۲ (مقیاس‌پذیرتر): Node/Express روی لیارا یا مشابه

### راه‌اندازی محلی

```bash
cd server
npm install
cp .env.example .env   # مقادیر واقعی را در .env بگذارید
npm start
```

### استقرار روی لیارا

1. یک اپ Node.js در [Liara](https://liara.ir) بسازید و پوشه‌ی `server/` (بدون `apps-script/`) را دیپلوی کنید.
2. متغیرهای محیطی زیر را در پنل Liara تنظیم کنید: `HAMDEL_SERVER_API_KEY`، `BAZAAR_API_TOKEN`، `MYKET_ACCESS_TOKEN`، و اختیاری `HAMDEL_PACKAGE_NAME`.
3. آدرس نهایی (مثلاً `https://hamdel-server.iran.liara.run`) را در `server.properties` به عنوان `HAMDEL_SERVER_BASE_URL` بگذارید.

توجه: این نسخه از هدر `X-Server-Key` و مسیرهای جدا (`/api/verify-purchase`, `/api/entitlement/:id`) استفاده می‌کند، برخلاف نسخه‌ی Apps Script که همه‌چیز را در بدنه‌ی JSON یک URL واحد می‌فرستد (چون Apps Script هدر سفارشی را نمی‌خواند). اگر بین این دو گزینه جابه‌جا شدید، حتماً `PurchaseVerificationClient.kt` سمت اندروید را هم با پروتکل همان گزینه هماهنگ کنید — این دو فعلاً معادل هم نیستند از نظر شکل درخواست.

## نکات باقی‌مانده برای تولید واقعی

- توکن API بازار و Access Token مایکت را فقط سمت سرور (Script properties یا `server/.env`) نگه دارید؛ هرگز در APK یا مخزن گیت.
- نرخ‌محدودسازی (rate limiting) در هیچ‌کدام پیاده نشده؛ برای نسخه‌ی Node می‌توانید `express-rate-limit` اضافه کنید؛ Apps Script خودش محدودیت روزانه‌ی فراخوانی دارد که برای این حجم معمولاً کافی است.
