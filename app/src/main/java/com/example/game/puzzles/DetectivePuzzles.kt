package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.pick
import com.example.game.engine.pickN
import com.example.game.engine.step
import kotlin.random.Random

/**
 * Mantık Dedektifi: şüphelilerin her biri farklı bir yerde (ve üst seviyelerde farklı renkte)
 * bulunuyor. Doğru ipuçları birlikte yalnızca tek bir suçluyu mümkün kılar. Üretici bunu
 * tüm olası atamaları deneyerek doğrular.
 */
data class DetectiveSuspect(val name: String, val role: String, val avatar: String)

data class DetectivePuzzle(
    val title: String,
    val story: String,
    val fact: String,
    val suspects: List<DetectiveSuspect>,
    val clues: List<String>,
    val culprit: Int,
    val explanation: String,
    /** Hata yapınca elenebilecek masum şüpheliler (ipucu desteği için). */
    val innocents: List<Int>
)

private data class Setting(val place: String, val crime: String, val places: List<String>)

private val SETTINGS = listOf(
    Setting("Şehir Müzesi", "Sergideki tarihi saat yerinden kayboldu.", listOf("giriş holü", "sergi salonu", "arşiv", "kafeterya", "bahçe", "depo")),
    Setting("Grand Otel", "Kasadaki elmas kolye çalındı.", listOf("lobi", "restoran", "havuz", "spor salonu", "teras", "mutfak")),
    Setting("Karanfil Malikânesi", "Kütüphanedeki eski vasiyet ortadan kayboldu.", listOf("kütüphane", "salon", "kiler", "çatı katı", "sera", "bodrum")),
    Setting("Gece Treni", "Birinci sınıf kompartımandaki çanta açıldı.", listOf("yemekli vagon", "kompartıman", "koridor", "bagaj vagonu", "lokomotif", "kafe vagonu")),
    Setting("Teknoloji Ofisi", "Prototip telefonun çizimleri sızdırıldı.", listOf("toplantı odası", "laboratuvar", "mutfak", "sunucu odası", "resepsiyon", "otopark")),
    Setting("Lise Kampüsü", "Final sınavının soruları dolaptan alındı.", listOf("öğretmenler odası", "kantin", "spor salonu", "kütüphane", "laboratuvar", "bahçe")),
    Setting("Kruvaziyer Gemi", "Kaptanın pusulası kamarasından kayboldu.", listOf("güverte", "kamara", "restoran", "makine dairesi", "havuz", "tiyatro")),
    Setting("Sanat Galerisi", "Değerli tablo sahtesiyle değiştirildi.", listOf("ana salon", "atölye", "depo", "ofis", "kafe", "teras")),
    Setting("Kayak Oteli", "Şampiyonluk kupası vitrininden kayboldu.", listOf("şömine salonu", "kayak odası", "restoran", "sauna", "lobi", "teleferik")),
    Setting("Film Stüdyosu", "Senaryonun son sahnesi internete sızdırıldı.", listOf("set", "makyaj odası", "kurgu odası", "kostüm deposu", "kantin", "ses kabini")),
    Setting("Botanik Bahçesi", "Nadir orkide serasından çalındı.", listOf("sera", "göl kenarı", "kafe", "laboratuvar", "fidanlık", "giriş")),
    Setting("Uzay Üssü", "Görev kayıtları silindi.", listOf("kontrol odası", "laboratuvar", "hangar", "yemekhane", "gözlem kulesi", "jeneratör odası"))
)

private val SETTINGS_EN = listOf(
    Setting("City Museum", "The antique clock vanished from the exhibit.", listOf("entrance hall", "gallery", "archive", "cafeteria", "garden", "storeroom")),
    Setting("Grand Hotel", "The diamond necklace was stolen from the safe.", listOf("lobby", "restaurant", "pool", "gym", "terrace", "kitchen")),
    Setting("Carnation Manor", "The old will disappeared from the library.", listOf("library", "living room", "pantry", "attic", "greenhouse", "basement")),
    Setting("Night Train", "A bag in the first-class compartment was opened.", listOf("dining car", "compartment", "corridor", "baggage car", "engine", "café car")),
    Setting("Tech Office", "The prototype phone's drawings were leaked.", listOf("meeting room", "lab", "kitchen", "server room", "reception", "parking lot")),
    Setting("High School Campus", "The final exam questions were taken from a locker.", listOf("staff room", "canteen", "gym", "library", "lab", "schoolyard")),
    Setting("Cruise Ship", "The captain's compass vanished from his cabin.", listOf("deck", "cabin", "restaurant", "engine room", "pool", "theater")),
    Setting("Art Gallery", "A valuable painting was swapped for a fake.", listOf("main hall", "studio", "storeroom", "office", "café", "terrace")),
    Setting("Ski Lodge", "The championship trophy vanished from its case.", listOf("fireplace lounge", "ski room", "restaurant", "sauna", "lobby", "cable car")),
    Setting("Film Studio", "The script's final scene was leaked online.", listOf("set", "makeup room", "editing room", "costume store", "canteen", "sound booth")),
    Setting("Botanical Garden", "A rare orchid was stolen from the greenhouse.", listOf("greenhouse", "lakeside", "café", "lab", "nursery", "entrance")),
    Setting("Space Station", "The mission logs were deleted.", listOf("control room", "lab", "hangar", "mess hall", "observation tower", "generator room"))
)

