package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.pick
import com.example.game.engine.step
import com.example.game.engine.tierOf
import kotlin.random.Random

/**
 * Tersine Düşün: dört tür soru karışık gelir.
 * - Elle yazılmış bilmece ve tuzak sorular
 * - Ayna saat (görüntüden gerçek saati bul)
 * - Tersten işlem (sonuçtan başlangıç sayısını bul)
 * - Tersten okunan kelime
 */
data class ReversePuzzle(
    val kind: Int,
    val question: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String,
    /** Ayna saat sorusunda saatin gösterdiği dakika (0..719), diğerlerinde -1. */
    val clockMinutes: Int = -1,
    val key: String
)

private class Riddle(val q: String, val a: String, val w1: String, val w2: String, val w3: String, val why: String)

private val RIDDLES = listOf(
    Riddle("Bir babanın 4 kızı var ve her kızın 1 erkek kardeşi var. Bu ailede kaç çocuk var?", "5", "8", "6", "4", "Tek bir erkek kardeş, dört kızın hepsinin kardeşidir."),
    Riddle("Bir yarışta ikinciyi geçtin. Şimdi kaçıncısın?", "İkinci", "Birinci", "Üçüncü", "Sonuncu", "İkinciyi geçen onun yerine geçer."),
    Riddle("Bir yarışta sonuncuyu geçtin. Kaçıncı oldun?", "Bu imkânsız", "Sondan ikinci", "Sonuncu", "Birinci", "Sonuncunun arkasında kimse yoktur, onu geçemezsin."),
    Riddle("Bir çobanın 17 koyunu var. 9'u hariç hepsi kaçtı. Kaç koyunu kaldı?", "9", "8", "17", "0", "\"9'u hariç\" kaçtıysa 9 koyun kalır."),
    Riddle("Bir yılda kaç ayda 28 gün vardır?", "12", "1", "2", "11", "Her ayda en az 28 gün vardır."),
    Riddle("Hangisi daha ağırdır: 1 kilo pamuk mu, 1 kilo demir mi?", "İkisi eşit", "Demir", "Pamuk", "Hava durumuna bağlı", "İkisi de 1 kilodur."),
    Riddle("Elektrikli bir tren kuzeye gidiyor, rüzgâr güneye esiyor. Trenin dumanı hangi yöne gider?", "Dumanı olmaz", "Güneye", "Kuzeye", "Yukarı", "Elektrikli trenin dumanı yoktur."),
    Riddle("Bir horoz çatının tepesinde yumurtlarsa yumurta hangi tarafa düşer?", "Horoz yumurtlamaz", "Sağa", "Sola", "Rüzgârın yönüne", "Yumurtlayan tavuktur."),
    Riddle("Musa gemisine her hayvandan kaç tane aldı?", "Hiç, gemiyi yapan Nuh'tu", "2", "1", "7", "Tufan hikâyesindeki gemi Nuh'un gemisidir."),
    Riddle("1'den 100'e kadar sayılar yazılırsa kaç tane 9 rakamı kullanılır?", "20", "10", "11", "19", "Birler basamağında 10, onlar basamağında 10 tane 9 vardır."),
    Riddle("Doktor sana 3 hap verip her yarım saatte bir içmeni söyledi. Haplar kaç dakikada biter?", "60", "90", "30", "120", "İlki hemen, ikincisi 30. dakikada, üçüncüsü 60. dakikada içilir."),
    Riddle("Karanlık bir odada mum, gaz lambası ve soba var; elinde tek bir kibrit var. İlk neyi yakarsın?", "Kibriti", "Mumu", "Sobayı", "Gaz lambasını", "Önce kibriti yakmalısın."),
    Riddle("Bir tuğla 1 kilo artı yarım tuğla ağırlığındadır. Tuğla kaç kilodur?", "2", "1,5", "1", "3", "Yarım tuğla 1 kilo ise tam tuğla 2 kilodur."),
    Riddle("Bir sopa ve bir top toplam 110 TL. Sopa toptan 100 TL pahalı. Top kaç TL?", "5", "10", "15", "1", "Top 5 TL, sopa 105 TL olur."),
    Riddle("5 makine 5 dakikada 5 parça üretiyor. 100 makine 100 parçayı kaç dakikada üretir?", "5", "100", "20", "1", "Her makine 5 dakikada 1 parça üretir."),
    Riddle("Göldeki nilüferler her gün iki katına çıkıyor ve gölü 48 günde tamamen kaplıyor. Gölün yarısını kaç günde kaplar?", "47", "24", "46", "12", "Son gün iki katına çıktığına göre bir gün önce yarısı doluydu."),
    Riddle("Hangisi tersten okununca da aynı kalır?", "KAYAK", "KALEM", "KİTAP", "KAYIK", "K-A-Y-A-K iki yönden de aynı okunur."),
    Riddle("Yer altında sakallı dede. Nedir?", "Pırasa", "Havuç", "Patates", "Kök", "Kökleri sakala benzeyen pırasadır."),
    Riddle("Ben giderim o gider, arkamdan tıpış tıpış eder. Nedir?", "Gölge", "Kedi", "Rüzgâr", "Ayak sesi", "Gölge peşinden hiç ayrılmaz."),
    Riddle("Ayağı yok yürür, gözü yok ağlar. Nedir?", "Bulut", "Nehir", "Rüzgâr", "Mum", "Bulut gökyüzünde gezer ve yağmur yağdırır."),
    Riddle("Ne kadar çok alırsan o kadar büyüyen şey nedir?", "Çukur", "Borç", "Balon", "Gölge", "Çukurdan toprak aldıkça çukur büyür."),
    Riddle("Kurulandıkça ıslanan şey nedir?", "Havlu", "Sünger", "Şemsiye", "Yağmurluk", "Havlu seni kuruladıkça kendisi ıslanır."),
    Riddle("Kırılmadan kullanılamayan şey nedir?", "Yumurta", "Ceviz", "Bardak", "Kilit", "Yumurtayı kullanmak için önce kırarsın."),
    Riddle("Sana ait olduğu hâlde başkalarının senden daha çok kullandığı şey nedir?", "İsmin", "Telefonun", "Arabanın", "Odan", "İsmini en çok başkaları söyler."),
    Riddle("Elleri var ama alkışlayamaz. Nedir?", "Saat", "Robot", "Ağaç", "Eldiven", "Saatin akrep ve yelkovanına el denir."),
    Riddle("Dişleri var ama ısırmaz. Nedir?", "Tarak", "Testere", "Fermuar", "Kalem", "Tarağın dişleri saçı tarar."),
    Riddle("Yatağı var ama hiç uyumaz. Nedir?", "Nehir", "Hasta", "Bebek", "Kedi", "Nehrin aktığı yere nehir yatağı denir."),
    Riddle("Gözü var ama göremez. Nedir?", "İğne", "Patates", "Kasırga", "Kamera", "İpliğin geçtiği deliğe iğnenin gözü denir."),
    Riddle("Boynu var ama başı yok. Nedir?", "Şişe", "Gömlek", "Kuğu", "Gitar", "Şişenin ince kısmına boyun denir."),
    Riddle("Ayakları var ama yürüyemez. Nedir?", "Masa", "Bebek", "Robot", "Ayakkabı", "Masa ayakları üzerinde durur."),
    Riddle("Sabah dört, öğlen iki, akşam üç ayakla yürüyen nedir?", "İnsan", "Kedi", "Kaplumbağa", "Robot", "Bebekken emekler, büyüyünce yürür, yaşlanınca bastonla yürür."),
    Riddle("Ne kadar çok olursa o kadar az görürsün. Nedir?", "Karanlık", "Işık", "Gözlük", "Sis perdesi", "Karanlık arttıkça görüş azalır."),
    Riddle("Adı söylendiği anda bozulan şey nedir?", "Sessizlik", "Sır", "Söz", "Rüya", "\"Sessizlik\" dediğin an sessizlik bozulur."),
    Riddle("Masada 4 kalem ve 3 elma var. Bir kalem ve iki elma aldın. Kaç elman olur?", "2", "1", "3", "5", "Aldığın elmalar senindir: 2."),
    Riddle("Otobüsü sen kullanıyorsun. İlk durakta 4 kişi indi, 3 kişi bindi. Sonraki durakta 5 kişi indi. Şoförün adı ne?", "Senin adın", "Bilinemez", "Ali", "Durak sayısı", "Otobüsü sen kullanıyorsun, şoför sensin."),
    Riddle("Akrep ile yelkovan bir günde (24 saat) kaç kez üst üste gelir?", "22", "24", "12", "48", "12 saatte 11 kez, 24 saatte 22 kez çakışırlar."),
    Riddle("Saat tam 3:00'te akrep ile yelkovan arasındaki açı kaç derecedir?", "90", "45", "60", "120", "Her saat aralığı 30°, 3 aralık = 90°."),
    Riddle("Çekmecede 4 farklı renkte karışık çorap var. Karanlıkta en az kaç çorap çekersen aynı renkten bir çift garanti olur?", "5", "4", "8", "2", "4 farklı renk çekebilirsin; 5. çorap mutlaka birine eşlenir."),
    Riddle("30 kişilik bir sınıfta en az kaç kişinin doğum günü kesinlikle aynı aydadır?", "3", "2", "12", "1", "30 kişi 12 aya dağılınca bir ayda en az 3 kişi olur."),
    Riddle("Bugün salıysa 100 gün sonra hangi gün olur?", "Perşembe", "Salı", "Çarşamba", "Cuma", "100 = 14 hafta + 2 gün; salıdan 2 gün sonrası perşembe."),
    Riddle("1 km uzunluğundaki bir tren, 1 km'lik tünele saatte 60 km hızla giriyor. Trenin tünelden tamamen çıkması kaç dakika sürer?", "2", "1", "3", "4", "Trenin 2 km yol alması gerekir; dakikada 1 km gider."),
    Riddle("Babamın oğlu ama benim kardeşim değil. Kim?", "Ben", "Kuzenim", "Amcam", "Dedem", "Babamın kardeşim olmayan oğlu benim."),
    Riddle("Bir kurbağa 10 m'lik kuyudan çıkmak için gündüz 3 m tırmanıp gece 2 m kayıyor. Kaçıncı gün çıkar?", "8", "10", "7", "9", "7 günün sonunda 7 m'dedir; 8. gün 3 m tırmanıp çıkar."),
    Riddle("Ali'nin annesinin üç çocuğu var: Nisan, Mayıs ve ...?", "Ali", "Haziran", "Mart", "Temmuz", "Soru Ali'nin annesinden bahsediyor."),
    Riddle("Bir uçak iki ülkenin sınırına düştü. Yaralılar nereye gömülür?", "Yaralılar gömülmez", "Kalkış ülkesine", "Varış ülkesine", "Sınıra", "Yaralılar yaşıyor!"),
    Riddle("Bir adam yağmurda şapkasız ve şemsiyesiz yürüdü ama saçının tek teli ıslanmadı. Neden?", "Keldi", "Yağmur durdu", "Koşuyordu", "Gölgedeydi", "Saçı olmayanın saçı ıslanmaz."),
    Riddle("Dünyanın tam ortasında ne var?", "N harfi", "Çekirdek", "Ekvator", "Magma", "D-Ü-N-Y-A kelimesinin ortasında N vardır."),
    Riddle("İstanbul'un başında ne var?", "İ harfi", "Boğaz", "Galata", "Vali", "İSTANBUL kelimesi İ ile başlar."),
    Riddle("Bir günde kaç saniye vardır?", "86400", "3600", "1440", "36000", "24 × 60 × 60 = 86400."),
    Riddle("Yarısı 4 olan sayının iki katı kaçtır?", "16", "8", "4", "12", "Sayı 8'dir, iki katı 16."),
    Riddle("Hangi kelime her zaman YANLIŞ yazılır?", "Yanlış", "Doğru", "Hata", "Tersine", "\"Yanlış\" kelimesi hep yanlış diye yazılır."),
    Riddle("İki baba ve iki oğul balığa çıktı, 3 balık tuttular ve herkese birer balık düştü. Nasıl?", "Dede, baba ve torundular", "Biri balık yemedi", "Bir balık bölündü", "Biri gelmedi", "Baba hem baba hem oğuldur: toplam 3 kişi."),
    Riddle("Bir anne ve kızı, bir anne ve kızı, bir büyükanne ve torunu yürüyüşe çıktı. En az kaç kişiydiler?", "3", "6", "4", "5", "Büyükanne, anne ve kız: hepsi bu ilişkileri sağlar."),
    Riddle("Bakteriler her dakika ikiye bölünüyor; tek bakteriyle şişe 60 dakikada doluyor. İki bakteriyle kaç dakikada dolar?", "59", "30", "60", "58", "İki bakteriyle başlamak bir dakika önden gitmek demektir."),
    Riddle("6 elmanın 2/3'ünü yersen kaç elma kalır?", "2", "4", "3", "1", "6'nın 2/3'ü 4; geriye 2 kalır."),
    Riddle("Bir zarın karşılıklı yüzlerindeki sayıların toplamı kaçtır?", "7", "6", "8", "Değişir", "1-6, 2-5, 3-4: hep 7."),
    Riddle("Bir küpün kaç kenarı vardır?", "12", "6", "8", "24", "Küpün 6 yüzü, 8 köşesi, 12 kenarı vardır."),
    Riddle("Ali Veli'den uzun, Veli de Can'dan uzun. En kısa kim?", "Can", "Ali", "Veli", "Bilinemez", "Ali > Veli > Can."),
    Riddle("Ayşe Elif'ten büyük, Zeynep de Ayşe'den büyük. En küçük kim?", "Elif", "Ayşe", "Zeynep", "Bilinemez", "Zeynep > Ayşe > Elif."),
    Riddle("Bir yumurta 4 dakikada haşlanıyorsa 3 yumurta aynı tencerede kaç dakikada haşlanır?", "4", "12", "8", "6", "Hepsi aynı anda haşlanır."),
    Riddle("Hangi rakam 180° ters çevrilince değeri artar?", "6", "9", "8", "1", "6 ters çevrilince 9 olur."),
    Riddle("Hangi rakam 180° ters çevrilince yine kendisi olur?", "8", "6", "7", "4", "8 her iki yönde de aynı görünür."),
    Riddle("11 + 2 = 1 nerede doğrudur?", "Saatte", "Hiçbir yerde", "Takvimde", "İkilik sistemde", "Saat 11'den 2 saat sonra saat 1 olur."),
    Riddle("Hangi soruya hiçbir zaman dürüstçe \"evet\" diyemezsin?", "Uyuyor musun?", "Aç mısın?", "Hazır mısın?", "Burada mısın?", "Uyuyan kişi cevap veremez."),
    Riddle("Bir geminin yanında 5 basamaklı bir merdiven sarkıyor. Su her saat 1 basamak yükselirse 3 saat sonra kaç basamak suyun üstünde kalır?", "5", "2", "3", "0", "Gemi de suyla birlikte yükselir."),
    Riddle("Bir uçtan yakılınca 60 dakikada yanan ip iki ucundan birden yakılırsa kaç dakikada biter?", "30", "60", "15", "120", "İki alev aynı anda yanarak süreyi yarıya indirir."),
    Riddle("8 kişi bir odada herkes herkesle bir kez tokalaşırsa toplam kaç tokalaşma olur?", "28", "64", "56", "16", "8 × 7 / 2 = 28."),
    Riddle("6 takımlı bir ligde her takım diğerleriyle bir kez oynarsa toplam kaç maç oynanır?", "15", "30", "36", "12", "6 × 5 / 2 = 15."),
    Riddle("32 takımlı bir eleme turnuvasında şampiyon belli olana kadar kaç maç oynanır?", "31", "32", "16", "64", "Her maç bir takımı eler; 31 takım elenmelidir."),
    Riddle("1'den 10'a kadar olan sayıların toplamı kaçtır?", "55", "50", "45", "100", "10 × 11 / 2 = 55."),
    Riddle("Bir sayının yarısının yarısı 5 ise o sayı kaçtır?", "20", "10", "25", "15", "5 × 2 × 2 = 20."),
    Riddle("Bir koşuda 6. sıradasın. 2 kişiyi geçtin, sonra 1 kişi seni geçti. Kaçıncısın?", "5", "4", "6", "3", "6 − 2 + 1 = 5."),
    Riddle("Bir kuyruğun tam ortasındasın ve önünde 7 kişi var. Kuyrukta kaç kişi var?", "15", "14", "8", "16", "Önünde 7, arkanda 7, bir de sen: 15."),
    Riddle("Bir zeytin ağacında 3 kiraz vardı, 1'ini yedim. Kaç kiraz kaldı?", "Zeytin ağacında kiraz olmaz", "2", "1", "3", "Zeytin ağacında kiraz yetişmez."),
    Riddle("Ağaçta 10 kuş vardı; avcı birini vurdu. Ağaçta kaç kuş kaldı?", "Hiç", "9", "1", "10", "Silah sesine diğerleri uçup gider."),
    Riddle("Yuvarlak bir pastayı 8 eşit dilime ayırmak için merkezden geçen en az kaç düz kesim gerekir?", "4", "8", "3", "2", "Her merkezden geçen kesim 2 dilim sınırı oluşturur."),
    Riddle("Bir kitapta sayfa numaraları 1'den başlıyor. 9. sayfa ile 12. sayfa arasında (ikisi dahil) kaç sayfa var?", "4", "3", "2", "5", "9, 10, 11, 12."),
    Riddle("Bir merdivenin 1. basamağından 5. basamağına çıkmak 4 saniye sürüyor. 1. basamaktan 9. basamağa kaç saniyede çıkılır?", "8", "9", "7", "10", "1→5 arası 4 adım; 1→9 arası 8 adım."),
    Riddle("Bir saat her saat başında saat kaçsa o kadar çalar. Bir günde (24 saat) kaç kez çalar?", "156", "144", "78", "300", "12 saatte 1+2+...+12 = 78; 24 saatte 156."),
    Riddle("Bir kutuda 10 kırmızı ve 10 mavi bilye var. Bakmadan en az kaç bilye çekersen aynı renkten 2 tane garanti olur?", "3", "2", "11", "4", "İki farklı renk çekebilirsin; üçüncüsü eşleşir."),
    Riddle("Bir ailede 5 kardeş var; her erkek kardeşin kız kardeş sayısı erkek kardeş sayısına eşit. Kaç kız var?", "2", "3", "1", "4", "3 erkek, 2 kız: her erkeğin 2 erkek, 2 kız kardeşi olur."),
    Riddle("Kaç tane 2 dakikalık şarkı 1 saati doldurur?", "30", "60", "20", "120", "60 / 2 = 30."),
    Riddle("Bir şeyin yarısı ile çeyreğinin toplamı 30 ise tamamı kaçtır?", "40", "60", "45", "120", "3/4'ü 30 ise tamamı 40."),
    Riddle("Saat 6:00'da akrep ile yelkovan arasındaki açı kaç derecedir?", "180", "90", "360", "120", "6 aralık × 30° = 180°."),
    Riddle("Bir dikdörtgenin kaç köşegeni vardır?", "2", "4", "1", "8", "Karşılıklı köşeleri birleştiren 2 doğru."),
    Riddle("Bir üçgenin iç açılarının toplamı kaç derecedir?", "180", "360", "90", "270", "Her üçgende iç açılar toplamı 180°."),
    Riddle("Bir haftada kaç saat vardır?", "168", "120", "144", "196", "7 × 24 = 168."),
    Riddle("Bir odada 4 köşe var, her köşede bir kedi, her kedinin karşısında 3 kedi var. Odada kaç kedi var?", "4", "12", "16", "7", "Her kedi diğer 3 kediyi görür; toplam 4."),
    Riddle("Bir çiftlikte 10 tavuk ve 4 inek var. Toplam kaç ayak vardır?", "36", "28", "40", "56", "10 × 2 + 4 × 4 = 36."),
    Riddle("Bir sınıfın önünde 3, arkasında 3 sıra var ve sen ortadasın. Sınıfta kaç sıra var?", "7", "6", "9", "3", "3 + 1 + 3 = 7."),
    Riddle("Ahmet'in 3 kalemi var, Mehmet'in Ahmet'ten 2 fazla. İkisinin toplam kaç kalemi var?", "8", "5", "7", "10", "3 + 5 = 8."),
    Riddle("100'den kaç kez 5 çıkarabilirsin?", "Bir kez", "20 kez", "5 kez", "Hiç", "İlk çıkarmadan sonra artık 95'ten çıkarırsın."),
    Riddle("Bir trende 25 vagon var; tren 5 durakta durdu. Kaç vagonu var?", "25", "20", "30", "5", "Durmak vagon sayısını değiştirmez."),
    Riddle("Bir kâğıdı ikiye katlarsan 2 kat olur. 3 kez ikiye katlarsan kaç kat olur?", "8", "6", "3", "9", "2 × 2 × 2 = 8."),
    Riddle("Bir saat 3 dakikada bir 1 dakika geri kalıyorsa, 1 saat sonra kaç dakika geri kalmış olur?", "20", "3", "60", "15", "60 / 3 = 20 kez geri kalır."),
    Riddle("Annemin babasının tek kızının oğlu (soran bir erkekse) kimdir?", "Kendisi", "Dayısı", "Dedesi", "Kuzeni", "Annesinin babasının tek kızı annesidir; onun oğlu kendisi."),
    Riddle("4 kedi 4 fareyi 4 dakikada yakalıyor. 100 fareyi 100 dakikada yakalamak için kaç kedi gerekir?", "4", "100", "25", "1", "4 kedi her 4 dakikada 4 fare, 100 dakikada 100 fare yakalar."),
    Riddle("Bir sepette 6 yumurta var. 6 kişi birer yumurta alıyor ama sepette 1 yumurta kalıyor. Nasıl?", "Sonuncu sepetle birlikte aldı", "Biri almadı", "Yumurta kırıldı", "7 yumurta vardı", "Son kişi yumurtayı sepetin içinde aldı."),
    Riddle("Hangi cümle tersten okunduğunda da aynıdır?", "Ey Edip, Adana'da pide ye", "Ankara'da pide ye", "Pide ye Edip", "Adana'da kebap ye", "Harfleri sondan başa okuyunca aynı cümle çıkar.")
)

