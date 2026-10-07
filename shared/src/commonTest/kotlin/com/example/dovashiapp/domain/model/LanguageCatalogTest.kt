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
            Language(code = "en", name = "English", nativeName = "English"),
            LanguageCatalog.all.find { it.code == "en" }
        )
    }

    @Test
    fun `catalog contains Mandarin Chinese`() {
        assertEquals(
            Language(code = "zh", name = "Mandarin Chinese", nativeName = "普通话"),
            LanguageCatalog.all.find { it.code == "zh" }
        )
    }

    @Test
    fun `byCode resolves English`() {
        assertEquals(
            Language(code = "en", name = "English", nativeName = "English"),
            LanguageCatalog.byCode("en")
        )
    }

    @Test
    fun `byCode resolves Mandarin Chinese`() {
        assertEquals(
            Language(code = "zh", name = "Mandarin Chinese", nativeName = "普通话"),
            LanguageCatalog.byCode("zh")
        )
    }

    @Test
    fun `byCode returns null for unknown code`() {
        assertNull(LanguageCatalog.byCode("fr"))
    }
}
