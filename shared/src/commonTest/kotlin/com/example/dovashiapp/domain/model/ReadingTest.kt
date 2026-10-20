package com.example.dovashiapp.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadingTest {
    private val plain = Language("aa", "Alpha", "Alpha", requiresReading = false)
    private val script = Language("bb", "Beta", "Beta", requiresReading = true)
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
}
