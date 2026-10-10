package com.example.dovashiapp.domain.model

/**
 * The Reading to store with a translation into [targetLanguageCode]: the trimmed [reading] when the target
 * Language requires one, otherwise null. Decided by Language metadata, never by comparing codes.
 */
fun readingFor(targetLanguageCode: String?, reading: String?, languageByCode: (String) -> Language?): String? {
    val target = targetLanguageCode?.let(languageByCode) ?: return null
    if (!target.requiresReading) return null
    return visibleTextOrNull(reading)
}
