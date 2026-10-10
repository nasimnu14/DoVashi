package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.service.TranslationException
import com.example.dovashiapp.domain.service.TranslationRequest
import com.example.dovashiapp.domain.service.TranslationResult
import com.example.dovashiapp.domain.service.TranslationService
import com.example.dovashiapp.testing.FakeMessageRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class TranslateMessageUseCaseTest {
    private val plain = Language("aa", "Alpha", "Alpha")
    private val script = Language("bb", "Beta", "Beta", readingSystem = "Test Romanization")
    private val byCode: (String) -> Language? = { code -> listOf(plain, script).find { it.code == code } }
    private val repository = FakeMessageRepository()

    private class FakeTranslator(var failure: Exception? = null) : TranslationService {
        val requests = mutableListOf<TranslationRequest>()
        /** Runs during the call, to simulate the Message changing while the request is in flight. */
        var duringCall: suspend () -> Unit = {}
        override suspend fun translate(request: TranslationRequest): TranslationResult {
            requests += request
            duringCall()
            failure?.let { throw it }
            return TranslationResult(request.sourceLanguage, request.targetLanguage, request.transcribedText, "translated", "reading")
        }
    }

    private val translator = FakeTranslator()
    private val translate = TranslateMessageUseCase(repository, translator, SaveTranslationUseCase(repository, byCode), byCode)

    private suspend fun translating(source: String?, target: String?, text: String? = "hello") =
        repository.insertMessage(1, MessageStatus.TRANSLATING, source, target, "audio/a.m4a", text)

    @Test
    fun translatesAndCompletesWithReadingWhenTheTargetNeedsOne() = runTest {
        val id = translating("aa", "bb")
        assertTrue(translate(id))
        assertEquals(TranslationRequest("aa", "bb", "hello", readingSystem = "Test Romanization"), translator.requests.single())
        val message = repository.getMessage(id)!!
        assertEquals(MessageStatus.COMPLETED, message.status)
        assertEquals("translated" to "reading", message.translatedText to message.reading)
    }

    @Test
    fun readingFlagFollowsTargetMetadataAndUnneededReadingIsDropped() = runTest {
        val id = translating("bb", "aa")
        assertTrue(translate(id))
        assertFalse(translator.requests.single().readingRequired)
        assertNull(translator.requests.single().readingSystem)
        assertNull(repository.getMessage(id)!!.reading)
    }

    @Test
    fun realCatalogAsksForPinyinForMandarinChinese() = runTest {
        val (english, mandarin) = LanguageCatalog.all
        val real = TranslateMessageUseCase(repository, translator, SaveTranslationUseCase(repository, LanguageCatalog::byCode), LanguageCatalog::byCode)
        val id = translating(english.code, mandarin.code)
        assertTrue(real(id))
        assertEquals("Hanyu Pinyin with tone marks", translator.requests.single().readingSystem)
    }

    @Test
    fun preconditionsFailWithoutCallingTheService() = runTest {
        val completed = repository.insertMessage(1, MessageStatus.COMPLETED, "aa", "bb", null, "x", "y")
        val noText = translating("aa", "bb", text = null)
        val noSource = translating(null, "bb")
        val noTarget = translating("aa", null)
        for (id in listOf(completed, noText, noSource, noTarget, 999L)) assertFalse(translate(id), "id $id")
        assertTrue(translator.requests.isEmpty())
    }

    @Test
    fun resultIsDiscardedWhenTheMessageChangedDuringTheCall() = runTest {
        val id = translating("aa", "bb")
        translator.duringCall = {
            // A superseding attempt: failed, retried from the Recording, re-transcribed in the other direction.
            repository.markFailed(id)
            repository.markTranscribing(id)
            repository.saveTranscription(id, "different", "bb", "aa")
        }
        assertFalse(translate(id))
        val message = repository.getMessage(id)!!
        assertEquals(MessageStatus.TRANSLATING, message.status)
        assertNull(message.translatedText)
    }

    @Test
    fun serviceFailurePropagatesAndLeavesTheMessageTranslating() = runTest {
        val id = translating("aa", "bb")
        translator.failure = TranslationException(TranslationException.Reason.NETWORK)
        assertFailsWith<TranslationException> { translate(id) }
        assertEquals(MessageStatus.TRANSLATING, repository.getMessage(id)!!.status)
    }
}