object ReverseGenerator : PuzzleGenerator<ReversePuzzle> {
    override val gameId = "reverse_thinking"

    private fun fmtTime(m: Int): String {
        val h = m / 60
        val mm = m % 60
        return "${if (h == 0) 12 else h}:${mm.toString().padStart(2, '0')}"
    }

    private fun riddle(rng: Random): ReversePuzzle {
        val r = RIDDLES.pick(rng)
        val opts = listOf(r.a, r.w1, r.w2, r.w3).shuffled(rng)
        return ReversePuzzle(0, r.q, opts, opts.indexOf(r.a), r.why, key = "R:" + r.q)
    }

    private fun mirrorClock(rng: Random, difficulty: Float): ReversePuzzle? {
        val stepMin = step(difficulty, 30, 15, 5, 5, 5)
        val shown = rng.nextInt(720 / stepMin) * stepMin
        if (shown == 0 || shown == 360) return null // simetrik saatler (12:00, 6:00) tuzak değil
        val real = (720 - shown) % 720
        val cands = linkedSetOf(real, shown, (real + 60) % 720, (real + 660) % 720, (720 - shown + 30) % 720)
        cands.remove(real)
        val wrong = cands.toList().shuffled(rng).take(3)
        if (wrong.size < 3) return null
        val opts = (wrong + real).shuffled(rng)
        return ReversePuzzle(
            1, "Bu saat AYNADA böyle görünüyor. Gerçek saat kaç?",
            opts.map { fmtTime(it) }, opts.indexOf(real),
            "Aynadaki saat ${fmtTime(shown)} gibi görünür; gerçek saat 12:00 − ${fmtTime(shown)} = ${fmtTime(real)}.",
            clockMinutes = shown, key = "C:$shown"
        )
    }

