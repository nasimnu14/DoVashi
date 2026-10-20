package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.repository.MessageRepository

class MarkMessageTranscribingUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(messageId: Long): Boolean = repository.markTranscribing(messageId)
}
