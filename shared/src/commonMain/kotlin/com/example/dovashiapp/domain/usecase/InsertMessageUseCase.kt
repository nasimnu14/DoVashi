package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.repository.MessageRepository

class InsertMessageUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(
        conversationId: Long,
        status: MessageStatus,
        sourceLanguage: String? = null,
        targetLanguage: String? = null,
        audioPath: String? = null,
        transcribedText: String? = null,
        translatedText: String? = null,
        reading: String? = null,
    ): Long = repository.insertMessage(
        conversationId, status, sourceLanguage, targetLanguage, audioPath,
        transcribedText, translatedText, reading,
    )
}
