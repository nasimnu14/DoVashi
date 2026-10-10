package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.repository.MessageRepository
import com.example.dovashiapp.domain.service.TranslationRequest
import com.example.dovashiapp.domain.service.TranslationService

/**
 * Translates a Translating Message from its stored transcript and saves the result (COMPLETED). Returns false,
 * without calling the service, when the Message isn't Translating or lacks its transcript or Language Codes; false
 * when its inputs changed during the call (the result is discarded); and false when the result can't be saved. A [com.example.dovashiapp.domain.service.TranslationException] propagates:
 * the caller marks the Message failed.
 */
class TranslateMessageUseCase(
    private val messageRepository: MessageRepository,
    private val translationService: TranslationService,
    private val saveTranslation: SaveTranslationUseCase,
    private val languageByCode: (String) -> Language? = LanguageCatalog::byCode,
) {
    suspend operator fun invoke(messageId: Long): Boolean {
        val message = messageRepository.getMessage(messageId) ?: return false
        if (message.status != MessageStatus.TRANSLATING) return false
        val text = message.transcribedText ?: return false
        val source = message.sourceLanguage ?: return false
        val target = message.targetLanguage ?: return false
        val result = translationService.translate(
            TranslationRequest(source, target, text, readingRequired = languageByCode(target)?.requiresReading == true),
        )
        // Defence in depth for S8: never store a result for inputs that changed while the call was in flight.
        val current = messageRepository.getMessage(messageId) ?: return false
        if (current.status != MessageStatus.TRANSLATING || current.transcribedText != text ||
            current.sourceLanguage != source || current.targetLanguage != target
        ) {
            return false
        }
        return saveTranslation(messageId, result.translatedText, result.reading)
    }
}
