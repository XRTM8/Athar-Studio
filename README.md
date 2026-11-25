# أَثَــر | Athar Studio

<p align="center">
  <img src="https://img.shields.io/badge/Architecture-Kotlin%20%2B%20Jetpack%20Compose-E5C07B?style=for-the-badge" alt="Architecture" />
  <img src="https://img.shields.io/badge/Platform-Android%20(Universal%20ABI)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0%2B)-blue?style=for-the-badge" alt="Min SDK" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-lightgrey?style=for-the-badge" alt="License" />
</p>

أداة تصميم وكتابة عربية لنظام أندرويد مبنية بلغة **Kotlin** و**Jetpack Compose**، تركز على ضبط الكشيدة العربية السياقية، الطبقات المتعددة، معالجة الصور والأفلام والأقنعة، والتصدير حتى دقة 4K (2160p).

---

[خطة استقرار سلسلة 2.7 للدعم طويل الأمد](LTS.md)

## الميزات الفعلية

1. **محرك الكشيدة وموازنة الفقرات (`PureArabicKashidaEngine.kt`, `ArabicParagraphJustifier.kt`):**
   * تحليل سياق الكلمة العربية لاختيار مواضع التطويل مع الحفاظ على التشكيل ولام الألف ومنع المواضع غير الصحيحة.
   * موازنة الفقرات بالقياس الفعلي لأبعاد الخط (`Pixel Justification` و`Optical Block Balance`).

2. **الخطوط العربية والمخصصة (`AtharFontManager.kt`, `SfntFontParser.kt`):**
   * خطوط عربية مرفقة برخصة SIL OFL 1.1 تشمل خطوطاً متغيرة (`Cairo`) ومتعددة الأوزان (`Tajawal`, `Alexandria`, `Readex Pro`, وغيرها).
   * استيراد خطوط `.ttf` و`.otf` وتجميع أوزان العائلة الواحدة وتضمينها داخل حزمة المشروع (`.athar`).

3. **مسار معالجة الخلفية والكولاج الموحد (`AtharBackgroundPipeline.kt`):**
   * تطابق المعالجة بين المعاينة والتصدير (PNG/JPEG بدقة 1080p و2160p وPDF): ضبط لوني، تمويه، تدرج لوني، 3D LUT لأفلام التصوير (`FilmPresetEngine`)، خلط قنوات HSL (`AtharColorMixEngine`)، إضافات عدسات (`AtharOpticalLensEngine`)، وهج وحبيبات وGlitch، وأقنعة ضوئية ودخانية.
   * كولاج متعدد الخانات مع تأطير باللمس لكل صورة (تغطية 1:1، تدوير 90°، وعكس أفقي) وتصحيح اتجاه EXIF.

4. **تصدير الصور والمستندات (`export/`):**
   * تصدير صور كاملة (JPEG) أو ملصقات شفافة مقصوصة تلقائياً (PNG).
   * تصدير صفحات PDF للطباعة (`AtharPdfExporter`).
   * تصدير النصوص والتدرجات متجهياً كـ SVG مع تحويل النصوص المقوسة والخطوط المخصصة إلى `<path>` (`AtharSvgExporter`) — لا يشمل صور الخلفية أو الفلاتر النقطية.

5. **إدارة المشاريع والتحديث (`project/`, `updater/`):**
   * حفظ المشاريع كأرشيف مضغوط (`.athar`) وتحديث المشروع المفتوح موضعياً مع تنظيف الأصول المؤقتة، وتضمين الخطوط المخصصة والصور.
   * فحص التحديثات عبر GitHub Releases مع التحقق من رابط HTTPS وبصمة شهادة توقيع الحزمة قبل التثبيت.

---

## هيكلية المشروع

```text
DESIGN/
├── android-app/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/app/xrtm8/athar/
│   │   │   │   ├── core/         # الكشيدة، معالجة الصور، الأفلام 3D LUT، العدسات، الأقنعة، الكولاج
│   │   │   │   ├── typography/   # قياس النصوص، إدارة الخطوط، محلل SFNT، النص المقوس
│   │   │   │   ├── model/        # نماذج الحالة والطبقات والفلاتر
│   │   │   │   ├── export/       # التصدير (PNG/JPEG/PDF/SVG) والحفظ في MediaStore
│   │   │   │   ├── project/      # حفظ/قراءة .athar وتجزئة JSON والمعاينة المصغرة
│   │   │   │   ├── updater/      # فحص وتثبيت التحديثات مع التحقق من الشهادة
│   │   │   │   └── ui/           # الشاشات، اللوحات، المعاينة، وطبقة ViewModel/viewmodel
│   │   │   ├── assets/           # الخطوط (.ttf) وأصول الأقنعة (.webp)
│   │   │   └── res/              # الموارد والأيقونات
│   │   ├── proguard-rules.pro
│   │   └── build.gradle.kts
│   └── settings.gradle.kts
│
└── .github/workflows/
    └── release.yml
```

---

## البناء والاختبار

### المتطلبات:
* **JDK 17**
* **Android SDK** (compileSdk 34 / targetSdk 34)

### تشغيل الاختبارات وفحص الكود:
```powershell
cd android-app
.\gradlew.bat testReleaseUnitTest lintRelease
```

### بناء حزمة الإنتاج الموقعة:
```powershell
cd android-app
.\gradlew.bat assembleRelease
```

---

## الترخيص
المشروع مرخص تحت رخصة **GNU General Public License v3.0**.
