package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.repository.MessageRepository

class MarkMessageFailedUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(messageId: Long): Boolean = repository.markFailed(messageId)
}
