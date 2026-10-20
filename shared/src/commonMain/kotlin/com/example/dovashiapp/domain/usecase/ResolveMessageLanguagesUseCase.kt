package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.MessageLanguages
import com.example.dovashiapp.domain.model.Script
import com.example.dovashiapp.domain.model.baseLanguageCode
import com.example.dovashiapp.domain.model.scriptUnits
import com.example.dovashiapp.domain.model.targetLanguageFor
import com.example.dovashiapp.domain.service.Transcription

/**
 * Picks a Message's Source Language from its Conversation's Language Pair: the language the speech-to-text call
 * detected if it is one of the two; otherwise the one whose own scripts (those the other Language doesn't share)
 * carry more of the transcript. Returns null rather than guess. The Target Language is always the other one.
 */
class ResolveMessageLanguagesUseCase(
    private val languageByCode: (String) -> Language? = LanguageCatalog::byCode,
) {
    operator fun invoke(conversation: Conversation, transcription: Transcription): MessageLanguages? {
        val pair = listOf(conversation.language1Code, conversation.language2Code)
        if (pair[0] == pair[1]) return null // not a valid pair (I7); never produce source == target
        val detected = transcription.languageCode?.let(::baseLanguageCode)
        // Only a detected code that names exactly one of the two counts (e.g. not "zh" for a zh-CN ↔ zh-TW pair).
        val source = pair.singleOrNull { baseLanguageCode(it) == detected }
            ?: byScript(pair[0], pair[1], transcription.text)
            ?: return null
        return MessageLanguages(source, targetLanguageFor(source, conversation))
    }

    private fun byScript(language1: String, language2: String, text: String): String? {
        val units = scriptUnits(text)
        val scripts1 = scriptsOf(language1)
        val scripts2 = scriptsOf(language2)
        val score1 = (scripts1 - scripts2).sumOf { units[it] ?: 0 }
        val score2 = (scripts2 - scripts1).sumOf { units[it] ?: 0 }
        return when {
            score1 > score2 -> language1
            score2 > score1 -> language2
            else -> null
        }
    }

    private fun scriptsOf(code: String): Set<Script> = languageByCode(code)?.scripts.orEmpty()
}
