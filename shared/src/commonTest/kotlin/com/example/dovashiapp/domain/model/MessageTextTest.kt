package com.example.dovashiapp.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageTextTest {
    @Test
    fun trimsSurroundingWhitespaceIncludingIdeographicSpace() = assertEquals("你好", visibleTextOrNull("\u3000 你好 \n"))

    @Test
    fun nothingVisibleIsNull() {
        assertNull(visibleTextOrNull(null))
        assertNull(visibleTextOrNull("   "))
        assertNull(visibleTextOrNull("\u200B"))
        assertNull(visibleTextOrNull("\uFEFF \u200B"))
    }

    @Test
    fun innerFormatCharactersAreKept() = assertEquals("a\u200Db", visibleTextOrNull(" a\u200Db "))

    @Test
    fun formatAndControlCharactersAreTrimmedFromTheEnds() {
        assertEquals("hi", visibleTextOrNull("\uFEFFhi\u200B"))
        assertNull(visibleTextOrNull("\u0000\u0007"))
    }
}
