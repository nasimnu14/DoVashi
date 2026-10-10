package com.example.dovashiapp.domain.model

object LanguageCatalog {
    val all: List<Language> = listOf(
        Language(code = "en", name = "English", nativeName = "English", requiresReading = false, scripts = setOf(Script.LATIN)),
        Language(code = "zh", name = "Mandarin Chinese", nativeName = "普通话", requiresReading = true, scripts = setOf(Script.HAN)),
    )

    fun byCode(code: String): Language? = all.find { it.code == code }
}
