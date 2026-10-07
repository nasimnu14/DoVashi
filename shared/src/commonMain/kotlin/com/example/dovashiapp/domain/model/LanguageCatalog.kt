package com.example.dovashiapp.domain.model

object LanguageCatalog {
    val all: List<Language> = listOf(
        Language(code = "en", name = "English", nativeName = "English"),
        Language(code = "zh", name = "Mandarin Chinese", nativeName = "普通话")
    )

    fun byCode(code: String): Language? = all.find { it.code == code }
}
