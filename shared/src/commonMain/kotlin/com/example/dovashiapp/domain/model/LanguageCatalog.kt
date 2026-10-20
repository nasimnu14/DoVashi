package com.example.dovashiapp.domain.model

object LanguageCatalog {
    val all: List<Language> = listOf(
        Language(code = "en", name = "English", nativeName = "English", requiresReading = false),
        Language(code = "zh", name = "Mandarin Chinese", nativeName = "普通话", requiresReading = true),
    )

    fun byCode(code: String): Language? = all.find { it.code == code }
}
