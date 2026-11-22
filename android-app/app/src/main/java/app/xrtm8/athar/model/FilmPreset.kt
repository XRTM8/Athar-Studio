package app.xrtm8.athar.model

enum class FilmPreset(
    val id: String,
    val titleAr: String,
    val brand: String,
    val description: String
) {
    NONE("none", "بدون فلتر", "أصلي", "الصورة الطبيعية بدون تعديل لوني"),
    FUJI_CLASSIC_CHROME("fuji_classic_chrome", "فوجي كروم كلاسيك", "Fujifilm", "ألوان هادئة وسينمائية مع تباين ناعم للظلال"),
    FUJI_VELVIA("fuji_velvia", "فوجي فيلفيا 50", "Fujifilm", "تشبع فائق وألوان حية حارة للطبيعة والسماء"),
    FUJI_PROVIA("fuji_provia", "فوجي بروفيا 100F", "Fujifilm", "تدرج متزن وألوان حقيقية فائقة الواقعية"),
    KODAK_PORTRA_400("kodak_portra_400", "كوداك بورترا 400", "Kodak", "دفء ناعم وألوان بشرة ذهبية ساحرة"),
    KODAK_TRI_X("kodak_tri_x", "كوداك تري-إكس 400", "Kodak", "أبيض وأسود عالي التباين بطابع وثائقي عتيق"),
    LEICA_MONOCHROME("leica_mono", "لايكا مونوكروم", "Leica", "أبيض وأسود نقي بتدرجات رمادية ناعمة وعميقة"),
    CINEMATIC_TEAL_ORANGE("teal_orange", "سينمائي تيل & أورانج", "Cinema", "التدرج الهوليوودي العصري بين الأزرق والبرتقالي"),
    VINTAGE_WARMTH("vintage_warmth", "دفء السبعينات", "Vintage", "ألوان باهتة دافئة مع لمسة ذهبية نوستالجية")
}
