package com.example.dovashiapp.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.dovashiapp.data.database.DoVashiDatabase
import com.example.dovashiapp.data.debug.DebugSeeder
import com.example.dovashiapp.domain.model.ConversationSummary
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.model.Message
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.usecase.CreateConversationUseCase
import com.example.dovashiapp.testing.FakeClock
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SqlRepositoriesTest {

    private class Fixture(scope: TestScope) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
            .also { DoVashiDatabase.Schema.create(it) }
        val database = DoVashiDatabase(driver)
        val clock = FakeClock(Instant.parse("2026-10-08T12:00:00Z"))
        val dispatcher = UnconfinedTestDispatcher(scope.testScheduler)
        val conversations = SqlConversationRepository(database, clock, dispatcher)
        val messages = SqlMessageRepository(database, clock, dispatcher)

        suspend fun conversation(title: String = "T"): Long = conversations.createConversation(title, "en", "zh")
        suspend fun summaries(): List<ConversationSummary> = conversations.observeSummaries().first()
    }

    @Test
    fun createdConversationHasEqualCreatedAndUpdatedAt() = runTest {
        val f = Fixture(this)
        f.conversation("English ↔ Mandarin Chinese")
        val c = f.summaries().single().conversation
        assertEquals(f.clock.instant.toEpochMilliseconds(), c.createdAt)
        assertEquals(c.createdAt, c.updatedAt)
        assertEquals("English ↔ Mandarin Chinese", c.title)
        assertEquals("en", c.language1Code)
        assertEquals("zh", c.language2Code)
    }

    @Test
    fun createConversationUseCaseStoresTheSelectedOrder() = runTest {
        val f = Fixture(this)
        val create = CreateConversationUseCase(f.conversations)
        val (first, second) = LanguageCatalog.all
        f.clock.instant += 1.minutes
        val reversedId = create(second, first)
        f.clock.instant += 1.minutes
        val forwardId = create(first, second)

        val byId = f.summaries().associateBy { it.conversation.id }
        val forward = byId.getValue(forwardId).conversation
        val reversed = byId.getValue(reversedId).conversation
        assertEquals(listOf(first.code, second.code), listOf(forward.language1Code, forward.language2Code))
        assertEquals("${first.name} ↔ ${second.name}", forward.title)
        assertEquals(listOf(second.code, first.code), listOf(reversed.language1Code, reversed.language2Code))
        assertEquals("${second.name} ↔ ${first.name}", reversed.title)
        assertEquals(forward.createdAt, forward.updatedAt)
    }

    @Test
    fun hasConversationsReflectsTable() = runTest {
        val f = Fixture(this)
        assertFalse(f.conversations.hasConversations())
        f.conversation()
        assertTrue(f.conversations.hasConversations())
    }

    @Test
    fun sortedByUpdatedAtDescThenIdDesc() = runTest {
        val f = Fixture(this)
        val a = f.conversation("A")
        f.clock.instant += 1.minutes
        val b = f.conversation("B")
        f.clock.instant += 1.minutes
        val c = f.conversation("C")
        assertEquals(listOf(c, b, a), f.summaries().map { it.conversation.id })
    }

    @Test
    fun equalUpdatedAtBreaksTieByIdDescending() = runTest {
        val f = Fixture(this)
        val first = f.conversation("first")
        val second = f.conversation("second")
        assertEquals(f.summaries().map { it.conversation.updatedAt }.distinct().size, 1)
        assertEquals(listOf(second, first), f.summaries().map { it.conversation.id })
    }

    @Test
    fun conversationWithoutMessagesHasNoLastMessageAndZeroCount() = runTest {
        val f = Fixture(this)
        f.conversation()
        val s = f.summaries().single()
        assertNull(s.lastMessageText)
        assertNull(s.lastMessageStatus)
        assertEquals(0L, s.messageCount)
    }

    @Test
    fun countIncludesMessagesOfEveryStatus() = runTest {
        val f = Fixture(this)
        val id = f.conversation()
        MessageStatus.entries.forEach { f.messages.insertMessage(id, it) }
        assertEquals(MessageStatus.entries.size.toLong(), f.summaries().single().messageCount)
    }

    @Test
    fun lastMessageIsNewestByCreatedAt() = runTest {
        val f = Fixture(this)
        val id = f.conversation()
        f.messages.insertMessage(id, MessageStatus.COMPLETED, transcribedText = "old")
        f.clock.instant += 1.minutes
        f.messages.insertMessage(id, MessageStatus.COMPLETED, transcribedText = "new")
        val s = f.summaries().single()
        assertEquals("new", s.lastMessageText)
        assertEquals(MessageStatus.COMPLETED, s.lastMessageStatus)
    }

    @Test
    fun lastMessageTiesOnCreatedAtBreakByHigherId() = runTest {
        val f = Fixture(this)
        val id = f.conversation()
        f.messages.insertMessage(id, MessageStatus.COMPLETED, transcribedText = "first")
        f.messages.insertMessage(id, MessageStatus.FAILED)
        val s = f.summaries().single()
        assertNull(s.lastMessageText)
        assertEquals(MessageStatus.FAILED, s.lastMessageStatus)
        assertEquals(2L, s.messageCount)
    }

    @Test
    fun insertMessageBumpsUpdatedAtAndMovesConversationToTopInOneEmission() = runTest {
        val f = Fixture(this)
        val a = f.conversation("A")
        f.clock.instant += 1.minutes
        val b = f.conversation("B")
        f.clock.instant += 1.minutes
        val c = f.conversation("C")
        val emissions = mutableListOf<List<ConversationSummary>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { f.conversations.observeSummaries().toList(emissions) }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(c, b, a), emissions.last().map { it.conversation.id })

        f.clock.instant += 1.minutes
        f.messages.insertMessage(a, MessageStatus.COMPLETED, transcribedText = "hi")
        testScheduler.advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(a, c, b), emissions.last().map { it.conversation.id })
        assertEquals("hi", emissions.last().first().lastMessageText)
        assertEquals(1L, emissions.last().first().messageCount)
        val first = emissions.last().first().conversation
        assertEquals(f.clock.instant.toEpochMilliseconds(), first.updatedAt)
        assertNotEquals(first.createdAt, first.updatedAt)
        emissions.drop(1).forEach { emission ->
            assertEquals(listOf(a, c, b), emission.map { it.conversation.id }, "no intermediate half-updated state")
        }
    }

    @Test
    fun messageForMissingConversationIsRejectedAndLeavesDataUntouched() = runTest {
        val f = Fixture(this)
        f.conversation()
        val before = f.summaries()
        f.clock.instant += 1.minutes
        val failure = runCatching { f.messages.insertMessage(999, MessageStatus.RECORDING) }
        assertTrue(failure.isFailure)
        assertEquals(before, f.summaries(), "no message row and no updatedAt change")
    }

    @Test
    fun nullableMessageColumnsAreAccepted() = runTest {
        val f = Fixture(this)
        val id = f.conversation()
        f.messages.insertMessage(id, MessageStatus.RECORDING)
        val s = f.summaries().single()
        assertNull(s.lastMessageText)
        assertEquals(MessageStatus.RECORDING, s.lastMessageStatus)
    }

    @Test
    fun sortFollowsUpdatedAtNotInsertionOrder() = runTest {
        val f = Fixture(this)
        f.clock.instant += 2.minutes
        val newestButFirstInserted = f.conversation("A")
        f.clock.instant -= 2.minutes
        val oldestButLastInserted = f.conversation("B")
        f.clock.instant += 1.minutes
        val middle = f.conversation("C")
        assertEquals(listOf(newestButFirstInserted, middle, oldestButLastInserted), f.summaries().map { it.conversation.id })
    }

    @Test
    fun lastMessageFollowsCreatedAtNotInsertionOrder() = runTest {
        val f = Fixture(this)
        val id = f.conversation()
        f.clock.instant += 5.minutes
        f.messages.insertMessage(id, MessageStatus.COMPLETED, transcribedText = "newest")
        f.clock.instant -= 5.minutes
        f.messages.insertMessage(id, MessageStatus.COMPLETED, transcribedText = "older but higher id")
        assertEquals("newest", f.summaries().single().lastMessageText)
    }

    @Test
    fun updatedAtNeverMovesBackwardsWhenTheClockDoes() = runTest {
        val f = Fixture(this)
        val id = f.conversation()
        val created = f.summaries().single().conversation.createdAt
        f.clock.instant -= 10.minutes
        f.messages.insertMessage(id, MessageStatus.COMPLETED, transcribedText = "x")
        assertEquals(created, f.summaries().single().conversation.updatedAt)
    }

    @Test
    fun unknownStoredStatusReadsAsFailedInsteadOfCrashing() = runTest {
        val f = Fixture(this)
        val id = f.conversation()
        f.driver.execute(
            null,
            "INSERT INTO Message(conversationId, status, createdAt) VALUES ($id, 'SOMETHING_NEW', 1)",
            0,
        )
        val s = f.summaries().single()
        assertEquals(MessageStatus.FAILED, s.lastMessageStatus)
        assertEquals(1L, s.messageCount)
    }

    @Test
    fun observeMessagesIsNewestFirstAndScopedToTheConversation() = runTest {
        val f = Fixture(this)
        val a = f.conversation("A")
        val b = f.conversation("B")
        f.clock.instant += 1.minutes
        val older = f.messages.insertMessage(a, MessageStatus.COMPLETED, transcribedText = "older")
        f.messages.insertMessage(b, MessageStatus.COMPLETED, transcribedText = "other conversation")
        f.clock.instant += 1.minutes
        val tieFirst = f.messages.insertMessage(a, MessageStatus.COMPLETED, transcribedText = "tie 1")
        val tieSecond = f.messages.insertMessage(a, MessageStatus.TRANSLATING, transcribedText = "tie 2")

        val messages = f.messages.observeMessages(a).first()

        // The tie-break is also what SQLite's reverse index scan yields, so this can't isolate `id DESC`;
        // it guards the overall order.
        assertEquals(listOf(tieSecond, tieFirst, older), messages.map { it.id })
        assertTrue(messages.all { it.conversationId == a })
        assertEquals(MessageStatus.TRANSLATING, messages.first().status)
    }

    @Test
    fun observeMessagesMapsEveryColumn() = runTest {
        val f = Fixture(this)
        val a = f.conversation()
        val id = f.messages.insertMessage(a, MessageStatus.COMPLETED, "en", "zh", "audio/x.m4a", "Hi", "你好", "Nǐ hǎo")
        val m = f.messages.observeMessages(a).first().single()
        assertEquals(
            Message(id, a, "en", "zh", "audio/x.m4a", "Hi", "你好", "Nǐ hǎo", MessageStatus.COMPLETED, f.clock.instant.toEpochMilliseconds()),
            m,
        )
    }

    @Test
    fun observeMessagesReEmitsWhenAMessageIsInserted() = runTest {
        val f = Fixture(this)
        val a = f.conversation()
        val emissions = mutableListOf<List<Message>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { f.messages.observeMessages(a).toList(emissions) }

        f.messages.insertMessage(a, MessageStatus.RECORDING)

        assertEquals(listOf(0, 1), emissions.map { it.size })
        job.cancel()
    }

    @Test
    fun observeMessagesReadsUnknownStatusAsFailed() = runTest {
        val f = Fixture(this)
        val id = f.conversation()
        f.driver.execute(null, "INSERT INTO Message(conversationId, status, createdAt) VALUES ($id, 'BOGUS', 1)", 0)
        assertEquals(MessageStatus.FAILED, f.messages.observeMessages(id).first().single().status)
    }

    @Test
    fun observeConversationReturnsTheRowOrNull() = runTest {
        val f = Fixture(this)
        val id = f.conversation("English ↔ Mandarin Chinese")
        assertEquals("English ↔ Mandarin Chinese", f.conversations.observeConversation(id).first()?.title)
        assertNull(f.conversations.observeConversation(id + 100).first())
    }

    @Test
    fun debugSeederCreatesVariedConversationsOnceThroughTheRealPath() = runTest {
        val f = Fixture(this)
        val seeder = DebugSeeder(f.database, f.clock, f.dispatcher)

        seeder.seedIfEmpty(sampleAudioPath = "audio/sample.wav")
        val first = f.summaries()
        seeder.seedIfEmpty()

        assertEquals(first, f.summaries(), "second run must be a no-op")
        assertEquals(4, first.size)
        assertEquals(listOf(2L, 2L, 0L, 1L), first.map { it.messageCount })
        assertEquals(listOf("火车站在哪里？", "我需要一杯水", null, null), first.map { it.lastMessageText })
        assertEquals(MessageStatus.FAILED, first.last().lastMessageStatus)
        val recent = f.messages.observeMessages(first.first().conversation.id).first()
        assertEquals(listOf(null, "audio/sample.wav"), recent.map { it.audioPath })
        assertEquals(listOf(null, "Nǐ jīntiān hǎo ma?"), recent.map { it.reading })
        assertTrue(recent.all { it.translatedText != null && it.sourceLanguage != it.targetLanguage })
        assertTrue(first.all { it.conversation.updatedAt <= f.clock.instant.toEpochMilliseconds() })
        assertEquals(first.map { it.conversation.updatedAt }.sortedDescending(), first.map { it.conversation.updatedAt })
    }
}
