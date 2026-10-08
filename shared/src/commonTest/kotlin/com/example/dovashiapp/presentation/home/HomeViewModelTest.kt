package com.example.dovashiapp.presentation.home

import com.example.dovashiapp.domain.model.ConversationSummary
import com.example.dovashiapp.domain.model.MessageStatus
import com.example.dovashiapp.domain.usecase.ObserveConversationSummariesUseCase
import com.example.dovashiapp.testing.FakeClock
import com.example.dovashiapp.testing.FakeConversationRepository
import com.example.dovashiapp.testing.summary
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsLoadingThenEmptyThenContent() = runTest {
        val source = MutableSharedFlow<List<ConversationSummary>>()
        val clock = FakeClock(Instant.parse("2026-10-08T12:00:00Z"))
        val viewModel = HomeViewModel(
            ObserveConversationSummariesUseCase(FakeConversationRepository(source)), clock, TimeZone.UTC,
        )
        val states = mutableListOf<HomeUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(states) }

        assertEquals(HomeUiState.Loading, states.last())

        source.emit(emptyList())
        assertEquals(HomeUiState.Empty, states.last())

        source.emit(listOf(summary(id = 1, updatedAt = clock.instant.toEpochMilliseconds(), lastMessageText = "Hi", lastMessageStatus = MessageStatus.COMPLETED)))
        val rows = assertIs<HomeUiState.Content>(states.last()).rows
        assertEquals(listOf("Just now"), rows.map { it.timeLabel })
    }

    @Test
    fun timeLabelsAreRecomputedOnlyWhenTheListEmitsAgain() = runTest {
        val source = MutableSharedFlow<List<ConversationSummary>>()
        val clock = FakeClock(Instant.parse("2026-10-08T12:00:00Z"))
        val viewModel = HomeViewModel(
            ObserveConversationSummariesUseCase(FakeConversationRepository(source)), clock, TimeZone.UTC,
        )
        val states = mutableListOf<HomeUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(states) }
        val data = listOf(summary(id = 1, updatedAt = clock.instant.toEpochMilliseconds()))

        source.emit(data)
        clock.instant += 5.minutes
        assertEquals("Just now", assertIs<HomeUiState.Content>(states.last()).rows.single().timeLabel)
        source.emit(data)

        assertEquals("5 min ago", assertIs<HomeUiState.Content>(states.last()).rows.single().timeLabel)
    }
}
