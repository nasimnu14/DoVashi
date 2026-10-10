package com.example.dovashiapp.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageCatalogTest {

    @Test
    fun `catalog contains exactly two languages`() {
        assertEquals(2, LanguageCatalog.all.size)
    }

    @Test
    fun `catalog contains English`() {
        assertEquals(
            Language(code = "en", name = "English", nativeName = "English", scripts = setOf(Script.LATIN)),
            LanguageCatalog.all.find { it.code == "en" }
        )
    }

    @Test
    fun `catalog contains Mandarin Chinese`() {
        assertEquals(
            Language(code = "zh", name = "Mandarin Chinese", nativeName = "普通话", readingSystem = "Hanyu Pinyin with tone marks", scripts = setOf(Script.HAN)),
            LanguageCatalog.all.find { it.code == "zh" }
        )
    }

    @Test
    fun `byCode resolves English`() {
        assertEquals(
            Language(code = "en", name = "English", nativeName = "English", scripts = setOf(Script.LATIN)),
            LanguageCatalog.byCode("en")
        )
    }

    @Test
    fun `byCode resolves Mandarin Chinese`() {
        assertEquals(
            Language(code = "zh", name = "Mandarin Chinese", nativeName = "普通话", readingSystem = "Hanyu Pinyin with tone marks", scripts = setOf(Script.HAN)),
            LanguageCatalog.byCode("zh")
        )
    }

    @Test
    fun `byCode returns null for unknown code`() {
        assertNull(LanguageCatalog.byCode("fr"))
    }

    @Test
    fun `Mandarin Chinese Readings use pinyin with tone marks`() {
        assertEquals("Hanyu Pinyin with tone marks", LanguageCatalog.byCode("zh")?.readingSystem)
        assertEquals(null, LanguageCatalog.byCode("en")?.readingSystem)
    }

    @Test
    fun `a blank Reading system is rejected`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { Language("xx", "X", "X", readingSystem = " ") }
    }

    @Test
    fun `only Mandarin Chinese requires a Reading in Phase 1`() {
        assertEquals(listOf("zh"), LanguageCatalog.all.filter { it.requiresReading }.map { it.code })
    }
}
