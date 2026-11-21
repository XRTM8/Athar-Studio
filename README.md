# أَثَــر | Athar Studio

<p align="center">
  <img src="https://img.shields.io/badge/Architecture-Pure%20Kotlin%20%2B%20Jetpack%20Compose-E5C07B?style=for-the-badge" alt="Architecture" />
  <img src="https://img.shields.io/badge/Platform-Android%20(Universal%20ABI)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0%2B)-blue?style=for-the-badge" alt="Min SDK" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-lightgrey?style=for-the-badge" alt="License" />
</p>

بيئة تصميم وطباعة عربية احترافية ومتقدمة لنظام أندرويد. تم بناؤها بمعمارية حديثة كلياً بلغة **Kotlin** وواجهات **Jetpack Compose** التفاعلية، مع محرك خطوط وكشيدة عربية سياقية تحترم أصول وقواعد الخط العربي الكلاسيكي، ومحرك تصدير فائق الدقة (4K UHD) خالي من تسريبات الذاكرة.

---

## 🌟 الميزات الجوهرية والمعمارية (Core Architectural Features)

1. **محرك الكشيدة العربي السياقي الأصيل (Authentic Arabic Kashida Engine):**
   * كود كوتلن نقي 100% (`PureArabicKashidaEngine.kt`) يحلل سياق الكلمة العربية صرفياً ورسمياً.
   * يدعم أصول الخط العربي: أولوية مد أسنان السين والشين، ما بعد الصاد والضاد، ومواضع ما قبل الحرف الأخير (Pre-Final)، مع منع المدود المحظورة كمد ما بين الميم والجيم أو لام الألف.
   * الحفاظ التام على علامات التشكيل والحركات والرموز القرآنية ملحقة بالحروف الأصلية.

2. **موازنة ومحاذاة الفقرات الموحدة (Unified Paragraph Justifier & Block Balancing):**
   * محرك موحد (`ArabicParagraphJustifier.kt`) يتيح خيارين للمصمم:
     * **المحاذاة المتساوية بالبكسل (Pixel Justification):** قياس دقيق لأطوال السطور بالبكسل وفق أبعاد الخط المستخدم وموازنتها.
     * **الموازنة الكتلية المتناسقة (Optical Block Balance):** حساب الأوزان البصرية للأحرف وتوزيع الكشيدات لتكوين كتلة نصية مستطيلة متناسقة.

3. **سلاسة 120Hz وتصفير التخصيصات (Zero Allocations in onDraw):**
   * تصفير حجز الكائنات (`Zero GC Churn`) في حلقة الرسم التفاعلية عبر كاش داخلي لفلاتر الألوان (`ColorMatrixColorFilter`) والظلال (`BlurMaskFilter`) وأنماط الدمج (`PorterDuffXfermode`).
   * كاش ذكي متزامن لقياس وتقطيع النصوص (`AtharTextLayout.computeLayout`) يلغي إعادة الحسابات أثناء إيماءات اللمس والسحب والتدوير والزووم.

4. **تصدير 4K فائق الدقة بدون انهيارات (Zero-OOM 4K Export Pipeline):**
   * معالجة التغبيش للتصدير بدقة 4K عبر خوارزمية تنقيص ذكية متبوعة بتكبير ثنائي الأبعاد (`Bilinear Filtering`) عبر العتاد، مما يوفر 95% من استهلاك الذاكرة ويمنع انهيار الـ Heap.
   * تسريع قص الملصقات الشفافة (`cropTransparentBitmap`) بخوارزمية مسح متدرج (Step-Stride) تفحص الأطراف في أجزاء من الثانية.

