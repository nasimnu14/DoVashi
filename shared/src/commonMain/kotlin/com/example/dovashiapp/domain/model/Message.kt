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
) {
    /** The transcript and both Language Codes are saved, so a retry can resume at translation. */
    val hasTranscript: Boolean get() = transcribedText != null && sourceLanguage != null && targetLanguage != null

    /** Failed with something to resume from: the saved transcript, else the Recording (doc 12). */
    val isRetryable: Boolean get() = status == MessageStatus.FAILED && (hasTranscript || !audioPath.isNullOrBlank())
}
