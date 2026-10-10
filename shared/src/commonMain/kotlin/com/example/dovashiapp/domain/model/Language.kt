package com.example.dovashiapp.domain.model

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    /** Whether a translation into this Language should carry a Reading (e.g. pinyin for a non-Latin script). */
    val requiresReading: Boolean = false,
)
