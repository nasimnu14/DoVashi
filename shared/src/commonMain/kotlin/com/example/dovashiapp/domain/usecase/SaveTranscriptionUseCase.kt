package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.MAX_MESSAGE_TEXT_LENGTH
import com.example.dovashiapp.domain.model.visibleTextOrNull
import com.example.dovashiapp.domain.repository.MessageRepository

class SaveTranscriptionUseCase(private val repository: MessageRepository) {
    /**
     * Returns false, writing nothing, when the transcript has no visible text (silence is an expected outcome) or is
     * longer than [MAX_MESSAGE_TEXT_LENGTH], the Message doesn't exist, or it isn't Transcribing. On false the
     * pipeline marks the Message failed (refused too if it moved on). Invalid Language Codes are a caller bug and
     * throw: they come from the Conversation's Language Pair, never straight from the network.
     */
    suspend operator fun invoke(
        messageId: Long,
        transcribedText: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): Boolean {
        require(sourceLanguage.isNotBlank() && targetLanguage.isNotBlank()) { "Both Language Codes are required" }
        require(sourceLanguage != targetLanguage) { "Source and target Language must differ" }
        val text = visibleTextOrNull(transcribedText)?.takeIf { it.length <= MAX_MESSAGE_TEXT_LENGTH } ?: return false
        return repository.saveTranscription(messageId, text, sourceLanguage, targetLanguage)
    }
}