5. **استوديو الذكاء الحسابي المساعد (Designer-Assist Intelligence):**
   * **عزل العنصر ووضع النص خلفه (Subject Depth):** مدعوم بنماذج `Google ML Kit Selfie Segmentation` للعزل العصبي البشري فائق الدقة مع معالجة حواف ناعمة، وخوارزمية تدرج لوني كبديل ذكي للأجسام الصامتة.
   * **التباين البصري التلقائي (Optical Legibility):** تحليل إضاءة وألوان خلفية النص واقتراح ألوان وظلال زجاجية تضمن أعلى مقروئية.
   * **التوزيع المغناطيسي الذكي (Smart Focal Placement):** اكتشاف المساحات الفارغة (Negative Space) ووضع النصوص تلقائياً في المناطق الأكثر اتزاناً بصرياً.

6. **أمان التزامن في رسم المسارات المقوسة (Thread-Safe Curved Text):**
   * عزل كائنات `Path` و `RectF` في `CurvedTextRenderer` باستخدام `ThreadLocal`، مما يمنع حدوث أي تصادم بياني أثناء تصدير الصور في الخلفية بالتزامن مع تفاعل المستخدم على الكانفاس.

7. **إدارة متقدمة للمشاريع (.athar Archive Format):**
   * حفظ المشاريع كحزم مضغوطة تتضمن بيانات التصميم (JSON)، صور الخلفية، ومعاينات مصغرة كاملة الطبقات.
   * تحصين كامل ضد ثغرات المسارات (Zip Slip Protection).
   * سياسة إخلاء دورية للكاش المؤقت لمنع استنزاف ذاكرة الهاتف التخزينية.

8. **نظام التحديث الذاتي الآمن (Secure In-App Updater):**
   * فحص الإصدارات الجديدة عبر واجهة برمجة تطبيقات GitHub Releases وتنزيل التحديث في الكاش الداخلي الآمن مع التحقق من سلامة الحزمة واسم المعرّف.

---

## 📁 هيكلية المشروع (Project Directory Tree)

```text
DESIGN/
├── android-app/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/app/xrtm8/athar/
│   │   │   │   ├── core/         # محرك الكشيدة، العزل العصبي، والذكاء الحسابي
│   │   │   │   ├── typography/   # موازنة الفقرات، كاش النصوص، والخط المقوس
│   │   │   │   ├── model/        # نماذج الطبقات، المؤثرات، وحالة الكانفاس
│   │   │   │   ├── export/       # محرك التصدير بدقة 4K وحفظ الصور
│   │   │   │   ├── project/      # إدارة حزم المشاريع والمعاينات المصغرة
│   │   │   │   ├── updater/      # نظام التحديث الذاتي الآمن
│   │   │   │   └── ui/           # شاشات ومكونات Jetpack Compose التفاعلية
│   │   │   ├── assets/fonts/     # مكتبة الخطوط العربية المضمنة (.ttf)
│   │   │   └── res/              # الموارد والأيقونات والمتجهات
│   │   ├── proguard-rules.pro    # قواعد R8 لضغط الكود وحماية النماذج
│   │   └── build.gradle.kts      # إعدادات البناء والتوقيع الآمن والتقليص
│   └── settings.gradle.kts
│
└── .github/workflows/
    └── release.yml               # خط الإنتاج الآلي (CI/CD) لاختبار وبناء النسخ الرسمية
```

---

## 🛠️ خطوات البناء والتشغيل (Build & Compilation)

### المتطلبات المسبقة:
* **JDK 17** (مثال: Microsoft OpenJDK أو Temurin 17).
* **Android SDK** (API 34 و Build Tools 34.0.0+).

### بناء وتشغيل اختبارات الوحدة:
```powershell
cd android-app
./gradlew testReleaseUnitTest
```

### بناء حزمة الإنتاج الموقعة (Signed Release APK):
```powershell
cd android-app
./gradlew assembleRelease
```

ملف الحزمة الموقع والمضغوط عبر R8 تجده في المسار:
```text
android-app/app/build/outputs/apk/release/app-release.apk
```

---

## 🔒 ترخيص الاستخدام (License)
المشروع مرخص تحت رخصة **GNU General Public License v3.0**.
