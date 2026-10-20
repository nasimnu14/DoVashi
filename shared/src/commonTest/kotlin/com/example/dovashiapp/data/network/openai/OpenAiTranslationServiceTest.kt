package com.example.dovashiapp.data.network.openai

import com.example.dovashiapp.data.network.createHttpClient
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.service.TranslationException
import com.example.dovashiapp.domain.service.TranslationException.Reason
import com.example.dovashiapp.domain.service.TranslationRequest
import com.example.dovashiapp.domain.service.TranslationResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class OpenAiTranslationServiceTest {
    private val catalog = listOf(Language("aa", "Alpha", "Alpha"), Language("bb", "Beta", "Beta", requiresReading = true))
    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<String>()
    private val toB = TranslationRequest("aa", "bb", "How are you?", readingRequired = true)

    private fun content(source: String = "aa", target: String = "bb", translated: String = "Ça va ?", reading: String? = "sa va") =
        buildJsonObject {
            put("sourceLanguage", source); put("targetLanguage", target)
            put("translatedText", translated); put("englishReading", reading)
        }.toString()

    private fun chat(content: String?, finishReason: String = "stop", refusal: String? = null) = buildJsonObject {
        put("choices", Json.parseToJsonElement(
            """[{"finish_reason":"$finishReason","message":{"role":"assistant","content":${Json.encodeToString(content)},"refusal":${Json.encodeToString(refusal)}}}]""",
        ))
    }.toString()

    private fun service(
        apiKey: String = "test-key",
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = chat(content()),
        failure: Throwable? = null,
        headers: io.ktor.http.Headers = headersOf(HttpHeaders.ContentType, "application/json"),
    ): OpenAiTranslationService {
        val engine = MockEngine { request ->
            requests += request
            bodies += request.body.toByteArray().decodeToString()
            failure?.let { throw it }
            respond(body, status, headers)
        }
        return OpenAiTranslationService(createHttpClient(engine), OpenAiConfig(apiKey, "https://example.test/v1"), { c -> catalog.find { it.code == c } })
    }

    private suspend fun reasonOf(block: suspend () -> Unit): Reason = assertFailsWith<TranslationException> { block() }.reason

    @Test
    fun sendsStructuredFieldsAndAStrictSchemaPinnedToTheCodes() = runTest {
        val result = service().translate(toB)

        assertEquals(TranslationResult("aa", "bb", "How are you?", "Ça va ?", "sa va"), result)
        val request = requests.single()
        assertEquals("https://example.test/v1/chat/completions", request.url.toString())
        assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
        val json = Json.parseToJsonElement(bodies.single()).jsonObject
        assertEquals("gpt-4.1-mini", json["model"]!!.jsonPrimitive.content)
        assertEquals("0.2", json["temperature"]!!.jsonPrimitive.content)
        assertTrue(json["max_completion_tokens"]!!.jsonPrimitive.content.toInt() in 1_024..16_384)
        val format = json["response_format"]!!.jsonObject
        assertEquals("json_schema", format["type"]!!.jsonPrimitive.content)
        val schema = format["json_schema"]!!.jsonObject
        assertEquals("translation", schema["name"]!!.jsonPrimitive.content)
        assertTrue(schema["strict"]!!.jsonPrimitive.boolean)
        val root = schema["schema"]!!.jsonObject
        assertEquals("object", root["type"]!!.jsonPrimitive.content)
        assertEquals("false", root["additionalProperties"]!!.jsonPrimitive.content)
        val properties = root["properties"]!!.jsonObject
        assertEquals("[\"aa\"]", properties["sourceLanguage"]!!.jsonObject["enum"].toString())
        assertEquals("[\"bb\"]", properties["targetLanguage"]!!.jsonObject["enum"].toString())
        assertEquals("\"string\"", properties["englishReading"]!!.jsonObject["type"].toString(), "required → non-null")
        assertEquals(properties.keys, root["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet())
        assertEquals(setOf("sourceLanguage", "targetLanguage", "translatedText", "englishReading"), properties.keys)
        val messages = json["messages"]!!.jsonArray
        assertEquals("system", messages[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertTrue("speech to translate" in messages[0].jsonObject["content"]!!.jsonPrimitive.content)
        val user = messages[1].jsonObject["content"]!!.jsonPrimitive.content
        val fields = Json.parseToJsonElement(user.substringAfter('\n')) as JsonObject
        assertEquals("aa", fields["sourceLanguage"]!!.jsonPrimitive.content)
        assertEquals("bb", fields["targetLanguage"]!!.jsonPrimitive.content)
        assertEquals("Alpha", fields["sourceLanguageName"]!!.jsonPrimitive.content)
        assertEquals("Beta", fields["targetLanguageName"]!!.jsonPrimitive.content)
        assertEquals("How are you?", fields["text"]!!.jsonPrimitive.content)
        assertTrue(fields["pronunciationRequired"]!!.jsonPrimitive.boolean)
        assertTrue("englishReading" in user)
    }

    @Test
    fun noReadingWantedAndNullReadingParses() = runTest {
        val reverse = TranslationRequest("bb", "aa", "x", readingRequired = false)
        val result = service(body = chat(content(source = "bb", target = "aa", reading = null))).translate(reverse)
        assertEquals(TranslationResult("bb", "aa", "x", "Ça va ?", null), result)
        val json = Json.parseToJsonElement(bodies.single()).jsonObject
        val user = json["messages"]!!.jsonArray[1].jsonObject["content"]!!.jsonPrimitive.content
        assertTrue("Set englishReading to null." in user)
        val reading = json["response_format"]!!.jsonObject["json_schema"]!!.jsonObject["schema"]!!.jsonObject["properties"]!!
            .jsonObject["englishReading"]!!.jsonObject["type"].toString()
        assertEquals("[\"string\",\"null\"]", reading)
    }

    @Test
    fun readingIsDroppedWhenNotRequestedEvenIfTheModelSendsOne() = runTest {
        val reverse = TranslationRequest("bb", "aa", "x", readingRequired = false)
        assertNull(service(body = chat(content(source = "bb", target = "aa", reading = "extra"))).translate(reverse).reading)
    }

    @Test
    fun overLongTranslationIsRejected() = runTest {
        assertEquals(Reason.REJECTED, reasonOf { service(body = chat(content(translated = "x".repeat(10_001)))).translate(toB) })
    }

    @Test
    fun outputTokensAreCapped() = runTest {
        service().translate(toB.copy(transcribedText = "x".repeat(10_000)))
        assertEquals(8_192, Json.parseToJsonElement(bodies.single()).jsonObject["max_completion_tokens"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun targetOnlyMismatchIsInvalid() = runTest {
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = chat(content(target = "aa"))).translate(toB) })
    }

    @Test
    fun missingFinishReasonOrBlankContentIsInvalid() = runTest {
        val noFinish = """{"choices":[{"message":{"content":${Json.encodeToString(content())}}}]}"""
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = noFinish).translate(toB) })
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = chat("   ")).translate(toB) })
    }

    @Test
    fun errorObjectInA200IsInvalid() = runTest {
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = """{"error":{"message":"boom"}}""").translate(toB) })
    }

    @Test
    fun missingKeyFailsWithoutARequest() = runTest {
        assertEquals(Reason.MISSING_API_KEY, reasonOf { service(apiKey = "").translate(toB) })
        assertTrue(requests.isEmpty())
    }

    @Test
    fun statusCodesMapToReasons() = runTest {
        assertEquals(Reason.UNAUTHORIZED, reasonOf { service(status = HttpStatusCode.Unauthorized).translate(toB) })
        assertEquals(Reason.RATE_LIMITED, reasonOf { service(status = HttpStatusCode.TooManyRequests).translate(toB) })
        assertEquals(Reason.REJECTED, reasonOf { service(status = HttpStatusCode.BadRequest).translate(toB) })
        assertEquals(Reason.REJECTED, reasonOf { service(status = HttpStatusCode.NotFound).translate(toB) })
        assertEquals(Reason.REJECTED, reasonOf { service(status = HttpStatusCode.UnprocessableEntity).translate(toB) })
        assertEquals(Reason.SERVER, reasonOf { service(status = HttpStatusCode.InternalServerError).translate(toB) })
    }

    @Test
    fun transportFailureIsNetworkWithoutTheKey() = runTest {
        val e = assertFailsWith<TranslationException> { service(failure = IOException("reset")).translate(toB) }
        assertEquals(Reason.NETWORK, e.reason)
        assertFalse(generateSequence<Throwable>(e) { it.cause }.any { "test-key" in it.toString() })
    }

    @Test
    fun refusalAndContentFilterAreRefused() = runTest {
        assertEquals(Reason.REFUSED, reasonOf { service(body = chat(null, refusal = "I can't help with that.")).translate(toB) })
        assertEquals(Reason.REFUSED, reasonOf { service(body = chat(null, finishReason = "content_filter")).translate(toB) })
    }

    @Test
    fun runningOutOfTokensIsRejected() = runTest {
        assertEquals(Reason.REJECTED, reasonOf { service(body = chat(content(), finishReason = "length")).translate(toB) })
    }

    @Test
    fun truncatedEmptyOrMalformedOutputIsInvalid() = runTest {
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = """{"choices":[]}""").translate(toB) })
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = chat("not json")).translate(toB) })
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = chat(null)).translate(toB) })
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = "<html>").translate(toB) })
    }

    @Test
    fun echoMismatchIsInvalid() = runTest {
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = chat(content(source = "bb", target = "aa"))).translate(toB) })
    }

    @Test
    fun oversizedResponseIsInvalid() = runTest {
        // A valid reply padded past 1 MB: only the size cap can reject it.
        val padded = chat(content()) + " ".repeat(1_100_000)
        val big = headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.ContentLength to listOf(padded.encodeToByteArray().size.toString()))
        assertEquals(Reason.INVALID_RESPONSE, reasonOf { service(body = padded, headers = big).translate(toB) })
    }

    @Test
    fun cancellationPassesThrough() = runTest {
        val entered = CompletableDeferred<Unit>()
        val engine = MockEngine { entered.complete(Unit); awaitCancellation() }
        val translator = OpenAiTranslationService(createHttpClient(engine), OpenAiConfig("k"))
        var failure: Throwable? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            try { translator.translate(toB) } catch (e: Throwable) { failure = e; throw e }
        }
        entered.await()
        job.cancelAndJoin()
        assertTrue(failure is CancellationException, "was $failure")
    }
}
