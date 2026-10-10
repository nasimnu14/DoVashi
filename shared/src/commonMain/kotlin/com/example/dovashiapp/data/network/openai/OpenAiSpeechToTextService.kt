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
import kotlinx.coroutines.CoroutineDispatcher
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
        val size = withContext(ioDispatcher) { openAiCall({ null }) { fileStorage.size(recordingReference) } }
            ?: throw SpeechToTextException(Reason.RECORDING_UNAVAILABLE)
        if (size > MAX_UPLOAD_BYTES) throw SpeechToTextException(Reason.RECORDING_REJECTED)
        val audio = readRecording(recordingReference) ?: throw SpeechToTextException(Reason.RECORDING_UNAVAILABLE)
        val fileName = recordingReference.substringAfterLast('/')
        val response = send(audio, fileName)
        when (classifyStatus(response.status.value)) {
            OpenAiStatus.OK -> Unit
            OpenAiStatus.REJECTED -> throw SpeechToTextException(Reason.RECORDING_REJECTED)
            OpenAiStatus.UNAUTHORIZED -> throw SpeechToTextException(Reason.UNAUTHORIZED)
            OpenAiStatus.RATE_LIMITED -> throw SpeechToTextException(Reason.RATE_LIMITED)
            OpenAiStatus.SERVER -> throw SpeechToTextException(Reason.SERVER)
        }
        if ((response.contentLength() ?: 0) > MAX_RESPONSE_BYTES) throw SpeechToTextException(Reason.INVALID_RESPONSE)
        // No cause on parse errors: their message quotes the body, i.e. the user's transcript.
        val body = openAiCall({ throw SpeechToTextException(Reason.INVALID_RESPONSE) }) {
            networkJson.decodeFromString<TranscriptionResponse>(response.bodyAsText())
        }
        // Whisper tends to invent text ("Thank you.") for silence; its own no-speech heuristic tells us when.
        val text = if (body.isSilence()) "" else body.text.trim()
        // Longer than a Message can store: retrying the same Recording would fail (and bill) again.
        if (text.length > MAX_MESSAGE_TEXT_LENGTH) throw SpeechToTextException(Reason.RECORDING_REJECTED)
        return Transcription(text, body.language?.let(::whisperLanguageCode))
    }

    /** Reads off the caller's thread (Recordings can be megabytes); any read failure means "unavailable". */
    private suspend fun readRecording(reference: String): ByteArray? =
        withContext(ioDispatcher) { openAiCall({ null }) { fileStorage.read(reference) } }

    // Connection, DNS, TLS and timeout failures; the cause carries no key or body.
    private suspend fun send(audio: ByteArray, fileName: String): HttpResponse =
        openAiCall({ throw SpeechToTextException(Reason.NETWORK, it) }) {
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
        }

    private companion object {
        const val MODEL = "whisper-1"
        /** OpenAI's transcription upload limit. */
        const val MAX_UPLOAD_BYTES = 25L * 1024 * 1024

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

