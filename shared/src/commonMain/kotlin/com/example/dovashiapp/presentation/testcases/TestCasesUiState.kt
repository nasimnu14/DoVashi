package com.example.dovashiapp.presentation.testcases

enum class TestCaseResult { UNSET, PASS, FAIL }

data class TestCaseUi(
    val ticket: String,
    val id: String,
    val title: String,
    val expected: String,
    val priority: String,
    val isAuto: Boolean,
    val result: TestCaseResult,
)

data class TicketGroupUi(
    val ticket: String,
    val cases: List<TestCaseUi>,
)

data class TestCasesUiState(
    val groups: List<TicketGroupUi>,
    val passed: Int,
    val failed: Int,
    val total: Int,
)
