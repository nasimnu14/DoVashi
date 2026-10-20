package com.example.dovashiapp.presentation.testcases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Debug-only checklist over [TestCaseCatalog]. Tap state is kept for the process lifetime only. */
class TestCasesViewModel(
    private val catalog: List<TestCase> = TestCaseCatalog.all,
) : ViewModel() {
    private val resultsByKey = MutableStateFlow<Map<String, TestCaseResult>>(emptyMap())

    val uiState: StateFlow<TestCasesUiState> = resultsByKey
        .map { buildTestCasesUiState(catalog, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildTestCasesUiState(catalog, emptyMap()))

    /** Tapping the already-selected result clears it back to [TestCaseResult.UNSET]. */
    fun onResultSelected(ticket: String, id: String, result: TestCaseResult) {
        val key = resultKey(ticket, id)
        resultsByKey.update { current ->
            if (current[key] == result) current - key else current + (key to result)
        }
    }
}
