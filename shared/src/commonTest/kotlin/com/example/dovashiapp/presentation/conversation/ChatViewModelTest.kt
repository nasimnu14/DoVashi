package com.example.dovashiapp.presentation.conversation

import androidx.lifecycle.ViewModelStore
import com.example.dovashiapp.domain.model.Conversation
import com.example.dovashiapp.domain.usecase.ObserveConversationUseCase
import com.example.dovashiapp.domain.usecase.ObserveMessagesUseCase
import com.example.dovashiapp.testing.FakeAudioPlayer
import com.example.dovashiapp.testing.FakeConversationRepository
import com.example.dovashiapp.testing.FakeMessageRepository
import com.example.dovashiapp.testing.PipelineFixture
import com.example.dovashiapp.testing.message
import com.example.dovashiapp.domain.usecase.RecordingState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val conversations = FakeConversationRepository()
    private val messages = FakeMessageRepository()
    private val player = FakeAudioPlayer()
    private val conversation = Conversation(1, "Alpha ↔ Beta", "en", "zh", 0, 0)

    private val pipeline = PipelineFixture(UnconfinedTestDispatcher())

    private fun viewModel(id: Long = 1) =
        ChatViewModel(id, ObserveConversationUseCase(conversations), ObserveMessagesUseCase(messages), player, pipeline.controller, pipeline.processor)

    private fun TestScope.collect(vm: ChatViewModel): MutableList<ChatUiState> {
        val states = mutableListOf<ChatUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.toList(states) }
        return states
    }

    private fun MutableList<ChatUiState>.bubbles() = assertIs<ChatUiState.Content>(last()).bubbles

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsLoadingThenShowsContentAndLiveUpdates() = runTest {
        val vm = viewModel()
        val states = collect(vm)
        assertEquals(ChatUiState.Loading, states.first())

        conversations.conversations.value = mapOf(1L to conversation)
        assertEquals(emptyList(), states.bubbles())

        messages.messages.value = mapOf(1L to listOf(message(2), message(1)))
        assertEquals(listOf(2L, 1L), states.bubbles().map { it.id })
        assertEquals("Alpha ↔ Beta", assertIs<ChatUiState.Content>(states.last()).title)
    }

    @Test
    fun unknownConversationIsNotFound() = runTest {
        val states = collect(viewModel(id = 99))
        conversations.conversations.value = mapOf(1L to conversation)
        assertEquals(ChatUiState.NotFound, states.last())
    }

    @Test
    fun tapPlaysThenPausesThenResumes() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/a")))
        val vm = viewModel()
        val states = collect(vm)

        vm.onPlaybackClick(1)
        assertEquals(BubblePlayback.PAUSE, states.bubbles().single().playback)
        vm.onPlaybackClick(1)
        assertEquals(BubblePlayback.PLAY, states.bubbles().single().playback)
        vm.onPlaybackClick(1)
        assertEquals(BubblePlayback.PAUSE, states.bubbles().single().playback)
        assertEquals(listOf("audio/a", "audio/a"), player.played)

        player.finish()
        assertEquals(BubblePlayback.PLAY, states.bubbles().single().playback)
    }

    @Test
    fun tappingAnotherMessageSwitchesRecording() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(2, audioPath = "audio/b"), message(1, audioPath = "audio/a")))
        val vm = viewModel()
        val states = collect(vm)

        vm.onPlaybackClick(1)
        vm.onPlaybackClick(2)

        assertEquals(listOf("audio/a", "audio/b"), player.played)
        assertEquals(listOf(BubblePlayback.PAUSE, BubblePlayback.PLAY), states.bubbles().map { it.playback })
    }

    private fun history(count: Int) = (count downTo 1).map { message(it.toLong()) }

    @Test
    fun opensWithOnlyTheNewestPageOfALargeConversation() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to history(100))
        val states = collect(viewModel())

        val ids = states.bubbles().map { it.id }
        assertEquals(ChatViewModel.PAGE_SIZE, ids.size)
        assertEquals(100L, ids.first())
    }

    @Test
    fun loadingOlderAddsOnePageAtATimeUntilEverythingIsLoaded() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to history(70))
        val vm = viewModel()
        val states = collect(vm)

        vm.onLoadOlder()
        assertEquals(2 * ChatViewModel.PAGE_SIZE, states.bubbles().size)
        vm.onLoadOlder()
        assertEquals(70, states.bubbles().size)
        vm.onLoadOlder() // everything is loaded: nothing more is requested
        vm.onLoadOlder()
        assertEquals(70, states.bubbles().size)
        assertEquals(1L, states.bubbles().last().id)
    }

    @Test
    fun smallConversationIgnoresLoadOlder() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to history(5))
        val vm = viewModel()
        val states = collect(vm)

        repeat(3) { vm.onLoadOlder() }
        messages.messages.value = mapOf(1L to history(6))

        // Had the window grown, the sixth Message would not push anything out; either way all six show.
        assertEquals(6, states.bubbles().size)
    }

    @Test
    fun newMessageDoesNotPushTheOlderOnesTheUserIsReadingOutOfTheWindow() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to history(100))
        val vm = viewModel()
        val states = collect(vm)
        vm.onLoadOlder() // 60 loaded; oldest shown is 41
        assertEquals(41L, states.bubbles().last().id)

        messages.messages.value = mapOf(1L to history(101))

        assertEquals(101L, states.bubbles().first().id)
        assertEquals(41L, states.bubbles().last().id, "the oldest loaded Message stays loaded")
        assertEquals(61, states.bubbles().size)
    }

    @Test
    fun reopeningAConversationStartsAtTheNewestPageAgain() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to history(100))
        val first = viewModel()
        val firstStates = collect(first)
        first.onLoadOlder()
        assertEquals(60, firstStates.bubbles().size)

        val reopened = collect(viewModel())

        assertEquals(ChatViewModel.PAGE_SIZE, reopened.bubbles().size)
        assertEquals(100L, reopened.bubbles().first().id)
    }

    @Test
    fun retryClickRerunsAFailedMessageAndItCompletes() = runTest {
        val id = pipeline.messages.insertMessage(1, com.example.dovashiapp.domain.model.MessageStatus.FAILED, audioPath = "audio/a.m4a")
        val vm = ChatViewModel(
            1, ObserveConversationUseCase(pipeline.conversations), ObserveMessagesUseCase(pipeline.messages), player,
            pipeline.controller, pipeline.processor,
        )
        val states = collect(vm)
        assertTrue(states.bubbles().single().canRetry)

        vm.onRetryClick(id)

        val bubble = states.bubbles().single()
        assertEquals("T(Good morning)", bubble.translatedText)
        assertEquals(false, bubble.canRetry)
    }

    @Test
    fun unplayableRecordingIsMarkedUnavailable() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/missing")))
        player.unplayable += "audio/missing"
        val vm = viewModel()
        val states = collect(vm)

        vm.onPlaybackClick(1)

        assertEquals(BubblePlayback.UNAVAILABLE, states.bubbles().single().playback)
    }

    @Test
    fun tapOnMessageWithoutRecordingDoesNothing() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = null)))
        val vm = viewModel()
        collect(vm)

        vm.onPlaybackClick(1)
        vm.onPlaybackClick(42)

        assertTrue(player.played.isEmpty())
    }

    @Test
    fun missingRecordingDoesNotInterruptThePlayingOne() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(2, audioPath = "audio/missing"), message(1, audioPath = "audio/a")))
        player.unplayable += "audio/missing"
        val vm = viewModel()
        val states = collect(vm)

        vm.onPlaybackClick(1)
        vm.onPlaybackClick(2)

        assertEquals(listOf(BubblePlayback.UNAVAILABLE, BubblePlayback.PAUSE), states.bubbles().map { it.playback })
    }

    @Test
    fun unavailableClearsWhenTheReferenceChanges() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/missing")))
        player.unplayable += "audio/missing"
        val vm = viewModel()
        val states = collect(vm)
        vm.onPlaybackClick(1)

        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/new")))

        assertEquals(BubblePlayback.PLAY, states.bubbles().single().playback)
    }

    @Test
    fun stopPlaybackStopsTheRecording() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/a")))
        val vm = viewModel()
        val states = collect(vm)
        vm.onPlaybackClick(1)

        vm.stopPlayback()

        assertEquals(BubblePlayback.PLAY, states.bubbles().single().playback)
    }

    private fun MutableList<ChatUiState>.content() = assertIs<ChatUiState.Content>(last())

    @Test
    fun micClickStartsThenStopsAndStopsPlaybackFirst() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        messages.messages.value = mapOf(1L to listOf(message(1, audioPath = "audio/a")))
        val vm = viewModel()
        val states = collect(vm)
        vm.onPlaybackClick(1)

        vm.onMicClick()
        assertIs<MicUi.Recording>(states.content().mic)
        assertEquals(BubblePlayback.PLAY, states.bubbles().single().playback, "playback stopped before recording")

        vm.onMicClick()
        assertEquals(MicUi.Idle, states.content().mic)
        assertEquals(1, pipeline.recorder.stops)
    }

    @Test
    fun anotherConversationRecordingMakesTheMicUnavailable() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        val states = collect(viewModel())
        pipeline.controller.start(2)
        assertEquals(MicUi.Unavailable, states.content().mic)
    }

    @Test
    fun deniedMicrophoneIsExplainedAndClearedOnTheNextTap() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        val vm = viewModel()
        val states = collect(vm)
        vm.onMicrophoneDenied()
        assertEquals("Microphone access is needed to record. You can allow it in Settings.", states.content().micMessage)
        vm.onMicClick()
        assertEquals(null, states.content().micMessage)
    }

    @Test
    fun recorderFailureIsExplained() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        pipeline.recorder.startSucceeds = false
        val vm = viewModel()
        val states = collect(vm)
        vm.onMicClick()
        assertEquals("Couldn't start recording. Please try again.", states.content().micMessage)
        assertEquals(MicUi.Idle, states.content().mic)
    }

    @Test
    fun aSecondTapWhileStartingStopsInsteadOfComplaining() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        pipeline.messages.insertGate = gate
        pipeline.recorder.stopDuration = 100
        val vm = viewModel()
        val states = collect(vm)

        vm.onMicClick()
        assertIs<MicUi.Recording>(states.content().mic, "starting already shows as recording here")
        vm.onMicClick()
        gate.complete(Unit)

        assertEquals(MicUi.Idle, states.content().mic)
        assertEquals(null, states.content().micMessage)
        assertEquals(RecordingState.Idle, pipeline.controller.state.value)
    }

    @Test
    fun aRecordingLostByAnAutomaticStopIsExplained() = runTest {
        conversations.conversations.value = mapOf(1L to conversation)
        val vm = viewModel()
        val states = collect(vm)
        pipeline.recorder.stopDuration = null
        vm.onMicClick()
        pipeline.clock.instant += kotlin.time.Duration.parse("30s")
        pipeline.controller.stopAny() // e.g. the app went to the background

        assertEquals("The recording didn't work. Please try again.", states.content().micMessage)
        assertEquals(MicUi.Idle, states.content().mic)
    }

    @Test
    fun clearingTheViewModelStopsItsRecording() = runTest {
        val store = ViewModelStore()
        val vm = viewModel()
        store.put("chat", vm)
        vm.onMicClick()
        assertIs<RecordingState.Recording>(pipeline.controller.state.value)
        store.clear()
        assertEquals(RecordingState.Idle, pipeline.controller.state.value)
    }

    @Test
    fun clearingTheViewModelReleasesThePlayer() = runTest {
        val store = ViewModelStore()
        store.put("chat", viewModel())
        store.clear()
        assertTrue(player.released)
    }
}
