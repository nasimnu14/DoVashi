package com.example.dovashiapp.data.network.openai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WhisperLanguagesTest {
    @Test
    fun namesMapToCodes() {
        assertEquals("en", whisperLanguageCode("english"))
        assertEquals("zh", whisperLanguageCode("chinese"))
        assertEquals("ja", whisperLanguageCode(" Japanese "))
        assertEquals("bn", whisperLanguageCode("bengali"))
        assertEquals("jv", whisperLanguageCode("javanese"))
    }

    @Test
    fun codesAreNormalised() {
        assertEquals("zh", whisperLanguageCode("zh-CN"))
        assertEquals("en", whisperLanguageCode("EN"))
    }

    @Test
    fun unknownOrBlankIsNull() {
        assertNull(whisperLanguageCode("klingon"))
        assertNull(whisperLanguageCode("xx"))
        assertNull(whisperLanguageCode("  "))
    }
}
