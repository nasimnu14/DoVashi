package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.MessageLanguages
import com.example.dovashiapp.domain.model.Script
import com.example.dovashiapp.domain.model.targetLanguageFor
import com.example.dovashiapp.domain.service.Transcription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolveMessageLanguagesUseCaseTest {
    private val latinA = Language("aa", "Alpha", "Alpha", scripts = setOf(Script.LATIN))
    private val hanB = Language("bb", "Beta", "Beta", scripts = setOf(Script.HAN))
    private val latinC = Language("cc", "Gamma", "Gamma", scripts = setOf(Script.LATIN))
    private val unknownD = Language("dd", "Delta", "Delta")
    private val catalog = listOf(latinA, hanB, latinC, unknownD)
    private val resolve = ResolveMessageLanguagesUseCase { code -> catalog.find { it.code == code } }

    private fun conversation(l1: String, l2: String) = Conversation(1, "t", l1, l2, 0, 0)

    @Test
    fun detectedLanguage1() =
        assertEquals(MessageLanguages("aa", "bb"), resolve(conversation("aa", "bb"), Transcription("hello", "aa")))

    @Test
    fun detectedLanguage2() =
        assertEquals(MessageLanguages("bb", "aa"), resolve(conversation("aa", "bb"), Transcription("你好", "bb")))

    @Test
    fun regionAndCaseVariantsAreNormalised() {
        assertEquals("bb", resolve(conversation("aa", "bb"), Transcription("你好", "BB-cn"))?.sourceLanguage)
        assertEquals("bb", resolve(conversation("aa", "bb"), Transcription("你好", " bb_Hans "))?.sourceLanguage)
    }

    @Test
    fun thirdLanguageCodeFallsBackToScript() =
        assertEquals(MessageLanguages("bb", "aa"), resolve(conversation("aa", "bb"), Transcription("你好", "yue")))

    @Test
    fun missingCodeFallsBackToScript() {
        assertEquals("aa", resolve(conversation("bb", "aa"), Transcription("Good morning", null))?.sourceLanguage)
        assertEquals("bb", resolve(conversation("aa", "bb"), Transcription("我用iPhone", null))?.sourceLanguage)
        assertEquals("aa", resolve(conversation("aa", "bb"), Transcription("I really love the city of 北京", null))?.sourceLanguage)
    }

    @Test
    fun scriptTieIsUndetermined() = assertNull(resolve(conversation("aa", "bb"), Transcription("hello 你", null)))

    @Test
    fun onlyScriptsUniqueToOneLanguageCount() {
        // Phase 2 shape: a Language written in Han and Kana against one written in Han only.
        val japaneseLike = Language("jj", "Jay", "Jay", scripts = setOf(Script.HAN, Script.KANA))
        val resolver = ResolveMessageLanguagesUseCase { code -> (catalog + japaneseLike).find { it.code == code } }
        assertEquals("jj", resolver(conversation("bb", "jj"), Transcription("日本語を勉強しています", null))?.sourceLanguage)
        assertNull(resolver(conversation("bb", "jj"), Transcription("日本語", null)), "Han only: shared by both")
    }

    @Test
    fun regionSubtagsInThePairAlsoMatch() {
        val regional = Language("bb-tw", "Beta TW", "Beta TW", scripts = setOf(Script.HAN))
        val resolver = ResolveMessageLanguagesUseCase { code -> (catalog + regional).find { it.code == code } }
        assertEquals(MessageLanguages("bb-tw", "aa"), resolver(conversation("aa", "bb-tw"), Transcription("你好", "bb")))
    }

    @Test
    fun detectedCodeMatchingBothRegionalCodesFallsBackToScript() {
        val regional1 = Language("bb-cn", "Beta CN", "Beta CN", scripts = setOf(Script.HAN))
        val regional2 = Language("bb-tw", "Beta TW", "Beta TW", scripts = setOf(Script.HAN))
        val resolver = ResolveMessageLanguagesUseCase { code -> listOf(regional1, regional2).find { it.code == code } }
        assertNull(resolver(conversation("bb-cn", "bb-tw"), Transcription("你好", "bb")))
    }

    @Test
    fun identicalPairCodesNeverResolve() = assertNull(resolve(conversation("aa", "aa"), Transcription("hello", "aa")))

    @Test
    fun detectedCodeWinsOverScript() =
        assertEquals("aa", resolve(conversation("aa", "bb"), Transcription("你好", "aa"))?.sourceLanguage)

    @Test
    fun sameScriptPairWithAnUnknownCodeIsUndetermined() =
        assertNull(resolve(conversation("aa", "cc"), Transcription("hola", "pt")))

    @Test
    fun textWithoutLettersIsUndetermined() = assertNull(resolve(conversation("aa", "bb"), Transcription("123 !!!", null)))

    @Test
    fun languageWithoutScriptsNeverMatchesByScript() {
        assertEquals("aa", resolve(conversation("aa", "dd"), Transcription("hello", null))?.sourceLanguage)
        assertNull(resolve(conversation("aa", "dd"), Transcription("你好", null)))
    }

    @Test
    fun targetIsAlwaysTheOtherLanguage() {
        val c = conversation("aa", "bb")
        assertEquals("bb", targetLanguageFor("aa", c))
        assertEquals("aa", targetLanguageFor("bb", c))
    }

    @Test
    fun phase1ExamplesWithTheRealCatalog() {
        val real = ResolveMessageLanguagesUseCase(LanguageCatalog::byCode)
        val (first, second) = LanguageCatalog.all
        val c = Conversation(1, "t", first.code, second.code, 0, 0)
        val reversed = Conversation(2, "t", second.code, first.code, 0, 0)
        assertEquals(MessageLanguages(first.code, second.code), real(c, Transcription("Good morning", null)))
        assertEquals(MessageLanguages(second.code, first.code), real(c, Transcription("你好", null)))
        assertEquals(MessageLanguages(first.code, second.code), real(reversed, Transcription("Good morning", null)))
        assertEquals(MessageLanguages(second.code, first.code), real(reversed, Transcription("你好", null)))
    }
}
