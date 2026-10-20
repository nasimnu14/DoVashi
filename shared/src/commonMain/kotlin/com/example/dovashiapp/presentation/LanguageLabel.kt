package com.example.dovashiapp.presentation

import com.example.dovashiapp.domain.model.Language

/** The Language's catalog name, or the raw Language Code when the catalog doesn't know it. */
fun languageLabel(code: String, languageByCode: (String) -> Language?): String = languageByCode(code)?.name ?: code
