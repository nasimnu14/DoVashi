package com.example.dovashiapp.presentation.createconversation

import androidx.lifecycle.viewModelScope
import com.example.dovashiapp.domain.model.Language
import com.example.dovashiapp.domain.model.LanguageCatalog
import com.example.dovashiapp.domain.usecase.CreateConversationUseCase
import com.example.dovashiapp.testing.FakeConversationRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CreateConversationViewModelTest {

    private val a = Language("aa", "Alpha", "Alpha")
    private val b = Language("bb", "Beta", "Beta")
    private val repository = FakeConversationRepository()

    private fun viewModel(languages: List<Language> = listOf(a, b)) =
        CreateConversationViewModel(CreateConversationUseCase(repository), languages)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun defaultsAreTheFirstTwoCatalogLanguages() {
        val state = viewModel(LanguageCatalog.all).uiState.value
        assertEquals(LanguageCatalog.all, state.languages)
        assertEquals(LanguageCatalog.all[0], state.language1)
        assertEquals(LanguageCatalog.all[1], state.language2)
        assertTrue(state.canStart)
        assertFalse(state.isSameLanguage)
    }

    @Test
    fun defaultLanguage2SkipsEntriesSharingLanguage1sCode() {
        val aDuplicate = a.copy(name = "Alpha again")
        val state = viewModel(listOf(a, aDuplicate, b)).uiState.value
        assertEquals(a, state.language1)
        assertEquals(b, state.language2)
    }

    @Test
    fun sameLanguageInBothFieldsDisablesStartAndCreatesNothing() = runTest {
        val vm = viewModel()
        vm.selectLanguage2(a)
        assertTrue(vm.uiState.value.isSameLanguage)
        assertFalse(vm.uiState.value.canStart)

        vm.start()

        assertTrue(repository.created.isEmpty())
        assertNull(vm.uiState.value.createdConversationId)
        // Proves the ViewModel guard stopped it, not the use case's `require` caught as a failure.
        assertNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isCreating)
    }

    @Test
    fun startPersistsTheSelectedOrderAndRequestsNavigation() = runTest {
        val vm = viewModel()
        vm.start()
        assertEquals(listOf(FakeConversationRepository.Created("Alpha ↔ Beta", "aa", "bb")), repository.created)
        assertEquals(1L, vm.uiState.value.createdConversationId)
        assertFalse(vm.uiState.value.canStart, "button stays disabled until the screen has navigated")
    }

    @Test
    fun reversedSelectionPersistsReversedCodes() = runTest {
        val vm = viewModel()
        vm.selectLanguage1(b)
        vm.selectLanguage2(a)
        vm.start()
        assertEquals(listOf(FakeConversationRepository.Created("Beta ↔ Alpha", "bb", "aa")), repository.created)
    }

    @Test
    fun secondStartWhileTheFirstIsRunningCreatesOnlyOne() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        val vm = viewModel()

        vm.start()
        assertTrue(vm.uiState.value.isCreating)
        assertFalse(vm.uiState.value.canStart)
        vm.start()
        gate.complete(Unit)

        assertEquals(1, repository.created.size)
    }

    @Test
    fun failureShowsAnErrorAndAllowsRetry() = runTest {
        repository.failNextCreate = IllegalStateException("disk full")
        val vm = viewModel()

        vm.start()
        assertEquals("Couldn't create the conversation. Please try again.", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isCreating)
        assertTrue(vm.uiState.value.canStart)
        assertNull(vm.uiState.value.createdConversationId)

        vm.start()
        assertEquals(1, repository.created.size)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(1L, vm.uiState.value.createdConversationId)
    }

    @Test
    fun selectingEitherLanguageClearsTheError() = runTest {
        val vm = viewModel()
        repository.failNextCreate = IllegalStateException("disk full")
        vm.start()
        assertNotNull(vm.uiState.value.errorMessage)
        vm.selectLanguage1(a)
        assertNull(vm.uiState.value.errorMessage)

        repository.failNextCreate = IllegalStateException("disk full")
        vm.start()
        assertNotNull(vm.uiState.value.errorMessage)
        vm.selectLanguage2(b)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun leavingTheScreenDuringCreateShowsNoError() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        val vm = viewModel()

        vm.start()
        vm.viewModelScope.cancel()
        gate.complete(Unit)

        // The fake is cancelled before it inserts; a real insert may already have committed (TC-24).
        assertTrue(repository.created.isEmpty())
        assertNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.createdConversationId)
    }

    @Test
    fun selectionIsIgnoredWhileCreating() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        val vm = viewModel()

        vm.start()
        vm.selectLanguage1(b)
        vm.selectLanguage2(b)
        gate.complete(Unit)

        assertEquals(a, vm.uiState.value.language1)
        assertEquals(b, vm.uiState.value.language2)
        assertEquals(listOf(FakeConversationRepository.Created("Alpha ↔ Beta", "aa", "bb")), repository.created)
    }

    @Test
    fun catalogWithOneLanguageLeavesLanguage2EmptyAndStartDisabled() {
        val state = viewModel(listOf(a)).uiState.value
        assertEquals(a, state.language1)
        assertNull(state.language2)
        assertFalse(state.canStart)
    }

    @Test
    fun emptyCatalogLeavesBothEmpty() {
        val state = viewModel(emptyList()).uiState.value
        assertNull(state.language1)
        assertNull(state.language2)
        assertFalse(state.canStart)
    }

    @Test
    fun onNavigationHandledClearsTheTarget() = runTest {
        val vm = viewModel()
        vm.start()
        assertNotNull(vm.uiState.value.createdConversationId)
        vm.onNavigationHandled()
        assertNull(vm.uiState.value.createdConversationId)
    }
}
