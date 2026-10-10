package com.example.dovashiapp.domain.model

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    /**
     * The Reading system for translations into this Language (e.g. "Hanyu Pinyin with tone marks"), or null when
     * none is needed. Its presence is what makes a Language need a Reading.
     */
    val readingSystem: String? = null,
    /** Scripts this Language is written in; empty means unknown (no script-based detection). */
    val scripts: Set<Script> = emptySet(),
) {
    init {
        require(readingSystem == null || readingSystem.isNotBlank()) { "A Reading system needs a name" }
    }

    /** Whether translations into this Language carry a Reading. */
    val requiresReading: Boolean get() = readingSystem != null
}
