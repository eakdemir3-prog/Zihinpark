package com.example.game.puzzles

/** Kategorili Türkçe kelime havuzu (Şifre Kırıcı, Kelime Avı ve Tersine Düşün kullanır). */
object WordData {
    const val ALPHABET = "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ"

    val CATEGORIES: Map<String, List<String>> = linkedMapOf(
        "Hayvan" to listOf(
            "KEDİ", "KÖPEK", "ASLAN", "KAPLAN", "ZÜRAFA", "FİL", "TAVŞAN", "KARTAL", "BAYKUŞ", "YUNUS",
            "BALİNA", "PENGUEN", "KANGURU", "TİLKİ", "KURT", "AYI", "SİNCAP", "KAPLUMBAĞA", "KELEBEK", "ARI",
            "KARINCA", "ÖRDEK", "HOROZ", "İNEK", "KOYUN", "KEÇİ", "EŞEK", "DEVE", "MAYMUN", "PAPAĞAN",
            "YILAN", "TİMSAH", "AHTAPOT", "ZEBRA", "GERGEDAN", "LEOPAR", "ÇİTA", "KUNDUZ", "SERÇE", "LEYLEK",
            "MARTI", "GEYİK", "KİRPİ", "YARASA", "HAMSİ"
        ),
        "Meyve ve Sebze" to listOf(
            "ELMA", "ARMUT", "KİRAZ", "ÇİLEK", "MUZ", "KARPUZ", "KAVUN", "ÜZÜM", "PORTAKAL", "LİMON",
            "MANDALİNA", "ŞEFTALİ", "KAYISI", "ERİK", "İNCİR", "NAR", "AYVA", "VİŞNE", "DUT", "ANANAS",
            "DOMATES", "BİBER", "PATATES", "SOĞAN", "HAVUÇ", "ISPANAK", "MARUL", "KABAK", "PATLICAN", "FASULYE",
            "SARIMSAK", "BEZELYE", "MANTAR", "PIRASA", "TURP"
        ),
        "Eşya" to listOf(
            "MASA", "SANDALYE", "KALEM", "DEFTER", "KİTAP", "SAAT", "LAMBA", "AYNA", "HALI", "PERDE",
            "YASTIK", "YORGAN", "BARDAK", "TABAK", "KAŞIK", "ÇATAL", "BIÇAK", "TENCERE", "ŞEMSİYE", "ÇANTA",
            "ANAHTAR", "KİLİT", "MAKAS", "CETVEL", "SİLGİ", "DOLAP", "KOLTUK", "TELEFON", "BİLGİSAYAR", "KAMERA",
            "SÜPÜRGE", "FENER", "ÇEKİÇ", "TORNAVİDA", "KUMBARA"
        ),
        "Doğa" to listOf(
            "DENİZ", "DAĞ", "NEHİR", "GÖL", "ORMAN", "ÇÖL", "ADA", "VADİ", "ŞELALE", "MAĞARA",
            "YAĞMUR", "KAR", "RÜZGAR", "BULUT", "GÜNEŞ", "YILDIZ", "GÖKKUŞAĞI", "TOPRAK", "KAYA", "KUM",
            "YAPRAK", "ÇİÇEK", "AĞAÇ", "OKYANUS", "VOLKAN", "BUZUL", "ŞİMŞEK", "FIRTINA", "SİS", "DALGA",
            "TEPE", "KÖRFEZ", "YAYLA", "PINAR", "ÇAĞLAYAN"
        ),
        "Meslek" to listOf(
            "DOKTOR", "ÖĞRETMEN", "MÜHENDİS", "AVUKAT", "HEMŞİRE", "PİLOT", "ŞOFÖR", "AŞÇI", "FIRINCI", "BERBER",
            "TERZİ", "MİMAR", "POLİS", "ASKER", "ÇİFTÇİ", "BALIKÇI", "RESSAM", "YAZAR", "ŞAİR", "MUHASEBECİ",
            "ECZACI", "DİŞÇİ", "VETERİNER", "GAZETECİ", "KAPTAN", "MARANGOZ", "ELEKTRİKÇİ", "BAHÇIVAN", "KUAFÖR", "HAKEM",
            "KUYUMCU", "OYUNCU", "HAKİM", "BEKÇİ", "DALGIÇ"
        ),
        "Spor ve Oyun" to listOf(
            "FUTBOL", "BASKETBOL", "VOLEYBOL", "TENİS", "YÜZME", "KOŞU", "SATRANÇ", "GÜREŞ", "BOKS", "OKÇULUK",
            "KAYAK", "SÖRF", "BİSİKLET", "HENTBOL", "GOLF", "ESKRİM", "DART", "TAVLA", "OKEY", "JUDO",
            "KARATE", "MARATON", "HALTER", "KÜREK", "BİLARDO"
        ),
        "Şehir ve Ülke" to listOf(
            "ANKARA", "İSTANBUL", "İZMİR", "BURSA", "ANTALYA", "KONYA", "TRABZON", "ADANA", "MARDİN", "ERZURUM",
            "SAMSUN", "EDİRNE", "BOLU", "RİZE", "MUĞLA", "ALMANYA", "FRANSA", "İTALYA", "JAPONYA", "MISIR",
            "KAYSERİ", "SİNOP", "ÇORUM", "HATAY", "KARS"
        ),
        "Kavram" to listOf(
            "SEVGİ", "MUTLULUK", "UMUT", "CESARET", "SABIR", "BAŞARI", "ZAMAN", "HAYAL", "MERAK", "DOSTLUK",
            "BARIŞ", "ÖZGÜRLÜK", "ADALET", "HAFIZA", "ZEKA", "MANTIK", "BEYİN", "DÜŞÜNCE", "BİLGİ", "ENERJİ",
            "HUZUR", "GURUR", "AZİM", "EMEK", "DİKKAT"
        ),
        "Yiyecek" to listOf(
            "EKMEK", "PEYNİR", "ZEYTİN", "BAL", "REÇEL", "ÇORBA", "PİLAV", "MAKARNA", "KÖFTE", "BÖREK",
            "BAKLAVA", "LOKUM", "SİMİT", "AYRAN", "ÇAY", "KAHVE", "DONDURMA", "PASTA", "KURABİYE", "MANTI",
            "LAHMACUN", "PİDE", "HELVA", "SUCUK", "YOĞURT"
        ),
        "Bilim ve Uzay" to listOf(
            "GEZEGEN", "UYDU", "ROKET", "ATOM", "MOLEKÜL", "GALAKSİ", "TELESKOP", "MIKNATIS", "ELEKTRİK", "IŞIK",
            "SES", "ROBOT", "LAZER", "UZAY", "ASTRONOT", "METEOR", "YÖRÜNGE", "BAKTERİ", "HÜCRE", "KUTUP",
            "MİKROSKOP", "DENEY", "FORMÜL", "ENZİM", "PROTON"
        )
    )

    /** (kelime, kategori) çiftleri. */
    val ALL: List<Pair<String, String>> = CATEGORIES.flatMap { (cat, words) -> words.map { it to cat } }

    /** Türkçe harf sıklığına yakın dolgu harfleri. */
    const val FILLER = "AAAAEEEEİİİIIKKLLMMNNRRRSSTTUUYYDDBBOOÜÜŞŞÇÇGĞHPZCVFÖJ"
}