private val NAMES_EN = listOf(
    "Adam", "Amy", "Ben", "Chloe", "Daniel", "Ella", "Finn", "Grace", "Henry", "Isla", "Jack", "Lily",
    "Leo", "Mia", "Noah", "Olivia", "Oscar", "Ruby", "Sam", "Sophie", "Tom", "Zoe", "Max", "Emma",
    "Luke", "Nora", "Ryan", "Hannah", "Owen", "Lucy"
)

private val ROLES_EN = listOf(
    "Security Guard" to "👮", "Guide" to "🧑‍🏫", "Cleaner" to "🧹", "Cook" to "🧑‍🍳",
    "Accountant" to "🧑‍💼", "Technician" to "🧑‍🔧", "Photographer" to "📸", "Doctor" to "🧑‍⚕️",
    "Gardener" to "🧑‍🌾", "Artist" to "🧑‍🎨", "Manager" to "🤵", "Intern" to "🧑‍🎓", "Pilot" to "🧑‍✈️",
    "Journalist" to "🗞️", "Engineer" to "🧑‍💻"
)

private val COLORS_EN = listOf("red", "blue", "green", "yellow", "purple", "black", "white", "orange")

private val en get() = com.example.i18n.I18n.isEn

private val NAMES = listOf(
    "Ahmet", "Ayşe", "Mehmet", "Zeynep", "Can", "Elif", "Burak", "Selin", "Emre", "Deniz", "Kerem", "Ece",
    "Murat", "Derya", "Ozan", "Pınar", "Tolga", "İrem", "Serkan", "Gizem", "Barış", "Nazlı", "Onur", "Ceren",
    "Kaan", "Melis", "Arda", "Yasemin", "Cem", "Buse"
)

private val ROLES = listOf(
    "Güvenlik Görevlisi" to "👮", "Rehber" to "🧑‍🏫", "Temizlik Görevlisi" to "🧹", "Aşçı" to "🧑‍🍳",
    "Muhasebeci" to "🧑‍💼", "Teknisyen" to "🧑‍🔧", "Fotoğrafçı" to "📸", "Doktor" to "🧑‍⚕️",
    "Bahçıvan" to "🧑‍🌾", "Sanatçı" to "🧑‍🎨", "Müdür" to "🤵", "Stajyer" to "🧑‍🎓", "Pilot" to "🧑‍✈️",
    "Gazeteci" to "🗞️", "Mühendis" to "🧑‍💻"
)

private val COLORS = listOf("kırmızı", "mavi", "yeşil", "sarı", "mor", "siyah", "beyaz", "turuncu")

/** Bir aday atama: her şüpheli için yer ve renk indeksi. */
private class Assignment(val place: IntArray, val color: IntArray?)

private sealed class Clue {
    abstract fun holds(a: Assignment): Boolean
    abstract fun text(names: List<String>, places: List<String>, colors: List<String>): String

    class PlaceIs(val s: Int, val p: Int) : Clue() {
        override fun holds(a: Assignment) = a.place[s] == p
        override fun text(names: List<String>, places: List<String>, colors: List<String>) =
            if (en) "${names[s]} was in the ${places[p]}." else "${names[s]} ${Turkish.locativePast(places[p])}."
    }

    class PlaceNot(val s: Int, val p: Int) : Clue() {
        override fun holds(a: Assignment) = a.place[s] != p
        override fun text(names: List<String>, places: List<String>, colors: List<String>) =
            if (en) "${names[s]} was not in the ${places[p]}." else "${names[s]} ${Turkish.locative(places[p])} değildi."
    }

