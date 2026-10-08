package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.conversationTitle
import com.example.dovashiapp.domain.repository.ConversationRepository

class CreateConversationUseCase(private val repository: ConversationRepository) {
    suspend operator fun invoke(language1: Language, language2: Language): Long {
        require(language1.code != language2.code) { "A conversation needs two different languages" }
        return repository.createConversation(
            title = conversationTitle(language1, language2),
            language1Code = language1.code,
            language2Code = language2.code,
        )
    }
}
