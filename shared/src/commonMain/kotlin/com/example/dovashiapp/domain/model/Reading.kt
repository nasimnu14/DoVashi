package com.example.dovashiapp.domain.model

/**
 * The Reading to store with a translation into [targetLanguageCode]: the trimmed [reading] when the target
 * Language requires one and the Reading is English-readable (every letter Latin script, diacritics such as pinyin
 * tone marks included); otherwise null. Decided by Language metadata, never by comparing codes.
 */
fun readingFor(targetLanguageCode: String?, reading: String?, languageByCode: (String) -> Language?): String? {
    val target = targetLanguageCode?.let(languageByCode) ?: return null
    if (!target.requiresReading) return null
    val text = visibleTextOrNull(reading) ?: return null
    var hasLetter = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c.isHighSurrogate() && c in '\uD83C'..'\uD83E') {
            i += 2 // U+1F000–1FBFF: emoji and symbols, not letters
            continue
        }
        // Any other supplementary character (e.g. Han extension B) is a letter of another script.
        if (c.isSurrogate()) return null
        if (c.isLetter()) {
            if (!Script.isLatinLetter(c)) return null
            hasLetter = true
        }
        i++
    }
    return text.takeIf { hasLetter }
}
