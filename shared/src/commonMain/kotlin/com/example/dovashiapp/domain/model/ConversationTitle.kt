package com.example.dovashiapp.domain.model

fun conversationTitle(language1: Language, language2: Language): String =
    "${language1.name} ↔ ${language2.name}"
