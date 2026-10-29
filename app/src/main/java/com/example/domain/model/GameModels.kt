package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MintGreenDark
import com.example.ui.theme.PastelBlueDark
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralVariant

enum class GameCategory(val title: String, val color: Color) {
    LOGIC("Mantık & Çıkarım", PrimaryCoral),
    SPATIAL("Uzamsal Zekâ", PastelBlueDark),
    MATH("Sayısal & Analitik", MintGreenDark),
    ATTENTION("Dikkat & Gözlem", PrimaryCoralVariant),
    STRATEGY_REFLEX("Strateji & Hız", Color(0xFFD97706))
}

enum class GameDifficulty(val label: String) {
    EASY("Kolay"),
    MEDIUM("Orta"),
    HARD("Zor")
}

data class GameMeta(
    val id: String,
    val name: String,
    val category: GameCategory,
    val description: String,
    val difficulty: GameDifficulty,
    val iconEmoji: String,
    val route: String,
    val maxScore: Int = 100
)

enum class GameSortOption(val label: String) {
    NAME_ASC("İsim (A-Z)"),
    HIGHEST_SCORE("En Yüksek Skor"),
    DIFFICULTY("Zorluk Derecesi"),
    POPULAR("En Çok Oynanan")
}

object GameRegistry {
    val ALL_GAMES = listOf(
        // 1. Mantık Dedektifi
        GameMeta(
            id = "logic_detective",
            name = "Mantık Dedektifi",
            category = GameCategory.LOGIC,
            description = "İpuçlarını incele, şüphelileri analiz et ve suçluyu mantıksal çıkarımla bul.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "🕵️‍♂️",
            route = "game/logic_detective"
        ),
        // 2. Zihin Döndürme
        GameMeta(
            id = "spatial_rotation",
            name = "Zihin Döndürme",
            category = GameCategory.SPATIAL,
            description = "Şekli zihninde belirtilen açıyla döndür ve doğru yansımayı yakala.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "🔄",
            route = "game/spatial_rotation"
        ),
        // 3. Gizli Resim (Nonogram)
        GameMeta(
            id = "nonogram",
            name = "Gizli Resim (Nonogram)",
            category = GameCategory.SPATIAL,
            description = "Sayı ipuçlarını takip ederek ızgarayı doldur, gizli resmi ortaya çıkar.",
            difficulty = GameDifficulty.HARD,
            iconEmoji = "🖼️",
            route = "game/nonogram"
        ),
        // 4. Şifre Kırıcı
        GameMeta(
            id = "cipher_breaker",
            name = "Şifre Kırıcı",
            category = GameCategory.LOGIC,
            description = "Sezar kaydırma algoritmasıyla şifrelenmiş gizli kelimeleri çöz.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "🔐",
            route = "game/cipher_breaker"
        ),
        // 5. İmkânsız Görevler (Nehir Geçişi)
        GameMeta(
            id = "river_crossing",
            name = "İmkânsız Görevler",
            category = GameCategory.STRATEGY_REFLEX,
            description = "Kurt, koyun ve otu kimse birbirini yemeden karşı kıyıya güvenle geçir.",
            difficulty = GameDifficulty.HARD,
            iconEmoji = "🛶",
            route = "game/river_crossing"
        ),
        // 6. Şekil Mimarı
        GameMeta(
            id = "shape_architect",
            name = "Şekil Mimarı",
            category = GameCategory.SPATIAL,
            description = "Geometrik parçaları döndür ve hedef silueti eksiksiz tamamla.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "📐",
            route = "game/shape_architect"
        ),
        // 7. Tersine Düşün
        GameMeta(
            id = "reverse_thinking",
            name = "Tersine Düşün",
            category = GameCategory.LOGIC,
            description = "Alışılmışın dışına çık, paradoksları ve yanal düşünme sorularını yanıtla.",
            difficulty = GameDifficulty.EASY,
            iconEmoji = "💡",
            route = "game/reverse_thinking"
        ),
        // 8. Sayı Mühendisi
        GameMeta(
            id = "math_engineer",
            name = "Sayı Mühendisi",
            category = GameCategory.MATH,
            description = "Kesişen denklem matrisinde eksik işlem ve sayıları hesaplayarak doldur.",
            difficulty = GameDifficulty.HARD,
            iconEmoji = "🧮",
            route = "game/math_engineer"
        ),
        // 9. Detay Avcısı
        GameMeta(
            id = "detail_hunter",
            name = "Detay Avcısı",
            category = GameCategory.ATTENTION,
            description = "İki görsel kompozisyon arasındaki gizli farkları gözlemleyerek bul.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "🔍",
            route = "game/detail_hunter"
        ),
        // 10. Yolun Sonu (Labirent)
        GameMeta(
            id = "path_finder",
            name = "Yolun Sonu",
            category = GameCategory.STRATEGY_REFLEX,
            description = "Engelleri aş, anahtarı ele geçir, kilitli kapıyı açarak hedefe ulaş.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "🧭",
            route = "game/path_finder"
        ),
        // 11. Sayı Zinciri
        GameMeta(
            id = "number_chain",
            name = "Sayı Zinciri",
            category = GameCategory.MATH,
            description = "Sayı örüntüsündeki gizli matematik kuralını keşfet ve soru işaretini bul.",
            difficulty = GameDifficulty.EASY,
            iconEmoji = "🔢",
            route = "game/number_chain"
        ),
        // 12. Kelime Avı
        GameMeta(
            id = "word_hunt",
            name = "Kelime Avı",
            category = GameCategory.ATTENTION,
            description = "Harf matrisi içerisinde gizlenmiş Türkçe kelimeleri seçerek keşfet.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "📝",
            route = "game/word_hunt"
        ),
        // 13. Hafıza Işıkları
        GameMeta(
            id = "memory_lights",
            name = "Hafıza Işıkları",
            category = GameCategory.ATTENTION,
            description = "Işık ve ton sıralamasını aklında tut, sırayı bozmadan hatasız tekrarla.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "✨",
            route = "game/memory_lights"
        ),
        // 14. Refleks Arenası
        GameMeta(
            id = "reflex_arena",
            name = "Refleks Arenası",
            category = GameCategory.STRATEGY_REFLEX,
            description = "Işık yeşile döndüğü milisaniyede dokun, tepki hızını test et.",
            difficulty = GameDifficulty.EASY,
            iconEmoji = "⚡",
            route = "game/reflex_arena"
        ),
        // 15. Renk Kaçışı (Stroop)
        GameMeta(
            id = "color_stroop",
            name = "Renk Kaçışı",
            category = GameCategory.ATTENTION,
            description = "Yazı rengi ile kelime anlamı çeliştiğinde beyninin hızını ve odağını koru.",
            difficulty = GameDifficulty.HARD,
            iconEmoji = "🎨",
            route = "game/color_stroop"
        ),
        // 16. Desen Ustası
        GameMeta(
            id = "pattern_master",
            name = "Desen Ustası",
            category = GameCategory.SPATIAL,
            description = "Geometrik ve sembolik dönüşüm serisini takip et, sıradaki deseni seç.",
            difficulty = GameDifficulty.EASY,
            iconEmoji = "🧩",
            route = "game/pattern_master"
        ),
        // --- 6 YENİ OYUN (PROJE GENİŞLETME) ---
        // 17. Parçaları Birleştir (Jigsaw Puzzle)
        GameMeta(
            id = "jigsaw_puzzle",
            name = "Parçaları Birleştir",
            category = GameCategory.SPATIAL,
            description = "Bulmaca parçalarını mantıksal ve uzamsal olarak doğru yuvalara yerleştir.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "🧩",
            route = "game/jigsaw_puzzle"
        ),
        // 18. Gizli Nesneyi Bul (Hidden Object)
        GameMeta(
            id = "hidden_object",
            name = "Gizli Nesneyi Bul",
            category = GameCategory.ATTENTION,
            description = "Karmaşık ve ayrıntılı görsel sahne içindeki gizli eşyaları dikkatle tespit et.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "🔎",
            route = "game/hidden_object"
        ),
        // 19. Sayı Patlatma (Number Merge)
        GameMeta(
            id = "number_merge",
            name = "Sayı Patlatma",
            category = GameCategory.MATH,
            description = "Aynı sayıdaki taşları stratejik olarak birleştirerek daha büyük sayılara ulaş.",
            difficulty = GameDifficulty.HARD,
            iconEmoji = "💥",
            route = "game/number_merge"
        ),
        // 20. Doğru Kapıyı Seç (Logic Doors)
        GameMeta(
            id = "logic_doors",
            name = "Doğru Kapıyı Seç",
            category = GameCategory.LOGIC,
            description = "Tuzaklı kapıların ardındaki ipuçlarını analiz et, güvenli kapıyı doğru seç.",
            difficulty = GameDifficulty.MEDIUM,
            iconEmoji = "🚪",
            route = "game/logic_doors"
        ),
        // 21. Hedefe Dokun (Target Tap)
        GameMeta(
            id = "target_tap",
            name = "Hedefe Dokun",
            category = GameCategory.STRATEGY_REFLEX,
            description = "Ekranda beliren hedef sembol ve renklere zaman kaybetmeden dokun, isabetini artır.",
            difficulty = GameDifficulty.EASY,
            iconEmoji = "🎯",
            route = "game/target_tap"
        ),
        // 22. Labirentten Kaçış (Maze Escape)
        GameMeta(
            id = "maze_escape",
            name = "Labirentten Kaçış",
            category = GameCategory.STRATEGY_REFLEX,
            description = "Karmaşık labirentte çıkış rotasını planla, engelleri aş ve zamana karşı yarış.",
            difficulty = GameDifficulty.HARD,
            iconEmoji = "🏃‍♂️",
            route = "game/maze_escape"
        )
    )

    fun getGameById(id: String): GameMeta? = ALL_GAMES.find { it.id == id }
}
