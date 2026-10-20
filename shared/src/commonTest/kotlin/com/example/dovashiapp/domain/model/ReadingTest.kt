package com.example.dovashiapp.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadingTest {
    private val plain = Language("aa", "Alpha", "Alpha")
    private val script = Language("bb", "Beta", "Beta", readingSystem = "Test Romanization")
    private val byCode: (String) -> Language? = { code -> listOf(plain, script).find { it.code == code } }

    @Test
    fun keptAndTrimmedWhenTheTargetRequiresOne() = assertEquals("bee", readingFor("bb", " bee ", byCode))

    @Test
    fun droppedWhenTheTargetDoesNotRequireOne() = assertNull(readingFor("aa", "x", byCode))

    @Test
    fun droppedForAnUnknownTarget() = assertNull(readingFor("zz", "x", byCode))

    @Test
    fun droppedWhenThereIsNoTarget() = assertNull(readingFor(null, "x", byCode))

    @Test
    fun blankOrMissingReadingIsNull() {
        assertNull(readingFor("bb", "   ", byCode))
        assertNull(readingFor("bb", null, byCode))
    }

    @Test
    fun pinyinWithToneMarksAndOtherLatinReadingsAreKept() {
        assertEquals("Nǐ hǎo ma?", readingFor("bb", "Nǐ hǎo ma?", byCode))
        assertEquals("Lǜ chá, nǚ hái", readingFor("bb", "Lǜ chá, nǚ hái", byCode))
        assertEquals("Genki desu ka?", readingFor("bb", "Genki desu ka?", byCode))
        assertEquals("Nǐ 3 suì le!", readingFor("bb", "Nǐ 3 suì le!", byCode))
    }

    @Test
    fun readingsWithNonLatinLettersAreDropped() {
        assertNull(readingFor("bb", "你好", byCode))
        assertNull(readingFor("bb", "Ni hao 你", byCode))
        assertNull(readingFor("bb", "げんき", byCode))
        assertNull(readingFor("bb", "ㄋㄧˇ ㄏㄠˇ", byCode), "Zhuyin")
        assertNull(readingFor("bb", "ｹﾞﾝｷ", byCode), "half-width kana")
        assertNull(readingFor("bb", "Γειά", byCode), "Greek")
        assertNull(readingFor("bb", "\uD842\uDFB7", byCode), "Han outside the BMP")
    }

    @Test
    fun readingsWithoutLettersAreDropped() {
        assertNull(readingFor("bb", "—", byCode))
        assertNull(readingFor("bb", "?!", byCode))
    }

    @Test
    fun emojiAreNotLetters() {
        assertEquals("Xièxie 👍", readingFor("bb", "Xièxie 👍", byCode))
        assertNull(readingFor("bb", "👍", byCode), "no letters")
    }

    @Test
    fun phoneticExtensionLettersAreReadable() = assertEquals("ᴀᴅ", readingFor("bb", "ᴀᴅ", byCode))

    @Test
    fun ipaAndFullWidthLatinAreReadable() {
        assertEquals("ɑ", readingFor("bb", "ɑ", byCode))
        assertEquals("Ｎｉ", readingFor("bb", "Ｎｉ", byCode))
    }
}
