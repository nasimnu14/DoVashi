package com.example.dovashiapp.domain.service

/** Speech-to-text. Implementations may call OpenAI directly today and a backend proxy later. */
interface SpeechToTextService {
    /** Transcribes the Recording at [recordingReference]. Throws [SpeechToTextException] on any failure. */
    suspend fun transcribe(recordingReference: String): Transcription
}

/**
 * A transcript and the Language Code the speech-to-text call detected, if it could map one. [text] is empty when
 * nothing was said; callers check for visible text before resolving languages or saving.
 */
data class Transcription(val text: String, val languageCode: String?)

class SpeechToTextException(val reason: Reason, cause: Throwable? = null) :
    Exception("Speech-to-text failed: $reason", cause) {

    enum class Reason {
        MISSING_API_KEY,
        RECORDING_UNAVAILABLE,
        /** Too large, too short or an unsupported format: retrying the same Recording won't help. */
        RECORDING_REJECTED,
        UNAUTHORIZED,
        RATE_LIMITED,
        SERVER,
        NETWORK,
        INVALID_RESPONSE,
    }
}
