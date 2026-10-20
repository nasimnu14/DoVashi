package com.example.dovashiapp.data.network.openai

import com.example.dovashiapp.data.network.createHttpClient
import com.example.dovashiapp.domain.service.SpeechToTextException
import com.example.dovashiapp.domain.service.SpeechToTextException.Reason
import com.example.dovashiapp.domain.service.Transcription
import com.example.dovashiapp.testing.FakeFileStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.io.IOException
import kotlinx.coroutines.test.runTest

class OpenAiSpeechToTextServiceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<String>()
    private val files = FakeFileStorage(mapOf("audio/a.m4a" to byteArrayOf(1, 2, 3)))

    private fun TestScope.service(
        apiKey: String = "test-key",
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"text":" Good morning ","language":"english","duration":1.2}""",
        failure: Throwable? = null,
        baseUrl: String = "https://example.test/v1/",
    ): OpenAiSpeechToTextService {
        val engine = MockEngine { request ->
            requests += request
            bodies += request.body.toByteArray().decodeToString()
            failure?.let { throw it }
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return OpenAiSpeechToTextService(
            createHttpClient(engine), OpenAiConfig(apiKey, baseUrl), files, UnconfinedTestDispatcher(testScheduler),
        )
    }

    private suspend fun reasonOf(block: suspend () -> Unit): Reason =
        assertFailsWith<SpeechToTextException> { block() }.reason

    @Test
    fun sendsAWhisperVerboseJsonUploadAndParsesTheResult() = runTest {
        val result = service().transcribe("audio/a.m4a")

        assertEquals(Transcription("Good morning", "en"), result)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://example.test/v1/audio/transcriptions", request.url.toString())
        assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
        val body = bodies.single()
        fun field(name: String, value: String) =
            Regex("""name="$name"[^\r\n]*\r\n(?:[^\r\n]+\r\n)*\r\n${Regex.escape(value)}\r\n""").containsMatchIn(body)
        assertTrue(field("model", "whisper-1"), body)
        assertTrue(field("response_format", "verbose_json"), body)
        assertTrue(Regex("""name="file"; filename="a\.m4a"[\s\S]*?Content-Type: audio/mp4""").containsMatchIn(body), body)
    }

    @Test
    fun unknownLanguageGivesANullCode() = runTest {
        assertEquals(Transcription("你好", null), service(body = """{"text":"你好","language":"klingon"}""").transcribe("audio/a.m4a"))
    }

    @Test
    fun missingKeyFailsWithoutARequest() = runTest {
        assertEquals(Reason.MISSING_API_KEY, reasonOf { service(apiKey = " ").transcribe("audio/a.m4a") })
        assertTrue(requests.isEmpty())
    }

    @Test
    fun missingRecordingFailsWithoutARequest() = runTest {
        assertEquals(Reason.RECORDING_UNAVAILABLE, reasonOf { service().transcribe("audio/missing.m4a") })
        assertTrue(requests.isEmpty())
    }

    @Test
    fun statusCodesMapToReasons() = runTest {
        assertEquals(Reason.UNAUTHORIZED, reasonOf { service(status = HttpStatusCode.Unauthorized).transcribe("audio/a.m4a") })
        assertEquals(Reason.UNAUTHORIZED, reasonOf { service(status = HttpStatusCode.Forbidden).transcribe("audio/a.m4a") })
        assertEquals(Reason.RATE_LIMITED, reasonOf { service(status = HttpStatusCode.TooManyRequests).transcribe("audio/a.m4a") })
        assertEquals(Reason.SERVER, reasonOf { service(status = HttpStatusCode.InternalServerError).transcribe("audio/a.m4a") })
        assertEquals(Reason.RECORDING_REJECTED, reasonOf { service(status = HttpStatusCode.BadRequest).transcribe("audio/a.m4a") })
        assertEquals(Reason.RECORDING_REJECTED, reasonOf { service(status = HttpStatusCode.PayloadTooLarge).transcribe("audio/a.m4a") })
        assertEquals(Reason.RECORDING_REJECTED, reasonOf { service(status = HttpStatusCode.UnsupportedMediaType).transcribe("audio/a.m4a") })
        assertEquals(Reason.RECORDING_REJECTED, reasonOf { service(status = HttpStatusCode.UnprocessableEntity).transcribe("audio/a.m4a") })
        assertEquals(Reason.SERVER, reasonOf { service(status = HttpStatusCode.BadGateway).transcribe("audio/a.m4a") })
    }

    @Test
    fun errorsNeverCarryTheKey() = runTest {
        val e = assertFailsWith<SpeechToTextException> { service(status = HttpStatusCode.Unauthorized).transcribe("audio/a.m4a") }
        assertFalse("test-key" in e.toString())
    }

    @Test
    fun malformedResponsesAreInvalid() = runTest {
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = "not json").transcribe("audio/a.m4a") })
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = """{"language":"english"}""").transcribe("audio/a.m4a") })
    }

    @Test
    fun transportFailuresAreNetwork() = runTest {
        assertEquals(Reason.NETWORK, reasonOf { service(failure = IOException("connection reset")).transcribe("audio/a.m4a") })
        val timeout = HttpRequestTimeoutException("https://example.test/v1/audio/transcriptions", 120_000)
        assertEquals(Reason.NETWORK, reasonOf { service(failure = timeout).transcribe("audio/a.m4a") })
    }

    @Test
    fun invalidResponseCarriesNoBody() = runTest {
        val e = assertFailsWith<SpeechToTextException> { service(body = """{"txt":"secret words"}""").transcribe("audio/a.m4a") }
        assertNull(e.cause)
        assertFalse("secret" in e.toString())
    }

    @Test
    fun defaultBaseUrlIsOpenAiV1() = assertEquals("https://api.openai.com/v1", OpenAiConfig("k").baseUrl)

    @Test
    fun oversizedRecordingIsRejectedWithoutARequest() = runTest {
        files.sizes["audio/a.m4a"] = 26L * 1024 * 1024
        assertEquals(Reason.RECORDING_REJECTED, reasonOf { service().transcribe("audio/a.m4a") })
        assertTrue(requests.isEmpty())
    }

    @Test
    fun silenceDetectedByWhisperBecomesAnEmptyTranscript() = runTest {
        val silent = """{"text":"Thank you.","language":"english",
            "segments":[{"no_speech_prob":0.9,"avg_logprob":-1.5},{"no_speech_prob":0.8,"avg_logprob":-1.2}]}"""
        assertEquals(Transcription("", "en"), service(body = silent).transcribe("audio/a.m4a"))
        val speech = """{"text":"Hi","language":"english","segments":[{"no_speech_prob":0.9,"avg_logprob":-0.2}]}"""
        assertEquals(Transcription("Hi", "en"), service(body = speech).transcribe("audio/a.m4a"))
    }

    @Test
    fun wavRecordingsUploadAsAudioWav() = runTest {
        val wav = FakeFileStorage(mapOf("audio/seed-tone.wav" to byteArrayOf(1)))
        val engine = MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("""{"text":"x"}""", HttpStatusCode.OK)
        }
        OpenAiSpeechToTextService(createHttpClient(engine), OpenAiConfig("k"), wav, UnconfinedTestDispatcher(testScheduler))
            .transcribe("audio/seed-tone.wav")
        assertTrue(Regex("""filename="seed-tone\.wav"[\s\S]*?Content-Type: audio/wav""").containsMatchIn(bodies.single()), bodies.single())
    }

    @Test
    fun overLongTranscriptIsRejected() = runTest {
        val long = "x".repeat(10_001)
        assertEquals(Reason.RECORDING_REJECTED, reasonOf { service(body = """{"text":"$long"}""").transcribe("audio/a.m4a") })
    }

    @Test
    fun nullSegmentFieldsDoNotDiscardTheTranscript() = runTest {
        val body = """{"text":"Hi","segments":[{"no_speech_prob":null,"avg_logprob":null}]}"""
        assertEquals(Transcription("Hi", null), service(body = body).transcribe("audio/a.m4a"))
    }

    @Test
    fun storageFailuresAreUnavailable() = runTest {
        val broken = object : com.example.dovashiapp.audio.FileStorage {
            override fun resolve(reference: String): String? = error("disk")
            override fun size(reference: String): Long? = error("disk")
            override fun read(reference: String): ByteArray? = error("disk")
        }
        val stt = OpenAiSpeechToTextService(createHttpClient(MockEngine { error("no request expected") }), OpenAiConfig("k"), broken,
            UnconfinedTestDispatcher(testScheduler))
        assertEquals(Reason.RECORDING_UNAVAILABLE, reasonOf { stt.transcribe("audio/a.m4a") })
    }

    @Test
    fun networkErrorsNeverCarryTheKey() = runTest {
        val e = assertFailsWith<SpeechToTextException> { service(failure = IOException("reset")).transcribe("audio/a.m4a") }
        assertFalse("test-key" in e.toString() || "test-key" in e.cause.toString())
    }

    @Test
    fun oddButValidBodies() = runTest {
        assertEquals(Transcription("", null), service(body = """{"text":"   ","language":""}""").transcribe("audio/a.m4a"))
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = """{"text":null}""").transcribe("audio/a.m4a") })
    }

    @Test
    fun cancellationPassesThroughInsteadOfBecomingNetwork() = runTest {
        val engineEntered = CompletableDeferred<Unit>()
        val engine = MockEngine {
            engineEntered.complete(Unit)
            awaitCancellation()
        }
        val stt = OpenAiSpeechToTextService(
            createHttpClient(engine), OpenAiConfig("k"), files, UnconfinedTestDispatcher(testScheduler),
        )
        var failure: Throwable? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            try {
                stt.transcribe("audio/a.m4a")
            } catch (e: Throwable) {
                failure = e
                throw e
            }
        }
        engineEntered.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertTrue(failure is CancellationException, "was $failure")
    }
}
