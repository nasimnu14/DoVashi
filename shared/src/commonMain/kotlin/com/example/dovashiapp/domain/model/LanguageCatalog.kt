package com.example.dovashiapp.domain.model

object LanguageCatalog {
    val all: List<Language> = listOf(
        Language(code = "en", name = "English", nativeName = "English", scripts = setOf(Script.LATIN)),
        Language(code = "zh", name = "Mandarin Chinese", nativeName = "普通话", readingSystem = "Hanyu Pinyin with tone marks", scripts = setOf(Script.HAN)),
    )

    fun byCode(code: String): Language? = all.find { it.code == code }
}