    private fun inverseOps(rng: Random, difficulty: Float): ReversePuzzle? {
        val tier = tierOf(difficulty)
        val steps = step(difficulty, 2, 2, 3, 3, 4) + if (rng.nextFloat() < 0.3f && tier >= 2) 1 else 0
        val x = rng.nextInt(2, if (tier >= 3) 30 else 15)
        var v = x
        val parts = ArrayList<String>()
        repeat(steps) {
            val op = rng.nextInt(if (tier == 0) 3 else 4)
            when (op) {
                0 -> { val a = rng.nextInt(2, 15); v += a; parts += "$a ekledim" }
                1 -> { val a = rng.nextInt(1, (v - 1).coerceAtLeast(2)); if (a >= v) return null; v -= a; parts += "$a çıkardım" }
                2 -> { val k = rng.nextInt(2, if (tier >= 2) 6 else 4); v *= k; parts += "$k ile çarptım" }
                else -> {
                    val divisors = (2..6).filter { v % it == 0 && v / it > 1 }
                    if (divisors.isEmpty()) { val a = rng.nextInt(2, 9); v += a; parts += "$a ekledim" }
                    else { val k = divisors.pick(rng); v /= k; parts += "$k'${suffixDative(k)} böldüm" }
                }
            }
        }
        if (v > 2000 || v < 1) return null
        val q = "Bir sayı tuttum: " + parts.joinToString(", ") + ". Sonuç $v. Tuttuğum sayı kaç?"
        val cands = linkedSetOf(x + 1, x - 1, x + 2, x * 2, v, x + 3).filter { it != x && it > 0 }.shuffled(rng).take(3)
        if (cands.size < 3) return null
        val opts = (cands + x).shuffled(rng)
        return ReversePuzzle(2, q, opts.map { "$it" }, opts.indexOf(x), "Sondan başa ters işlemleri uygula: cevap $x.", key = "O:$q")
    }

