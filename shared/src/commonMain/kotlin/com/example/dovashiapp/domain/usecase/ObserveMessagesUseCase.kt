package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow

class ObserveMessagesUseCase(private val repository: MessageRepository) {
    /** The newest [limit] Messages, newest first. */
    operator fun invoke(conversationId: Long, limit: Int): Flow<List<Message>> = repository.observeMessages(conversationId, limit)
}
