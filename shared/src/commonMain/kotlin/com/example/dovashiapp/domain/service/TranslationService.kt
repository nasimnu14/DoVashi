package com.example.dovashiapp.domain.service

/** Translation. Implementations may call OpenAI directly today and a backend proxy later. */
interface TranslationService {
    /** Throws [TranslationException] on any failure. */
    suspend fun translate(request: TranslationRequest): TranslationResult
}

/**
 * What a translation needs, as data: Language Codes, the transcript, and the target's Reading system (null when the
 * target needs no Reading).
 */
data class TranslationRequest(
    val sourceLanguage: String,
    val targetLanguage: String,
    val transcribedText: String,
    val readingSystem: String?,
) {
    val readingRequired: Boolean get() = readingSystem != null
}

/** The translation contract's answer; the same shape for every Language Pair. */
data class TranslationResult(
    val sourceLanguage: String,
    val targetLanguage: String,
    val transcribedText: String,
    val translatedText: String,
    val reading: String?,
)

class TranslationException(val reason: Reason, cause: Throwable? = null) :
    Exception("Translation failed: $reason", cause) {

    enum class Reason { MISSING_API_KEY, UNAUTHORIZED, RATE_LIMITED, REJECTED, SERVER, NETWORK, INVALID_RESPONSE, REFUSED }
}
