package com.example.dovashiapp.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScriptTest {
    @Test
    fun classifiesLettersByScript() {
        assertEquals(Script.LATIN, Script.of('a'))
        assertEquals(Script.LATIN, Script.of('é'))
        assertEquals(Script.HAN, Script.of('你'))
        assertEquals(Script.KANA, Script.of('か'))
        assertEquals(Script.KANA, Script.of('カ'))
        assertEquals(Script.HANGUL, Script.of('한'))
        assertEquals(Script.BENGALI, Script.of('আ'))
        assertEquals(Script.ARABIC, Script.of('م'))
        assertEquals(Script.DEVANAGARI, Script.of('न'))
        assertEquals(Script.CYRILLIC, Script.of('д'))
    }

    @Test
    fun nonLettersHaveNoScript() {
        for (c in listOf('1', ' ', '?', '？', '×', '÷', '。')) assertNull(Script.of(c), "'$c'")
    }

    @Test
    fun alphabetsCountWordsAndHanCountsCharacters() {
        assertEquals(mapOf(Script.LATIN to 2), scriptUnits("Good morning!"))
        assertEquals(mapOf(Script.HAN to 3), scriptUnits("你好吗？"))
        assertEquals(mapOf(Script.HAN to 2, Script.LATIN to 1), scriptUnits("我用iPhone"))
        assertEquals(mapOf(Script.HAN to 5, Script.KANA to 6), scriptUnits("日本語を勉強しています"))
    }

    @Test
    fun wordsContinueAcrossApostrophesHyphensAndCombiningMarks() {
        assertEquals(mapOf(Script.LATIN to 2), scriptUnits("it's well-known"))
        assertEquals(mapOf(Script.DEVANAGARI to 1), scriptUnits("हिन्दी"))
        assertEquals(mapOf(Script.LATIN to 1), scriptUnits("cafe\u0301"))
        assertEquals(mapOf(Script.HAN to 3, Script.LATIN to 2), scriptUnits("我觉得it's OK"))
    }

    @Test
    fun noLettersHaveNoUnits() {
        assertEquals(emptyMap(), scriptUnits(""))
        assertEquals(emptyMap(), scriptUnits("123 !!!"))
    }
}
