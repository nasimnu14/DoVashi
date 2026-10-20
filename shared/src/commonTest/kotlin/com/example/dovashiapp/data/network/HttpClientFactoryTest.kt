package com.example.dovashiapp.data.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class HttpClientFactoryTest {
    private val lines = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(message: String) {
            lines += message
        }
    }
    private val engine = MockEngine {
        respond("""{"text":"Good morning","language":"english"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    private val log get() = lines.joinToString("\n")

    @Test
    fun jsonRequestsAndResponsesAreLoggedWithTheKeyMasked() = runTest {
        createHttpClient(engine, logger).post("https://example.test/v1/chat/completions") {
            bearerAuth("sk-test-secret")
            contentType(ContentType.Application.Json)
            setBody("""{"model":"gpt-4.1-mini","text":"How are you?"}""")
        }
        assertTrue("--> POST https://example.test/v1/chat/completions" in log, log)
        assertTrue("How are you?" in log, log)
        assertTrue("<-- 200" in log && "Good morning" in log, log)
        assertFalse("sk-test-secret" in log, log)
    }

    @Test
    fun recordingUploadsAreSummarisedNotDumped() = runTest {
        val audio = ByteArray(50_000) { (it * 37).toByte() }
        createHttpClient(engine, logger).submitFormWithBinaryData(
            "https://example.test/v1/audio/transcriptions",
            formData {
                append("model", "whisper-1")
                append("file", audio, Headers.build { append(HttpHeaders.ContentType, "audio/mp4"); append(HttpHeaders.ContentDisposition, "filename=\"a.m4a\"") })
            },
        ) { bearerAuth("sk-test-secret") }
        assertTrue("whisper-1" in log, log)
        assertTrue("binary 50000-byte body omitted" in log, log)
        assertTrue(log.length < 5_000, "the audio bytes must not be written out (log is ${log.length} chars)")
        assertFalse("sk-test-secret" in log, log)
    }

    @Test
    fun withoutALoggerNothingIsLogged() = runTest {
        createHttpClient(engine).post("https://example.test/v1/chat/completions") { setBody("x") }
        assertTrue(lines.isEmpty())
    }
}
