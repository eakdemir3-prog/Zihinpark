package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow

enum class GameCategory(val title: String, val color: Color, val emoji: String) {
    LOGIC("Mantık & Çıkarım", NeonPink, "🧠"),
    SPATIAL("Uzamsal Zekâ", NeonCyan, "📐"),
    MATH("Sayısal & Analitik", NeonGreen, "🧮"),
    ATTENTION("Dikkat & Gözlem", NeonYellow, "🔍"),
    STRATEGY_REFLEX("Strateji & Hız", NeonPurple, "⚡");

    companion object {
        fun fromTitle(title: String): GameCategory? = entries.firstOrNull { it.title == title }
    }
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
    /** Kısa "Nasıl oynanır" adımları (oyun ilk açıldığında ve seviye haritasında gösterilir). */
    val howTo: List<String>,
    val isNew: Boolean = false
) {
    val route: String get() = "levels/$id"
}

enum class GameSortOption(val label: String) {
    NAME_ASC("İsim (A-Z)"),
    HIGHEST_SCORE("En Yüksek Skor"),
    DIFFICULTY("Zorluk Derecesi"),
    POPULAR("En Çok Oynanan")
}

object GameRegistry {
    val ALL_GAMES = listOf(
        GameMeta(
            id = "logic_detective", name = "Mantık Dedektifi", category = GameCategory.LOGIC,
            description = "İpuçlarını birleştir, çelişkileri yakala ve suçluyu mantıkla bul.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "🕵️",
            howTo = listOf(
                "Olayı ve ipuçlarını oku.",
                "Her ipucu kesin doğrudur; birlikte tek bir kişiyi işaret ederler.",
                "Suçlu olduğunu düşündüğün şüpheliye dokun."
            )
        ),
        GameMeta(
            id = "spatial_rotation", name = "Zihin Döndürme", category = GameCategory.SPATIAL,
            description = "Şekli zihninde döndür, ayna görüntüsü tuzağına düşmeden doğru hâli seç.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "🔄",
            howTo = listOf(
                "Üstteki şekli istenen açıyla zihninde döndür.",
                "Seçeneklerden bazıları ayna görüntüsüdür; dikkat et.",
                "Doğru döndürülmüş hâle dokun."
            )
        ),
        GameMeta(
            id = "nonogram", name = "Gizli Resim", category = GameCategory.SPATIAL,
            description = "Satır ve sütun ipuçlarıyla ızgarayı doldur, gizli deseni ortaya çıkar.",
            difficulty = GameDifficulty.HARD, iconEmoji = "🖼️",
            howTo = listOf(
                "Sayılar o satırda/sütunda art arda kaç kare dolu olduğunu gösterir.",
                "Doldur kipinde kareleri boya, ✕ kipinde boş olduğundan emin olduklarını işaretle.",
                "Tüm dolu kareleri bul; yanlış boyama hata sayılır."
            )
        ),
        GameMeta(
            id = "cipher_breaker", name = "Şifre Kırıcı", category = GameCategory.LOGIC,
            description = "Sezar çarkını çevir, kaydırılmış harflerin ardındaki kelimeyi çöz.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "🔐",
            howTo = listOf(
                "Şifreli kelimedeki her harf alfabede aynı miktarda kaydırılmıştır.",
                "Çarkı çevirerek kaydırmayı geri al; anlamlı kelime belirince Çöz'e bas.",
                "İpucu kelimenin konusunu söyler."
            )
        ),
        GameMeta(
            id = "river_crossing", name = "İmkânsız Görevler", category = GameCategory.STRATEGY_REFLEX,
            description = "Herkesi karşı kıyıya geçir; kimse kimseyi yemesin.",
            difficulty = GameDifficulty.HARD, iconEmoji = "🛶",
            howTo = listOf(
                "Kayığa binecekleri seç, sonra kayığa dokunup karşıya geç.",
                "Kayıkçı yanında değilken yan yana kalamayacak olanlara dikkat et.",
                "Herkes karşı kıyıya geçince tur biter. Az hamle = çok yıldız."
            )
        ),
        GameMeta(
            id = "shape_architect", name = "Şekil Mimarı", category = GameCategory.SPATIAL,
            description = "Parçaları döndür, yerleştir ve hedef silueti boşluksuz tamamla.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "📐",
            howTo = listOf(
                "Alttan bir parça seç, gerekirse döndür.",
                "Parçanın sol üst karesini koymak istediğin hücreye dokun.",
                "Yerleşmiş parçaya dokunursan geri alınır. Silueti tamamen doldur."
            )
        ),
        GameMeta(
            id = "reverse_thinking", name = "Tersine Düşün", category = GameCategory.LOGIC,
            description = "Bilmeceler, ayna saatleri ve tersten işlemlerle kalıpların dışına çık.",
            difficulty = GameDifficulty.EASY, iconEmoji = "💡",
            howTo = listOf(
                "Soruyu dikkatle oku; ilk akla gelen cevap çoğu zaman tuzaktır.",
                "Ayna sorularında görüntüyü zihninde çevir.",
                "Tersten işlemlerde sondan başa doğru geri git."
            )
        ),
        GameMeta(
            id = "math_engineer", name = "Sayı Mühendisi", category = GameCategory.MATH,
            description = "Kesişen denklemlerdeki boşlukları her satır ve sütunu doğrulayacak şekilde doldur.",
            difficulty = GameDifficulty.HARD, iconEmoji = "🧮",
            howTo = listOf(
                "Her satır soldan sağa, her sütun yukarıdan aşağıya bir işlemdir.",
                "Boş kutuya dokun, sonra alttaki tuşlardan sayı seç.",
                "İşlemler sırayla, soldan sağa hesaplanır."
            )
        ),
        GameMeta(
            id = "detail_hunter", name = "Detay Avcısı", category = GameCategory.ATTENTION,
            description = "İki tablo arasındaki farkları süre dolmadan yakala.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "🔍",
            howTo = listOf(
                "Üstteki orijinal tabloyu alttakiyle karşılaştır.",
                "Farklı olan kareye alt tabloda dokun.",
                "Boş yere dokunmak hata sayılır."
            )
        ),
        GameMeta(
            id = "path_finder", name = "Yolun Sonu", category = GameCategory.STRATEGY_REFLEX,
            description = "Renkli anahtarları topla, kapıları aç ve en kısa rotayla hedefe ulaş.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "🧭",
            howTo = listOf(
                "Yön tuşlarıyla ya da kaydırarak hareket et.",
                "Kapıdan geçmek için aynı renkteki anahtarı önce topla.",
                "🏁 hedefine ulaş. En kısa yola yakın bitirirsen 3 yıldız."
            )
        ),
        GameMeta(
            id = "number_chain", name = "Sayı Zinciri", category = GameCategory.MATH,
            description = "Dizinin gizli kuralını keşfet ve eksik halkayı bul.",
            difficulty = GameDifficulty.EASY, iconEmoji = "🔢",
            howTo = listOf(
                "Sayılar arasındaki ilişkiyi bul: fark, çarpan, kare, iç içe diziler...",
                "Soru işaretinin yerine gelecek sayıyı seç.",
                "Hızlı ve hatasız cevap daha çok puan getirir."
            )
        ),
        GameMeta(
            id = "word_hunt", name = "Kelime Avı", category = GameCategory.ATTENTION,
            description = "Harf ızgarasında saklanan Türkçe kelimeleri parmağınla çiz.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "📝",
            howTo = listOf(
                "Listedeki kelimeleri ızgarada bul.",
                "İlk harften son harfe kadar parmağını sürükle.",
                "Üst seviyelerde kelimeler çapraz ve ters de yazılır."
            )
        ),
        GameMeta(
            id = "memory_lights", name = "Hafıza Işıkları", category = GameCategory.ATTENTION,
            description = "Yanan ışıkların sırasını aklında tut ve hatasız tekrarla.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "✨",
            howTo = listOf(
                "Işıkların yanma sırasını izle.",
                "Sıra sana gelince aynı sırayla dokun.",
                "Seviye arttıkça ışık sayısı ve hız artar."
            )
        ),
        GameMeta(
            id = "reflex_arena", name = "Refleks Arenası", category = GameCategory.STRATEGY_REFLEX,
            description = "Işık yeşile döndüğü an dokun; sahte sinyallere kanma.",
            difficulty = GameDifficulty.EASY, iconEmoji = "⚡",
            howTo = listOf(
                "Ekran yeşil olduğunda olabildiğince hızlı dokun.",
                "Erken dokunursan ya da sahte renge (sarı/mor) dokunursan hata olur.",
                "Hedef süre seviyeye göre kısalır."
            )
        ),
        GameMeta(
            id = "color_stroop", name = "Renk Kaçışı", category = GameCategory.ATTENTION,
            description = "Kelimenin anlamı ile rengi çatışınca odağını koru.",
            difficulty = GameDifficulty.HARD, iconEmoji = "🎨",
            howTo = listOf(
                "Üstteki kural ne istiyorsa onu seç: yazının RENGİ ya da kelimenin ANLAMI.",
                "Kural üst seviyelerde tur içinde değişebilir.",
                "Süre dolmadan hedef sayıda doğru cevap ver."
            )
        ),
        GameMeta(
            id = "pattern_master", name = "Desen Ustası", category = GameCategory.SPATIAL,
            description = "Satırlardaki şekil, sayı ve renk kurallarını çöz; eksik deseni seç.",
            difficulty = GameDifficulty.EASY, iconEmoji = "🔷",
            howTo = listOf(
                "Her satırda şekil, renk ve adet belli bir kurala uyar.",
                "Kuralı bul ve sağ alttaki boşluğa uyan deseni seç.",
                "Üst seviyelerde birden fazla kural aynı anda değişir."
            )
        ),
        GameMeta(
            id = "jigsaw_puzzle", name = "Parçaları Birleştir", category = GameCategory.SPATIAL,
            description = "Karışmış neon tablonun parçalarını yer değiştirerek birleştir.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "🧩",
            howTo = listOf(
                "Bir parçaya, sonra yer değiştirmek istediğin parçaya dokun.",
                "Doğru yerine oturan parça kilitlenir ve parlar.",
                "Üst seviyelerde parçalar dönük gelir; çift dokunuşla çevir."
            )
        ),
        GameMeta(
            id = "hidden_object", name = "Gizli Nesneyi Bul", category = GameCategory.ATTENTION,
            description = "Kalabalık sahnede aranan nesneleri gözden kaçırmadan bul.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "🔎",
            howTo = listOf(
                "Alttaki listede aranan nesneler var.",
                "Sahnede onlara dokun; yanlış dokunuş hata sayılır.",
                "Üst seviyelerde nesneler küçülür ve benzerleri artar."
            )
        ),
        GameMeta(
            id = "number_merge", name = "Sayı Patlatma", category = GameCategory.MATH,
            description = "Aynı sayıları kaydırarak birleştir, hedef sayıya hamle bitmeden ulaş.",
            difficulty = GameDifficulty.HARD, iconEmoji = "💥",
            howTo = listOf(
                "Tahtayı dört yöne kaydır; aynı sayılar çarpışınca birleşir.",
                "Her hamleden sonra yeni bir taş gelir.",
                "Hedef sayıyı hamle sınırı dolmadan oluştur."
            )
        ),
        GameMeta(
            id = "logic_doors", name = "Doğru Kapıyı Seç", category = GameCategory.LOGIC,
            description = "Kapı yazılarından hangilerinin doğru olduğunu çöz, güvenli kapıyı aç.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "🚪",
            howTo = listOf(
                "Sadece bir kapı güvenlidir.",
                "Üstteki kural kaç yazının doğru olduğunu söyler.",
                "Her olasılığı dene, kuralla çelişmeyen tek kapıyı seç."
            )
        ),
        GameMeta(
            id = "target_tap", name = "Hedefe Dokun", category = GameCategory.STRATEGY_REFLEX,
            description = "Komuta uyan hedeflere dokun, tuzaklardan kaç.",
            difficulty = GameDifficulty.EASY, iconEmoji = "🎯",
            howTo = listOf(
                "Üstteki komut hangi hedeflere dokunacağını söyler.",
                "Yanlış hedefe dokunmak hata sayılır.",
                "Süre dolmadan gereken sayıda isabet yap."
            )
        ),
        GameMeta(
            id = "maze_escape", name = "Labirentten Kaçış", category = GameCategory.STRATEGY_REFLEX,
            description = "Zaman dolmadan labirentin çıkışını bul; sis yükseldikçe yolunu hatırla.",
            difficulty = GameDifficulty.HARD, iconEmoji = "🏃",
            howTo = listOf(
                "Kaydırarak ya da yön tuşlarıyla ilerle.",
                "Çıkışa süre bitmeden ulaş.",
                "Üst seviyelerde sis yalnızca çevreni gösterir."
            )
        ),
        GameMeta(
            id = "memory_cards", name = "Neon Hafıza Kartları", category = GameCategory.ATTENTION,
            description = "Kartları çevir, neon eşleri hatırla ve tüm çiftleri en az hamleyle bul.",
            difficulty = GameDifficulty.MEDIUM, iconEmoji = "🃏",
            howTo = listOf(
                "İki kart çevir; aynıysa açık kalır.",
                "Farklıysa kapanırlar; yerlerini aklında tut.",
                "Tüm çiftleri süre dolmadan, az hamleyle bul."
            ),
            isNew = true
        )
    )

    fun getGameById(id: String): GameMeta? = ALL_GAMES.find { it.id == id }
}