    /** Sayıdan sonra gelen yönelme eki (2'ye, 3'e, 4'e, 5'e, 6'ya). */
    private fun suffixDative(n: Int): String = when (n % 10) {
        2, 7 -> "ye"
        3, 4, 5, 8 -> "e"
        6 -> "ya"
        9 -> "a"
        else -> "a"
    }

    private fun reversedWord(rng: Random, difficulty: Float): ReversePuzzle? {
        val tier = tierOf(difficulty)
        val (w, _) = WordData.ALL.filter { it.first.length in (if (tier == 0) 4..5 else 5..8) }.pick(rng)
        val rev = w.reversed()
        if (rev == w) return null
        // Çeldiriciler: aynı harflerle başka sıralamalar
        val cands = linkedSetOf<String>()
        var g = 0
        while (cands.size < 3 && g++ < 50) {
            val sh = w.toList().shuffled(rng).joinToString("")
            if (sh != w) cands += sh
        }
        if (cands.size < 3) return null
        val opts = (cands.toList() + w).shuffled(rng)
        return ReversePuzzle(3, "Tersten yazılmış kelimeyi düz oku:  $rev", opts, opts.indexOf(w), "$rev tersten okununca $w olur.", key = "W:$w")
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): ReversePuzzle? {
        val tier = tierOf(difficulty)
        val riddleShare = step(difficulty, 0.55f, 0.5f, 0.45f, 0.4f, 0.35f)
        val roll = rng.nextFloat()
        return when {
            roll < riddleShare -> riddle(rng)
            roll < riddleShare + 0.18f -> mirrorClock(rng, difficulty)
            tier <= 1 && roll < riddleShare + 0.3f -> reversedWord(rng, difficulty)
            else -> inverseOps(rng, difficulty)
        }
    }

    override fun fingerprint(puzzle: ReversePuzzle) = puzzle.key
}
