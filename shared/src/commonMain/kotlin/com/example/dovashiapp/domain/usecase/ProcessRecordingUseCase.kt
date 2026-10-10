package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.visibleTextOrNull
import com.example.dovashiapp.domain.repository.ConversationRepository
import com.example.dovashiapp.domain.service.SpeechToTextService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * The voice pipeline after recording (doc 06): Transcribing → speech-to-text → Source/Target Language from the
 * Conversation's pair → Translating → translation (+ Reading) → Completed. Every step persists before the next one,
 * and any failure marks the Message Failed, keeping everything saved so far.
 */
class ProcessRecordingUseCase(
    private val markTranscribing: MarkMessageTranscribingUseCase,
    private val speechToText: SpeechToTextService,
    private val conversations: ConversationRepository,
    private val resolveLanguages: ResolveMessageLanguagesUseCase,
    private val saveTranscription: SaveTranscriptionUseCase,
    private val translateMessage: TranslateMessageUseCase,
    private val markFailed: MarkMessageFailedUseCase,
) {
    suspend operator fun invoke(messageId: Long, conversationId: Long, recordingReference: String) {
        val completed = try {
            if (!markTranscribing(messageId)) return
            transcribeAndTranslate(messageId, conversationId, recordingReference)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive() // cancelled: leave it to the next attempt or the startup sweep
            false
        }
        if (!completed) {
            try {
                markFailed(messageId)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive() // the startup sweep will fail it instead
            }
        }
    }

    private suspend fun transcribeAndTranslate(messageId: Long, conversationId: Long, recordingReference: String): Boolean {
        val transcription = speechToText.transcribe(recordingReference)
        val text = visibleTextOrNull(transcription.text) ?: return false // silence
        val conversation = conversations.getConversation(conversationId) ?: return false
        val languages = resolveLanguages(conversation, transcription) ?: return false
        if (!saveTranscription(messageId, text, languages.sourceLanguage, languages.targetLanguage)) return false
        return translateMessage(messageId)
    }
}
