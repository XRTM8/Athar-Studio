# مَــدَى | Mada Design Studio

<p align="center">
  <img src="https://img.shields.io/badge/Architecture-Rust%20Core%20%2B%20Kotlin%20Compose-E5C07B?style=for-the-badge" alt="Architecture" />
  <img src="https://img.shields.io/badge/Platform-Android%20(ARM64--v8a)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge" alt="License" />
</p>

تطبيق تصميم وطباعة عربية عالي الأداء لأجهزة أندرويد، مبني بنواة مكتوبة بلغة **Rust** لمعالجة الخطوط والمتجهات وشيدرز الرسوميات، مع واجهة مستخدم تفاعلية حديثة كلياً بلغة **Kotlin (Jetpack Compose)**.

---

## 🌟 الميزات الجوهرية (التفوق الهندسي على التطبيقات المماثلة)

1. **المَدّ المتجهي الذكي (Smart Kashida):**
   * بدلاً من الحلول السطحية التي تكرر رمز التطويل (`ـ` / `U+0640`)، يعتمد التطبيق على خوارزمية مورفولوجية في محرك Rust تحدد بدقة نقاط الاتصال المسموح بمدها وفق قواعد الخط العربي الأصيل، وتحافظ على زاوية القلم وثبات سماكة الحرف.
2. **محرك الشيدرز والمؤثرات الفيزيائية (PBR Shaders):**
   * **الذهب الملكي (Royal Gold):** تذهيب عربي فاخر بتدرجات شمسية ولمعان دافئ.
   * **الكروم السائل (Liquid Chrome):** انعكاسات معدنية حادة ومصقولة ثلاثية الأبعاد.
   * **الزجاج المصنفر (Frosted Glass):** شفافية سينمائية مع تشتت وانكسار ضوئي.
   * **النيون المتوهج (Cyber Neon):** إشعاع ضوئي متعدد الطبقات (Multi-layer Glow).
   * **الظل السينمائي (Cinematic Shadow):** بروز ثلاثي الأبعاد مع ظل ممتد في الفضاء.
   * **الأبيض الناصع (Minimal Luxury):** بساطة ونقاء مستوحى من مدرسة التصميم السويسري.
3. **وضع النص خلف العنصر (Text Behind Subject):**
   * دعم الطبقات المتعددة لعزل العناصر ووضع النصوص في الفضاء ثلاثي الأبعاد.
4. **نظام التحديث الذاتي الهادئ (Gentle In-App Updater):**
   * فحص دوري في الخلفية لأحدث الإصدارات عبر GitHub Releases دون أي تعطيل للمستخدم.
   * إشعارات وبانرات خفيفة وغير متطفلة تتيح التحديث الفوري أو التأجيل.
   * تنزيل الحزمة والتثبيت التلقائي بنقرة واحدة عبر `FileProvider`.
5. **مفتاح توقيع دائمي أصلي (Permanent Production Keystore):**
   * الحزمة موقعة بمفتاح إنتاجي أصلي (RSA 4096-bit) بصلاحية تمتد لـ **100 عام (36,500 يوم)** لضمان استمرارية التحديثات دون أي تعارض في التوقيع.
6. **منع تضارب الحزم (Anti-Collision Namespace):**
   * اعتماد المعرّف المعكوس الفريد `app.xrtm8.mada` لضمان عدم حدوث أي تضارب مع أي تطبيق آخر.

---

## 📁 هيكلية المشروع (Architecture)

```text
DESIGN/
├── core-engine/               # نواة Rust عالية الأداء
│   ├── Cargo.toml
│   └── src/
│       ├── typography/        # تشكيل الخطوط وتفعيل OpenType عبر RustyBuzz
│       ├── kashida/           # حسابات الكشيدة والمد البارامتري
│       └── jni_bridge.rs      # واجهات JNI فائقة السرعة مع كوتلن
│
├── android-app/               # تطبيق أندرويد (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/app/xrtm8/mada/
│   │   │   │   ├── core/      # استدعاء مكتبة libmada_core.so عبر JNI
│   │   │   │   ├── model/     # نماذج البيانات وحالة الكانفاس
│   │   │   │   ├── updater/   # نظام التحديث الذاتي وتنزيل الـ APK
│   │   │   │   └── ui/        # شاشات ومكونات Compose التفاعلية
│   │   │   └── jniLibs/       # مكتبة الـ SO الثنائية المترجمة لـ ARM64-v8a
│   │   └── build.gradle.kts
│   └── settings.gradle.kts
│
├── keystore/                  # مفتاح التوقيع الإنتاجي الدائمي (100 سنة)
│   ├── mada-release.jks
│   └── keystore.properties
│
└── .github/workflows/         # خط أنابيب البناء والتوزيع التلقائي (CI/CD)
```

---

## 🛠️ متطلبات البناء اليدوي (Build Instructions)

1. **Rust & Cargo** مع حزمة `aarch64-linux-android` وأداة `cargo-ndk`.
2. **OpenJDK 17**.
3. **Android SDK & NDK r26b**.

### خطوات البناء:
```powershell
# 1. بناء مكتبة Rust لنظام ARM64:
cd core-engine
cargo ndk -t arm64-v8a -o ../android-app/app/src/main/jniLibs build --release

# 2. بناء حزمة الـ APK الموقعة:
cd ../android-app
./gradlew assembleRelease
```
ستجد ملف الـ APK الموقع والجاهز للتثبيت في:
`android-app/app/build/outputs/apk/release/app-release.apk`