    class ColorIs(val s: Int, val c: Int) : Clue() {
        override fun holds(a: Assignment) = a.color!![s] == c
        override fun text(names: List<String>, places: List<String>, colors: List<String>) =
            if (en) "${names[s]} was wearing ${colors[c]}." else "${names[s]} ${colors[c]} giyiyordu."
    }

    class ColorNot(val s: Int, val c: Int) : Clue() {
        override fun holds(a: Assignment) = a.color!![s] != c
        override fun text(names: List<String>, places: List<String>, colors: List<String>) =
            if (en) "${names[s]} was not wearing ${colors[c]}." else "${names[s]} ${colors[c]} giymiyordu."
    }

    class ColorAtPlace(val c: Int, val p: Int, val positive: Boolean) : Clue() {
        override fun holds(a: Assignment): Boolean {
            val who = a.color!!.indexOf(c)
            return if (who < 0) !positive else (a.place[who] == p) == positive
        }

        override fun text(names: List<String>, places: List<String>, colors: List<String>) =
            if (en) (if (positive) "The person in ${colors[c]} was in the ${places[p]}." else "The person in ${colors[c]} was not in the ${places[p]}.")
            else if (positive) "${Turkish.capitalize(colors[c])} giyen kişi ${Turkish.locativePast(places[p])}."
            else "${Turkish.capitalize(colors[c])} giyen kişi ${Turkish.locative(places[p])} değildi."
    }

    class EitherAt(val s1: Int, val s2: Int, val p: Int) : Clue() {
        override fun holds(a: Assignment) = a.place[s1] == p || a.place[s2] == p
        override fun text(names: List<String>, places: List<String>, colors: List<String>) =
            if (en) "Either ${names[s1]} or ${names[s2]} was in the ${places[p]}." else "${names[s1]} ile ${Turkish.ablativeName(names[s2])} biri ${Turkish.locativePast(places[p])}."
    }

    class NeitherAt(val s1: Int, val s2: Int, val p: Int) : Clue() {
        override fun holds(a: Assignment) = a.place[s1] != p && a.place[s2] != p
        override fun text(names: List<String>, places: List<String>, colors: List<String>) =
            if (en) "Neither ${names[s1]} nor ${names[s2]} was in the ${places[p]}." else "Ne ${names[s1]} ne de ${names[s2]} ${Turkish.locativePast(places[p])}."
    }
}

private fun permutations(n: Int): List<IntArray> {
    val out = ArrayList<IntArray>()
    fun rec(cur: IntArray, used: BooleanArray, i: Int) {
        if (i == n) { out += cur.copyOf(); return }
        for (v in 0 until n) if (!used[v]) {
            used[v] = true; cur[i] = v; rec(cur, used, i + 1); used[v] = false
        }
    }
    rec(IntArray(n), BooleanArray(n), 0)
    return out
}

object DetectiveGenerator : PuzzleGenerator<DetectivePuzzle> {
    override val gameId = "logic_detective"
    override val independent = true

    private val permCache = HashMap<Int, List<IntArray>>()
    private fun perms(n: Int) = permCache.getOrPut(n) { permutations(n) }

