package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.repository.MessageRepository

class RetryTranslationUseCase(private val repository: MessageRepository) {
    /** FAILED → TRANSLATING from the saved transcript; false if there is none or the Message isn't Failed. */
    suspend operator fun invoke(messageId: Long): Boolean = repository.retryTranslation(messageId)
}
