package com.example.dovashiapp.domain.usecase

import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.conversationTitle
import com.example.dovashiapp.testing.FakeConversationRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class CreateConversationUseCaseTest {
    private val english = LanguageCatalog.byCode("en")!!
    private val mandarin = LanguageCatalog.byCode("zh")!!

    @Test
    fun titleIsBuiltFromBothLanguageNames() {
        assertEquals("English ↔ Mandarin Chinese", conversationTitle(english, mandarin))
    }

    @Test
    fun createsConversationWithTitleAndCodes() = runTest {
        val repository = FakeConversationRepository(emptyFlow())
        CreateConversationUseCase(repository)(mandarin, english)
        assertEquals(
            listOf(FakeConversationRepository.Created("Mandarin Chinese ↔ English", "zh", "en")),
            repository.created,
        )
    }

    @Test
    fun identicalLanguagesAreRejectedAndNothingIsWritten() = runTest {
        val repository = FakeConversationRepository(emptyFlow())
        assertFailsWith<IllegalArgumentException> { CreateConversationUseCase(repository)(english, english) }
        assertTrue(repository.created.isEmpty())
    }
}