    override fun generate(index: Int, difficulty: Float, rng: Random): DetectivePuzzle? {
        val n = step(difficulty, 3, 3, 4, 4, 5)
        val useColor = difficulty >= 0.2f
        val byColor = useColor && rng.nextFloat() < 0.4f // suçlu rengiyle mi tarif ediliyor
        val minimize = difficulty >= 0.15f

        val setting = (if (en) SETTINGS_EN else SETTINGS).pick(rng)
        val places = setting.places.pickN(n, rng)
        val colors = (if (en) COLORS_EN else COLORS).pickN(n, rng)
        val names = (if (en) NAMES_EN else NAMES).pickN(n, rng)
        val roles = (if (en) ROLES_EN else ROLES).pickN(n, rng)

        val solPlace = perms(n).pick(rng)
        val solColor = if (useColor) perms(n).pick(rng) else null
        val sol = Assignment(solPlace, solColor)

        val culprit = rng.nextInt(n)
        val targetPlace = solPlace[culprit]
        val targetColor = solColor?.get(culprit)

        // Olası tüm atamalar
        val all = ArrayList<Assignment>()
        for (pp in perms(n)) {
            if (useColor) for (cp in perms(n)) all += Assignment(pp, cp) else all += Assignment(pp, null)
        }

        fun culpritOf(a: Assignment): Int =
            if (byColor) a.color!!.indexOf(targetColor!!) else a.place.indexOf(targetPlace)

        // Doğru ipucu havuzu (suçluyu doğrudan söyleyenler hariç)
        val pool = ArrayList<Clue>()
        for (s in 0 until n) for (p in 0 until n) {
            val c1 = Clue.PlaceIs(s, p)
            val c2 = Clue.PlaceNot(s, p)
            if (c1.holds(sol) && !(s == culprit && !byColor && p == targetPlace)) pool += c1
            if (c2.holds(sol)) pool += c2
        }
        if (useColor) {
            for (s in 0 until n) for (c in 0 until n) {
                val c1 = Clue.ColorIs(s, c)
                val c2 = Clue.ColorNot(s, c)
                if (c1.holds(sol) && !(s == culprit && byColor && c == targetColor)) pool += c1
                if (c2.holds(sol)) pool += c2
            }
            for (c in 0 until n) for (p in 0 until n) {
                val pos = Clue.ColorAtPlace(c, p, true)
                val neg = Clue.ColorAtPlace(c, p, false)
                if (pos.holds(sol)) pool += pos
                if (neg.holds(sol)) pool += neg
            }
        }
        for (s1 in 0 until n) for (s2 in s1 + 1 until n) for (p in 0 until n) {
            val e = Clue.EitherAt(s1, s2, p)
            val ne = Clue.NeitherAt(s1, s2, p)
            if (e.holds(sol) && solPlace[s1] != solPlace[s2]) pool += e
            if (ne.holds(sol)) pool += ne
        }
        // Zorlukla doğrudan "X şuradaydı" ipuçlarının oranını azalt
        val directWeight = if (difficulty < 0.3f) 1.0f else 0.35f
        val ordered = pool.shuffled(rng).map { c ->
            val direct = c is Clue.PlaceIs || c is Clue.ColorIs
            c to (if (direct) rng.nextFloat() / directWeight else rng.nextFloat())
        }.sortedBy { it.second }.map { it.first }

        val chosen = ArrayList<Clue>()
        var candidates: List<Assignment> = all
        for (c in ordered) {
            val filtered = candidates.filter { c.holds(it) }
            if (filtered.size == candidates.size) continue // bilgi vermeyen ipucu
            chosen += c
            candidates = filtered
            if (candidates.map(::culpritOf).toSet().size == 1) break
            if (chosen.size > 14) return null
        }
        if (candidates.map(::culpritOf).toSet() != setOf(culprit)) return null

        // Gereksiz ipuçlarını at (zorlaştırma)
        if (minimize) {
            var i = 0
            while (i < chosen.size && chosen.size > 3) {
                val trial = chosen.filterIndexed { j, _ -> j != i }
                val left = all.filter { a -> trial.all { it.holds(a) } }
                if (left.map(::culpritOf).toSet().size == 1) chosen.removeAt(i) else i++
            }
        }
        if (chosen.size < 2) return null

        val clueTexts = chosen.shuffled(rng).map { it.text(names, places, colors) }
        val fact = if (en) (if (byColor) "Witnesses say the culprit was wearing ${colors[targetColor!!]}."
            else "At the time of the crime, the culprit was in the ${places[targetPlace]}.")
        else if (byColor) "Tanıklara göre suçlu ${colors[targetColor!!]} giyiyordu."
        else "Olay anında suçlu ${Turkish.locativePast(places[targetPlace])}."
        val story = if (en) "${setting.place}: ${setting.crime} ${n} people were in the building, each in a different place" +
            (if (useColor) " and each wearing a different color." else ".")
        else "${setting.place}: ${setting.crime} O sırada ${n} kişi binadaydı; her biri farklı bir yerdeydi" +
            (if (useColor) " ve herkes farklı renkte giyinmişti." else ".")
        val explanation = if (en) buildString {
            append("${names[culprit]}: in the ${places[solPlace[culprit]]}")
            if (solColor != null) append(", wearing ${colors[solColor[culprit]]}")
            append(".")
        } else buildString {
            append("${names[culprit]}: ${Turkish.locative(places[solPlace[culprit]])}")
            if (solColor != null) append(", ${colors[solColor[culprit]]} giyiyordu")
            append(".")
        }
        val suspects = (0 until n).map { DetectiveSuspect(names[it], roles[it].first, roles[it].second) }
        return DetectivePuzzle(
            title = setting.place,
            story = story,
            fact = fact,
            suspects = suspects,
            clues = clueTexts,
            culprit = culprit,
            explanation = explanation,
            innocents = (0 until n).filter { it != culprit }
        )
    }

    override fun fingerprint(puzzle: DetectivePuzzle): String =
        puzzle.title + "|" + puzzle.suspects.joinToString(",") { it.name } + "|" + puzzle.clues.sorted().joinToString("|")
}
