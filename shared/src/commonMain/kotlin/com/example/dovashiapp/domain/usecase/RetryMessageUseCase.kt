package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.repository.MessageRepository

/**
 * Retries a Failed Message without recording again (doc 12): with a saved transcript only translation runs again;
 * otherwise the pipeline restarts from the saved Recording. Does nothing for a Message that isn't Failed or has
 * neither to resume from. Failures mark the Message Failed again, keeping everything saved.
 */
class RetryMessageUseCase(
    private val messages: MessageRepository,
    private val retryTranslation: RetryTranslationUseCase,
    private val translateMessage: TranslateMessageUseCase,
    private val processRecording: ProcessRecordingUseCase,
    private val markFailed: MarkMessageFailedUseCase,
) {
    suspend operator fun invoke(messageId: Long) {
        val message = messages.getMessage(messageId)?.takeIf { it.isRetryable } ?: return
        if (message.hasTranscript) {
            failUnlessCompleted(markFailed, messageId) {
                if (!retryTranslation(messageId)) null else translateMessage(messageId)
            }
        } else {
            processRecording(messageId, message.conversationId, message.audioPath!!)
        }
    }
}
