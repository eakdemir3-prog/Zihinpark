package com.example.game.puzzles

/** Türkçe ek yardımcıları (ünlü uyumu ve ünsüz benzeşmesi). */
object Turkish {
    private const val BACK = "aıou"
    private const val FRONT = "eiöü"
    private const val VOICELESS = "fstkçşhp"

    private fun lastVowel(word: String): Char? =
        word.lowercase(trLocale).lastOrNull { it in BACK || it in FRONT || it == 'â' || it == 'û' }

    private fun isBack(word: String): Boolean {
        val v = lastVowel(word) ?: return true
        return v in BACK || v == 'â' || v == 'û'
    }

    private fun endsVowel(word: String): Boolean {
        val c = word.lowercase(trLocale).lastOrNull() ?: return false
        return c in BACK || c in FRONT
    }

    private fun endsVoiceless(word: String): Boolean =
        word.lowercase(trLocale).lastOrNull()?.let { it in VOICELESS } == true

    val trLocale: java.util.Locale = java.util.Locale.forLanguageTag("tr-TR")

    /** Bulunma eki: "mutfak" -> "mutfakta", "bahçe" -> "bahçede". */
    fun locative(word: String): String {
        val d = if (endsVoiceless(word)) "t" else "d"
        return word + d + (if (isBack(word)) "a" else "e")
    }

    /** "mutfaktaydı", "bahçedeydi" */
    fun locativePast(word: String): String {
        val loc = locative(word)
        return loc + "y" + (if (isBack(word)) "dı" else "di")
    }

    /** Özel isme ayrılma eki: "Can'dan", "Ahmet'ten", "Ayşe'den". */
    fun ablativeName(name: String): String {
        val d = if (endsVoiceless(name)) "t" else "d"
        return "$name'" + d + (if (isBack(name)) "an" else "en")
    }

    /** Özel isme yönelme eki: "Can'a", "Ayşe'ye", "Ahmet'e". */
    fun dativeName(name: String): String {
        val y = if (endsVowel(name)) "y" else ""
        return "$name'" + y + (if (isBack(name)) "a" else "e")
    }

    /** Özel isme belirtme eki: "Can'ı", "Ayşe'yi", "Ahmet'i", "Onur'u". */
    fun accusativeName(name: String): String {
        val y = if (endsVowel(name)) "y" else ""
        val v = when (lastVowel(name)) {
            'a', 'ı' -> "ı"
            'e', 'i' -> "i"
            'o', 'u' -> "u"
            'ö', 'ü' -> "ü"
            else -> "ı"
        }
        return "$name'$y$v"
    }

    fun upper(s: String): String = s.uppercase(trLocale)
    fun lower(s: String): String = s.lowercase(trLocale)
    fun capitalize(s: String): String = s.replaceFirstChar { it.titlecase(trLocale) }
}
