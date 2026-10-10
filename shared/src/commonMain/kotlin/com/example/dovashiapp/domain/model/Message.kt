package com.example.dovashiapp.domain.model

/** One spoken turn. Language fields are Language Codes; `audioPath` is relative to app-private storage. */
data class Message(
    val id: Long,
    val conversationId: Long,
    val sourceLanguage: String?,
    val targetLanguage: String?,
    val audioPath: String?,
    val transcribedText: String?,
    val translatedText: String?,
    val reading: String?,
    val status: MessageStatus,
    val createdAt: Long,
)
