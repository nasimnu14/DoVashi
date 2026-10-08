package com.example.dovashiapp.domain.model

data class ConversationSummary(
    val conversation: Conversation,
    val lastMessageText: String?,
    val lastMessageStatus: MessageStatus?,
    val messageCount: Long,
)
