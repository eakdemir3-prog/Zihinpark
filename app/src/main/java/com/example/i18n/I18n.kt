package com.example.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.util.Locale

/**
 * Hafif çeviri katmanı. Uygulama metinleri kodda Türkçe yazılıdır; İngilizce seçiliyken
 * ekrana basılan her metin [t] ile assets/i18n_en.json sözlüğünden çevrilir.
 * - Birebir eşleşme
 * - Şablonlar: "Seviye {0} · ⭐ {1}" -> "Level {0} · ⭐ {1}" (yer tutucular da çevrilir)
 * - Büyük harfli metinler, " · " / satır ile bölünmüş parçalar ve başta emoji olan metinler
 */
object I18n {
    private const val PREFS = "zp_lang"
    /** "auto", "tr" ya da "en" */
    var pref by mutableStateOf("auto"); private set
    var lang by mutableStateOf("tr"); private set
    val isEn: Boolean get() = lang == "en"

    private var exact: Map<String, String> = emptyMap()
    private var upper: Map<String, String> = emptyMap()
    private var tpls: List<Pair<Regex, String>> = emptyList()
    private var loaded = false
    private val cache = object : LinkedHashMap<String, String>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 3000
    }
    private val TR = Locale.forLanguageTag("tr")

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("lang", "auto") ?: "auto"
        apply(context, p)
    }

    fun setPreference(context: Context, p: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("lang", p).apply()
        apply(context, p)
    }

    private fun apply(context: Context, p: String) {
        pref = p
        val l = when (p) {
            "tr" -> "tr"
            "en" -> "en"
            else -> if (Locale.getDefault().language == "tr") "tr" else "en"
        }
        if (l == "en") load(context.applicationContext)
        synchronized(cache) { cache.clear() }
        lang = l
    }

    private fun load(context: Context) {
        if (loaded) return
        runCatching {
            val o = JSONObject(context.assets.open("i18n_en.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            val ex = HashMap<String, String>()
            val e = o.getJSONObject("exact")
            for (k in e.keys()) ex[k] = e.getString(k)
            exact = ex
            upper = ex.entries.associate { it.key.uppercase(TR) to it.value.uppercase(Locale.ENGLISH) }
            val list = ArrayList<Pair<Regex, String>>()
            val t = o.getJSONArray("tpl")
            for (i in 0 until t.length()) {
                val a = t.getJSONArray(i)
                list += Regex(toPattern(a.getString(0)), RegexOption.DOT_MATCHES_ALL) to a.getString(1)
            }
            tpls = list
            loaded = true
        }
    }

    private fun toPattern(key: String): String {
        val sb = StringBuilder("^")
        var i = 0
        val ph = Regex("\\{\\d+\\}")
        var last = 0
        for (m in ph.findAll(key)) {
            sb.append(Regex.escape(key.substring(last, m.range.first)))
            sb.append("(.+?)")
            last = m.range.last + 1
            i++
        }
        sb.append(Regex.escape(key.substring(last))).append("$")
        return sb.toString()
    }

    /** Metni seçili dile çevirir. Türkçe seçiliyse aynen döner. */
    fun t(s: String): String {
        if (lang != "en" || s.isEmpty() || s[0] == RAW) return s
        synchronized(cache) { cache[s]?.let { return it } }
        val r = translate(s, 0)
        synchronized(cache) { cache[s] = r }
        return r
    }

    private fun translate(s: String, depth: Int): String {
        exact[s]?.let { return it }
        if (depth > 3) return s
        if (s.any { it.isLetter() } && s == s.uppercase(TR)) upper[s]?.let { return it }
        for ((re, out) in tpls) {
            val m = re.matchEntire(s) ?: continue
            var o = out
            m.groupValues.drop(1).forEachIndexed { i, g -> o = o.replace("{$i}", inner(g, depth)) }
            return o
        }
        for (sep in SEPARATORS) {
            if (s.contains(sep)) {
                val parts = s.split(sep)
                val tp = parts.map { translate(it, depth + 1) }
                if (tp != parts) return tp.joinToString(sep)
            }
        }
        // Baştaki emoji / sembol ya da sondaki noktalama
        val first = s.indexOfFirst { it.isLetterOrDigit() }
        if (first > 0) {
            val core = s.substring(first)
            val tc = translate(core, depth + 1)
            if (tc != core) return s.substring(0, first) + tc
        }
        val lastIdx = s.indexOfLast { it.isLetterOrDigit() }
        if (lastIdx in 0 until s.length - 1) {
            val core = s.substring(0, lastIdx + 1)
            val tc = translate(core, depth + 1)
            if (tc != core) return tc + s.substring(lastIdx + 1)
        }
        val trimmed = s.trim()
        if (trimmed != s && trimmed.isNotEmpty()) {
            val tc = translate(trimmed, depth + 1)
            if (tc != trimmed) return s.replace(trimmed, tc)
        }
        return s
    }

    /** Çevrilmemesi gereken metinler (kullanıcı adları) için görünmez işaret. */
    const val RAW = '\u2060'
    fun raw(s: String): String = if (s == "Zihin Gezgini" || s.isEmpty()) s else RAW + s

    private fun inner(g: String, depth: Int): String = if (g.isNotEmpty() && g[0] == RAW) g else if (g.any { it.isLetter() }) translate(g, depth + 1) else g

    private val SEPARATORS = listOf("\n", " · ", " — ", ". ", ": ", ", ")
}

/** Kısa yol: tr("metin") */
fun tr(s: String): String = I18n.t(s)
