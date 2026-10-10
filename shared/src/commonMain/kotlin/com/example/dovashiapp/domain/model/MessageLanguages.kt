package com.example.dovashiapp.domain.model

/** A Message's Source and Target Language Codes, always the two Languages of its Conversation. */
data class MessageLanguages(val sourceLanguage: String, val targetLanguage: String)

/** "zh-CN", " ZH_hans " → "zh": a Language Code's lowercase base subtag. */
fun baseLanguageCode(code: String): String = code.trim().lowercase().substringBefore('-').substringBefore('_')

/** The other Language of the pair; the one rule that lets any Language Pair work unchanged. */
fun targetLanguageFor(sourceLanguage: String, conversation: Conversation): String =
    if (sourceLanguage == conversation.language1Code) conversation.language2Code else conversation.language1Code
