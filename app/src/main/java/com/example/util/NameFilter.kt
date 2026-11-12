package com.example.util

/** Ligde herkese görünen kullanıcı adları için basit küfür / uygunsuz kelime süzgeci. */
object NameFilter {
    private val BAD = listOf(
        "amcık", "amk", "aq", "sik", "sık", "göt", "orospu", "oç", "piç", "yarrak", "yarak", "taşak", "pezevenk", "kahpe",
        "fahişe", "sürtük", "ibne", "puşt", "gavat", "kaltak", "salak", "aptal", "gerizekalı", "mal ", "dangalak", "şerefsiz",
        "ananı", "anan", "bacını", "sex", "seks", "porno", "penis", "vajina", "meme", "zenci", "nazi", "hitler", "terör",
        "fuck", "shit", "bitch", "dick", "pussy", "cunt", "nigg", "whore", "slut", "asshole"
    )

    private fun norm(s: String): String = s.lowercase(java.util.Locale.forLanguageTag("tr"))
        .replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a').replace('5', 's').replace('@', 'a').replace('$', 's')
        .replace(Regex("[._\\-*]"), "")

    fun isBad(name: String): Boolean {
        val n = " " + norm(name) + " "
        val compact = n.replace(" ", "")
        return BAD.any { b ->
            val bb = b.trim()
            if (bb.length <= 3) Regex("(^|[^a-zçğıöşü])${Regex.escape(bb)}([^a-zçğıöşü]|$)").containsMatchIn(n)
            else n.contains(bb) || compact.contains(bb)
        }
    }

    /** Uygunsuzsa varsayılan ada döner. */
    fun safe(name: String, fallback: String = "Zihin Gezgini"): String = if (isBad(name)) fallback else name
}
