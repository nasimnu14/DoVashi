package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.MAX_MESSAGE_TEXT_LENGTH
import com.example.dovashiapp.domain.model.readingFor
import com.example.dovashiapp.domain.model.visibleTextOrNull
import com.example.dovashiapp.domain.repository.MessageRepository

class SaveTranslationUseCase(
    private val repository: MessageRepository,
    private val languageByCode: (String) -> Language? = LanguageCatalog::byCode,
) {
    /**
     * Stores the translation, keeping [reading] only if the Message's target Language requires one. Returns false,
     * writing nothing, when the translation has no visible text or is longer than [MAX_MESSAGE_TEXT_LENGTH], the
     * Message doesn't exist, or it isn't Translating. An over-long Reading is dropped.
     */
    suspend operator fun invoke(messageId: Long, translatedText: String, reading: String?): Boolean {
        val text = visibleTextOrNull(translatedText)?.takeIf { it.length <= MAX_MESSAGE_TEXT_LENGTH } ?: return false
        val message = repository.getMessage(messageId) ?: return false
        val storedReading = readingFor(message.targetLanguage, reading, languageByCode)?.takeIf { it.length <= MAX_MESSAGE_TEXT_LENGTH }
        return repository.saveTranslation(messageId, text, storedReading)
    }
}
