package com.example.dovashiapp.domain.model

/** A writing system, used to tell a Conversation's two Languages apart by how a transcript is written. */
enum class Script(private val ranges: List<CharRange>, val countsCharacters: Boolean = false) {
    LATIN(listOf('A'..'Z', 'a'..'z', 'À'..'ɏ', 'Ḁ'..'ỿ')),
    // Written without spaces; one character is about a syllable or a word.
    HAN(listOf('㐀'..'䶿', '一'..'鿿', '豈'..'﫿'), countsCharacters = true),
    KANA(listOf('぀'..'ゟ', '゠'..'ヿ', 'ㇰ'..'ㇿ'), countsCharacters = true),
    HANGUL(listOf('ᄀ'..'ᇿ', '㄰'..'㆏', '가'..'힯')),
    BENGALI(listOf('ঀ'..'৿')),
    ARABIC(listOf('؀'..'ۿ', 'ݐ'..'ݿ')),
    DEVANAGARI(listOf('ऀ'..'ॿ')),
    CYRILLIC(listOf('Ѐ'..'ӿ'));

    companion object {
        /** The script of a letter, or null for anything else (digits, punctuation, spaces, unknown scripts). */
        fun of(c: Char): Script? =
            if (!c.isLetter()) null else entries.firstOrNull { script -> script.ranges.any { c in it } }

        /**
         * A letter English readers can sound out: [LATIN] (diacritics included, as in pinyin), IPA and phonetic
         * letters, the later Latin extensions, or full-width Latin. Broader than [LATIN], which is tuned for detection.
         */
        fun isLatinLetter(c: Char): Boolean =
            of(c) == LATIN || c in '\u0250'..'\u02FF' || c in '\u1D00'..'\u1DBF' || c in '\u2C60'..'\u2C7F' ||
                c in '\uA720'..'\uA7FF' || c in '\uAB30'..'\uAB6F' || c in '\uFF21'..'\uFF3A' || c in '\uFF41'..'\uFF5A'
    }
}

/**
 * How much of [text] each script carries, in rough word units: a run of letters of a space-separated script counts
 * once, while each character of a [Script.countsCharacters] script counts on its own. So "我用iPhone" is 2 Han to
 * 1 Latin, not 2 to 6.
 */
fun scriptUnits(text: String): Map<Script, Int> {
    val units = mutableMapOf<Script, Int>()
    var previous: Script? = null
    for (c in text) {
        val script = Script.of(c)
        if (script == null) {
            // Combining marks (Devanagari/Bengali vowel signs, accents) and in-word apostrophes or hyphens
            // continue the current word; anything else ends it.
            if (!c.isWordContinuation()) previous = null
            continue
        }
        if (script.countsCharacters || script != previous) units[script] = (units[script] ?: 0) + 1
        previous = script
    }
    return units
}

private fun Char.isWordContinuation(): Boolean =
    category == CharCategory.NON_SPACING_MARK || category == CharCategory.COMBINING_SPACING_MARK ||
        category == CharCategory.ENCLOSING_MARK || this == '\'' || this == '’' || this == '-' || this == '‐'
