package com.example.dovashiapp.data.network.openai

import com.example.dovashiapp.audio.FileStorage
import com.example.dovashiapp.data.network.networkJson
import com.example.dovashiapp.domain.model.MAX_MESSAGE_TEXT_LENGTH
import com.example.dovashiapp.domain.service.SpeechToTextException
import com.example.dovashiapp.domain.service.SpeechToTextException.Reason
import com.example.dovashiapp.domain.service.SpeechToTextService
import com.example.dovashiapp.domain.service.Transcription
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.quote
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OpenAI Whisper transcription. `verbose_json` is the only format that also reports the detected language. */
class OpenAiSpeechToTextService(
    private val httpClient: HttpClient,
    private val config: OpenAiConfig,
    private val fileStorage: FileStorage,
    private val ioDispatcher: CoroutineDispatcher,
) : SpeechToTextService {

    override suspend fun transcribe(recordingReference: String): Transcription {
        if (config.apiKey.isBlank()) throw SpeechToTextException(Reason.MISSING_API_KEY)
        val size = withContext(ioDispatcher) { runCatchingNonCancellation { fileStorage.size(recordingReference) } }
            ?: throw SpeechToTextException(Reason.RECORDING_UNAVAILABLE)
        if (size > MAX_UPLOAD_BYTES) throw SpeechToTextException(Reason.RECORDING_REJECTED)
        val audio = readRecording(recordingReference) ?: throw SpeechToTextException(Reason.RECORDING_UNAVAILABLE)
        val fileName = recordingReference.substringAfterLast('/')
        val response = send(audio, fileName)
        when (response.status.value) {
            in 200..299 -> Unit
            400, 413, 415 -> throw SpeechToTextException(Reason.RECORDING_REJECTED)
            401, 403 -> throw SpeechToTextException(Reason.UNAUTHORIZED)
            429 -> throw SpeechToTextException(Reason.RATE_LIMITED)
            else -> throw SpeechToTextException(Reason.SERVER)
        }
        if ((response.contentLength() ?: 0) > MAX_RESPONSE_BYTES) throw SpeechToTextException(Reason.INVALID_RESPONSE)
        val body = try {
            networkJson.decodeFromString<TranscriptionResponse>(response.bodyAsText())
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            // No cause: a parse error's message quotes the body, i.e. the user's transcript.
            throw SpeechToTextException(Reason.INVALID_RESPONSE)
        }
        // Whisper tends to invent text ("Thank you.") for silence; its own no-speech heuristic tells us when.
        val text = if (body.isSilence()) "" else body.text.trim()
        // Longer than a Message can store: retrying the same Recording would fail (and bill) again.
        if (text.length > MAX_MESSAGE_TEXT_LENGTH) throw SpeechToTextException(Reason.RECORDING_REJECTED)
        return Transcription(text, body.language?.let(::whisperLanguageCode))
    }

    /** Reads off the caller's thread (Recordings can be megabytes); any read failure means "unavailable". */
    private suspend fun readRecording(reference: String): ByteArray? =
        withContext(ioDispatcher) { runCatchingNonCancellation { fileStorage.read(reference) } }

    private suspend fun send(audio: ByteArray, fileName: String): HttpResponse = try {
        httpClient.submitFormWithBinaryData(
            url = "${config.baseUrl.trimEnd('/')}/audio/transcriptions",
            formData = formData {
                append("model", MODEL)
                append("response_format", "verbose_json")
                append(
                    "file",
                    audio,
                    Headers.build {
                        append(HttpHeaders.ContentType, contentTypeFor(fileName))
                        append(HttpHeaders.ContentDisposition, "filename=${fileName.quote()}") // quote() escapes
                    },
                )
            },
        ) { bearerAuth(config.apiKey) }
    } catch (e: Exception) {
        // Our own cancellation (Ktor may wrap it) propagates; anything else, a stray CancellationException
        // included, is a transport failure.
        currentCoroutineContext().ensureActive()
        // Connection, DNS, TLS and timeout failures; the cause carries no key or body.
        throw SpeechToTextException(Reason.NETWORK, e)
    }

    private companion object {
        const val MODEL = "whisper-1"
        /** OpenAI's transcription upload limit. */
        const val MAX_UPLOAD_BYTES = 25L * 1024 * 1024
        /** Far above any real transcription response; guards against a misbehaving proxy. */
        const val MAX_RESPONSE_BYTES = 1L * 1024 * 1024

        fun contentTypeFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
            "m4a", "mp4" -> "audio/mp4"
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "webm" -> "audio/webm"
            else -> "application/octet-stream"
        }
    }
}

@Serializable
private class TranscriptionResponse(
    val text: String,
    val language: String? = null,
    val segments: List<Segment> = emptyList(),
) {
    @Serializable
    class Segment(
        @SerialName("no_speech_prob") val noSpeechProb: Double? = null,
        @SerialName("avg_logprob") val avgLogprob: Double? = null,
    )

    /** Whisper's own thresholds: every segment is probably not speech and was decoded with low confidence. */
    fun isSilence(): Boolean = segments.isNotEmpty() &&
        segments.all { (it.noSpeechProb ?: 0.0) > 0.6 && (it.avgLogprob ?: 0.0) < -1.0 }
}

private suspend inline fun <T> runCatchingNonCancellation(block: () -> T?): T? = try {
    block()
} catch (e: Exception) {
    currentCoroutineContext().ensureActive()
    null
}
