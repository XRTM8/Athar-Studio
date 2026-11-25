# أثر | Athar Studio

## العربية

تطبيق أندرويد لإنشاء تصاميم تعتمد على النص والصورة، مع أدوات مخصصة للكتابة العربية. الكود الأساسي في `android-app/`، بلغة Kotlin وواجهة Jetpack Compose.

### الموجود في المستودع

- لوحة تصميم بطبقات نص متعددة، وتحريك وتدوير وتغيير حجم العناصر، مع أدوات للمحاذاة والتباعد والتأثيرات.
- معالجة للنص العربي تشمل اختيار مواضع الكشيدة وضبط الفقرات، وخطوطًا مرفقة مع إمكانية استيراد خطوط TTF وOTF.
- خلفيات من صور أو ألوان أو تدرجات، وكولاج، وأقنعة، وتعديلات لونية، ومؤثرات عدسات وحبيبات ووهج.
- حفظ المشاريع واستعادتها بصيغة `.athar` مع الصور والخطوط المستخدمة.
- تصدير الصور بصيغتي PNG وJPEG، والصفحات بصيغة PDF، والنص والتدرجات بصيغة SVG. تصدير SVG لا يتضمن خلفيات الصور أو المؤثرات النقطية.
- اختبارات وحدة واختبار Android آلي، ومسار لإصدارات GitHub والتحديث داخل التطبيق.

### البنية والبناء

توجد أدوات الرسم والمعالجة في `android-app/app/src/main/java/app/xrtm8/athar/core/`، والتنسيق العربي والخطوط في `typography/`، والحفظ في `project/`، والتصدير في `export/`. التطبيق يستهدف Android 8.0 فأحدث (API 26)، ويستخدم JDK 17 وAndroid SDK 34.

```bash
cd android-app
./gradlew testReleaseUnitTest
./gradlew assembleDebug
```

يتطلب بناء إصدار موقّع إعداد مفتاح التوقيع؛ راجع `android-app/app/build.gradle.kts`. توجد تفاصيل إضافية في [وثيقة البنية](android-app/ARCHITECTURE.md). وثيقة [LTS.md](LTS.md) تسجل خطة صيانة قديمة ويجب قراءتها مع رقم الإصدار الحالي في ملف البناء.

الترخيص: [GPL-3.0](LICENSE).

## English

Athar Studio is an Android app for creating text and image compositions, with tools for Arabic typography. The application lives in `android-app/` and uses Kotlin and Jetpack Compose.

### Repository contents

- A canvas with multiple text layers, positioning, rotation, scaling, alignment, spacing, and visual effects.
- Arabic kashida placement and paragraph justification, bundled fonts, and TTF/OTF import.
- Image, solid-color, and gradient backgrounds; collage layouts, masks, color adjustments, and lens, grain, and glow effects.
- `.athar` project save and restore, including referenced images and custom fonts.
- PNG and JPEG image export, PDF page export, and SVG export for text and gradients. SVG export does not include image backgrounds or raster effects.
- Unit and Android tests, a GitHub release workflow, and an in-app update path.

### Layout and build

Drawing and image processing are in `android-app/app/src/main/java/app/xrtm8/athar/core/`; Arabic text and fonts are in `typography/`, project storage in `project/`, and output formats in `export/`. The minimum Android version is 8.0 (API 26). The build uses JDK 17 and Android SDK 34.

```bash
cd android-app
./gradlew testReleaseUnitTest
./gradlew assembleDebug
```

A signed release requires signing key configuration; see `android-app/app/build.gradle.kts`. See the [architecture notes](android-app/ARCHITECTURE.md). [LTS.md](LTS.md) records an older maintenance plan; check the build file for the current version.

License: [GPL-3.0](LICENSE).


---

للتواصل / Contact: [@MA.XRT](https://www.instagram.com/ma.xrt/)
